/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package meltedstatus;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.UstaResponse;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

/**
 *
 * @author debian
 */
public class StatusProcessor {
    private Jedis redisPublisher;
    private final String mstaChannel;
    private final Logger logger;
    private String startTime;
    private String endTime;
    private final ConfigurationManager cfg;
    
    
    /**
     * CONSTRUCTOR INIT
     */
    public StatusProcessor(){
        this.logger = Logger.getLogger(MeltedStatus.class.getName());
        this.cfg = ConfigurationManager.getInstance();
        this.redisPublisher = new Jedis(cfg.getRedisHost(), cfg.getRedisPort(), cfg.getRedisReconnectionTimeout());

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
    
    public void meltedDisonnected(){
        redisPublisher.publish(mstaChannel, "MELTED DISONNECTED");
        logger.log(Level.SEVERE, "MeltedStatus - line: {0}", "MELTED DISCONNECTED");
    }
    
    
    /**
     * Detects events and calls the right response.
     * @param currentFrame
     * @param previousFrame
     * @param line
     */
    public void eventHandler(UstaResponse currentFrame, UstaResponse previousFrame, String line){
        // Prints line to logger
        //logger.log(Level.INFO, "MeltedStatus - line: {0}", line); // COMENTO ESTO PARA NO SPAMMEAR LA CONSOLA
        
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
                    status = sendClipToLog(previousFrame);
                }
                // from Playing to Stop - Sets EndTime and sends log
                else if (currentMode.equals("stopped")) {
                    redisPublisher.publish(mstaChannel, "PLAYBACK STOPPED");
                    logger.log(Level.INFO, "MeltedStatus - line: {0}", "PLAYBACK STOPPED");
                    this.endTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
                    status = sendClipToLog(previousFrame);
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
                sendClipToLog(previousFrame);

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
    
    private void wrongLenghtException(String[] cmd){
        redisPublisher.publish(mstaChannel, "WARNING: WrongLenghtException - USTA LINE: " + cmd);
        logger.log(Level.INFO, "WARNING: WrongLenghtException - USTA LINE: " + cmd);
    }
    
    private boolean sendClipToLog(UstaResponse frame){
        try {
            // Creates json and sends it to playout log
            ClipLog clip = new ClipLog(frame.getPlayingClipPath(), this.startTime, this.endTime);
            
            //POSTing in Resty
            Gson gson = new Gson();
            Resty resty = new Resty(Resty.Option.timeout(4000));

            //primero busco el idRawMedia que corresponde segun el nombre del clip
            String idRawMedia = "";
            String clipPath = clip.getName().replace("\"", ""); // Remove unnecesary quotes
            String encodedName = URLEncoder.encode(clipPath);

            // If the default media is playing then I don't log anything
            if(clipPath.equals(cfg.getDefaultMediaPath())){
                return false;
            }

            clip.setName(clip.getName());
            try {
                JSONArray jsonResource = resty.json("http://localhost:8001/api/medias/name/"+encodedName).array();
                JSONObject jsonMedia = jsonResource.getJSONObject(0);
                idRawMedia = (String) jsonMedia.get("id");
            } catch (IOException ex) {
                // TODO: HANDLE
                Logger.getLogger(StatusProcessor.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
                return false;
            } catch (Exception ex) {
                // TODO: HANDLE
                Logger.getLogger(StatusProcessor.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
                return false;
            }

            clip.setIdRawMedia(idRawMedia);
            System.out.println("clip = " + clip.toString());
            String jsonClip = gson.toJson(clip);
            Map<String, String> clipMap = gson.fromJson(jsonClip, Map.class);
            JSONObject jsonObject = new JSONObject(clipMap);
            try {
                resty.json("http://localhost:8080/api/playoutLog", Resty.content(jsonObject));
            } catch (IOException ex) {
                // TODO: HANDLE
                Logger.getLogger(StatusProcessor.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
                return false;
            }
        } catch (MeltedCommandException ex) {
            // TODO: HANDLE
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
