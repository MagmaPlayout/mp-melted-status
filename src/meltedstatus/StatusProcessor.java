package meltedstatus;

import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.UstaResponse;
import meltedBackend.statuscmd.StatusCmdProcessor;
import meltedstatus.adminApi.AdminApi;
import meltedstatus.adminApi.MPAdminApi;
import meltedstatus.playoutApi.MPPlayoutApi;
import meltedstatus.playoutApi.PlayoutApi;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import us.monoid.web.Resty;

/**
 *
 * @author debian
 */
public class StatusProcessor implements StatusCmdProcessor {
    private Jedis redisPublisher;
    private final String mstaChannel;
    private final Logger logger;
    private String startTime;
    private String endTime;
    private final ConfigurationManager cfg;
    private final MPPlayoutApi playoutApi;
    private final MPAdminApi adminApi;
    
    /**
     * CONSTRUCTOR INIT
     */
    public StatusProcessor(){
        this.logger = Logger.getLogger(MeltedStatus.class.getName());
        this.cfg = ConfigurationManager.getInstance();
        this.redisPublisher = new Jedis(cfg.getRedisHost(), cfg.getRedisPort(), cfg.getRedisReconnectionTimeout());
        
        Resty resty = new Resty(Resty.Option.timeout(4000));
        Gson gson = new Gson();
        this.playoutApi = new PlayoutApi(resty, gson, cfg);
        this.adminApi = new AdminApi(resty, gson, cfg);

        // Connects to redis server
        try {
            while(!connectToRedisPublisher(logger, cfg, redisPublisher)){
                logger.log(Level.SEVERE, "MeltedStatus - ERROR: Could not connect to the Redis server. Will retry shortly...");
                Thread.sleep(5000);
            }
        } catch (InterruptedException ex) {
            System.exit(1);
        }
        logger.log(Level.INFO, "Status Processor Ready.");
        this.mstaChannel = cfg.getRedisMstaChannel();
        this.startTime = "-";
        this.endTime = "-";
    }
    
    @Override      
    public void meltedDisonnected(){
        redisPublisher.publish(mstaChannel, "MELTED-DISONNECTED"); // TODO: poner los comandos por redis en algún lugar comun así el core los saca de ahí también
        logger.log(Level.SEVERE, "MeltedStatus - line: {0}", "MELTED-DISCONNECTED");
    }
    
    
    /**
     * Detects events and calls the right response.
     * @param currentFrame
     * @param previousFrame
     * @param line
     */
    @Override    
    public void eventHandler(UstaResponse currentFrame, UstaResponse previousFrame, String line){
        if (previousFrame == null) {
            // TODO no se si está bien salir si no tiene previous frame
            return;
        }

        // Publish line to redis
        redisPublisher.publish(mstaChannel, line);

        // check and prints current mode
        //checkMode(currentFrame);

        // checks mode change
        checkChangeMode(currentFrame, previousFrame);

        // checks for index change
        checkChangeIndex(currentFrame, previousFrame);
    }    
    
    private boolean checkChangeMode(UstaResponse currentFrame, UstaResponse previousFrame){
        boolean status = true;
        
        try {
            String previousMode = previousFrame.getUnitStatus();
            String currentMode = currentFrame.getUnitStatus();

            if(!previousMode.equals(currentMode)){
                redisPublisher.publish(mstaChannel, "CHANGE MODE FROM "+previousMode+" TO "+currentMode);
                logger.log(Level.INFO, "MeltedStatus - line: {0}", "CHANGE MODE FROM "+previousMode+" TO "+currentMode);
            }

            if (previousMode.equals("playing")) {
                // From Playing to Pause - posible final de Playlist - Sets EndTime and sends log
                if (currentMode.equals("paused")) {
                    redisPublisher.publish(mstaChannel, "PLAYBACK PAUSED");
                    logger.log(Level.INFO, "MeltedStatus - line: {0}", "PLAYBACK PAUSED");
                    this.endTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
                    status = sendClipToLogDB(previousFrame);
                    // TODO: mandar un play acá
                }
                // from Playing to Stop - Sets EndTime and sends log
                else if (currentMode.equals("stopped")) {
                    redisPublisher.publish(mstaChannel, "PLAYBACK STOPPED");
                    logger.log(Level.INFO, "MeltedStatus - line: {0}", "PLAYBACK STOPPED");
                    this.endTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
                    status = sendClipToLogDB(previousFrame);
                    // TODO: mandar un play acá
                }
            }
            // Play - Sets StartTime
            else if (currentMode.equals("playing")) {
                redisPublisher.publish(mstaChannel, "PLAYBACK STARTED");
                logger.log(Level.INFO, "MeltedStatus - line: {0}", "PLAYBACK STARTED");
                this.startTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
            }


        } catch (MeltedCommandException ex) {
            // TODO: HANDLE
            Logger.getLogger(StatusProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return status;
    }
    
    private void checkChangeIndex(UstaResponse currentFrame, UstaResponse previousFrame){
        try {
            int previousIndex = previousFrame.getPlayingClipIndex();
            int currentIndex = currentFrame.getPlayingClipIndex();

            if(previousIndex != currentIndex){ // Index changed
                redisPublisher.publish(mstaChannel, "INDEX CHANGED FROM "+previousIndex+" TO "+currentIndex);
                logger.log(Level.INFO, "MeltedStatus - line: {0}", "INDEX CHANGED FROM "+previousIndex+" TO "+currentIndex);

                // sets end time and sends log
                this.endTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
                sendClipToLogDB(previousFrame);

                // sets next clip start time
                this.startTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
            }
        } catch (MeltedCommandException ex) {
            // TODO: HANDLE
            Logger.getLogger(StatusProcessor.class.getName()).log(Level.INFO, "No media index. Line discarded...", ex);
        }
    }
    
    private String checkMode(String[] cmd){
        // CHECKS MODE "unknown"
        if(cmd[1].equals("unknown")){
            redisPublisher.publish(mstaChannel, "MODE: unknown");
            logger.log(Level.INFO, "MeltedStatus - line: {0}", "MODE: unknown");
            return "unknown";
        }
        // CHECKS MODE "stopped"
        if(cmd[1].equals("stopped")){
            redisPublisher.publish(mstaChannel, "MODE: stopped");
            logger.log(Level.INFO, "MeltedStatus - line: {0}", "MODE: stopped");       
            return "stopped";
        }
        // CHECKS MODE "paused"
        if(cmd[1].equals("paused")){
            redisPublisher.publish(mstaChannel, "MODE: paused");
            logger.log(Level.INFO, "MeltedStatus - line: {0}", "MODE: stopped");
            return "paused";
        }
        // CHECKS MODE "playing"
        if(cmd[1].equals("playing")){
            redisPublisher.publish(mstaChannel, "MODE: playing");
            logger.log(Level.INFO, "MeltedStatus - line: {0}", "MODE: playing");       
            return "playing";
        }
        // CHECKS MODE "offline"
        if(cmd[1].equals("offline")){
            redisPublisher.publish(mstaChannel, "MODE: offline");
            logger.log(Level.INFO, "MeltedStatus - line: {0}", "MODE: offline");       
            return "offline";
        }
        // CHECKS MODE "not_loaded"
        if(cmd[1].equals("not_loaded")){
            redisPublisher.publish(mstaChannel, "MODE: not_loaded");
            logger.log(Level.INFO, "MeltedStatus - line: {0}", "MODE: not_loaded"); 
            return "not_loaded";
        }
        // CHECKS MODE "disconnected"
        if(cmd[1].equals("disconnected")){
            redisPublisher.publish(mstaChannel, "MODE: disconnected");
            logger.log(Level.INFO, "MeltedStatus - line: {0}", "MODE: disconnected");
            return "disconnected";
        }
        
        redisPublisher.publish(mstaChannel, "MODE: ERROR READING MODE");
        logger.log(Level.INFO, "MeltedStatus - line: {0}", "MODE: ERROR READING MODE");
        return "ERROR READING MODE";
    }

    /**
     * Creates a ClipLog and sends it to the playoutLog DB table using the DB API.
     * 
     * @param frame
     * @return
     */
    private boolean sendClipToLogDB(UstaResponse frame){
        try {
            if(this.startTime.length() <= 1) return false;

            ClipLog clip = new ClipLog(frame.getPlayingClipPath(), this.startTime, this.endTime);
            String clipPath = clip.getName().replace("\"", ""); // Remove unnecesary quotes

            // If the default media is playing then I don't log anything
            // TODO: agregarcódigo de piece para el default media e.g. -1
            if(clipPath.equals(cfg.getDefaultMediaPath()) || clipPath.isEmpty()){
                return false;
            }

            int pieceId = Integer.parseInt(clipPath);

            // Get's the idRawMedia that matches the clipPath
            String idRawMedia = playoutApi.getIdRawMedia(pieceId);
            if(idRawMedia.isEmpty()){
                logger.log(Level.SEVERE, "StatusProcessor - Couldn''t get the idRawMedia for {0}", clipPath);
                return false;
            }
            clip.setIdRawMedia(idRawMedia);
            logger.log(Level.INFO, "StatusProcessor - idRawMedia = {0}", idRawMedia);

            // Get piece data from mp-playout-api
            clip = playoutApi.loadPieceData(clip);            

            // Call api to insert this log
            System.out.println(clip.toString());
            adminApi.insertLog(clip);
        } catch(NumberFormatException e){
            return false;
        } catch (MeltedCommandException ex) {
            //TODO HANDLE
            Logger.getLogger(StatusProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }
    
    /**
     * Connects to redis.
     * @param logger
     * @param cfg
     * @param redisPublisher
     * @return 
     */
    private static boolean connectToRedisPublisher(Logger logger, ConfigurationManager cfg, Jedis redisPublisher){
        logger.log(Level.INFO, "MeltedStatus - Attempt to connect to Redis Publisher server...");
        try {
            redisPublisher.connect();
        }
        catch(JedisConnectionException e){
            logger.log(Level.SEVERE, "MeltedStatus - ERROR: Could not connect to the Redis server. Is it running?");
            return false;
        }

        logger.log(Level.INFO, "MeltedStatus - Connected to Redis Publisher server.");
        return true;
    }
}
