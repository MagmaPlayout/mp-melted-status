package meltedstatus;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

/**
 * Connects to a running melted instance and queries the STATUS.
 * STATUS is a unique command, so I'm not going to use mpc-meltedBackend for this.
 * This way is clearer and simpler.
 * 
 * @author rombus
 */
public class MeltedStatus {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private boolean running;
    
    
    public static void main(String[] args) {
        MeltedStatus ms = new MeltedStatus();
        ms.run();
    }
    
    private void run(){
        // General config
        Logger logger = Logger.getLogger(MeltedStatus.class.getName());
        ConfigurationManager cfg = ConfigurationManager.getInstance();
        cfg.init(logger);
        
        /**
         * Connect to Redis publisher server.
         */
        Jedis redisPublisher = new Jedis(cfg.getRedisHost(), cfg.getRedisPort(), cfg.getRedisReconnectionTimeout());
        try {
            while(!connectToRedisPublisher(logger, cfg, redisPublisher)){
                logger.log(Level.SEVERE, "MeltedStatus - ERROR: Could not connect to the Redis server. Will retry shortly...");
                Thread.sleep(5000);
            }
        } catch (InterruptedException ex) {
            System.exit(1);
        }
        String mstaChannel = cfg.getRedisMstaChannel();
        UstaParser parser = new UstaParser();
        //Map<String, String> cmdMap = new HashMap<String, String>();
        String[] cmd;
        String startTime = "-";
        String endTime;
        String last_index = "N";
        String last_mode = "unknown";
        String last_name = "noclip";
        
        
        try {
            socket = new Socket(cfg.getMeltedHost(), cfg.getMeltedPort());
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
            writer.println("STATUS");        // Send telnet command
            running=true;
            while(running){               
                String line = reader.readLine(); // Blocking method
                if(line != null){
                    //logger.log(Level.INFO, "MeltedStatus - line: {0}", line); // COMENTO ESTO PARA NO SPAMMEAR LA CONSOLA
                    redisPublisher.publish(mstaChannel, line);
                    cmd = parser.getCmdParsed(line);
                    
                    if(cmd.length == 17){ // Filter USTA commands with propper lenght   
                                                
                        if(cmd[1].equals("unknown")){ // Nothing loaded yet
                            redisPublisher.publish(mstaChannel, "MODE: unknown");
                            logger.log(Level.INFO, "MeltedStatus - line: {0}", "MODE: unknown");       
                        }
                        
                        if(cmd[1].equals("stopped")){ // Nothing loaded yet
                            redisPublisher.publish(mstaChannel, "MODE: stopped");
                            logger.log(Level.INFO, "MeltedStatus - line: {0}", "MODE: stopped");       
                        }
                        
                        if(cmd[1].equals("playing") && last_mode.equals("stopped")){ // when first clip is played
                            redisPublisher.publish(mstaChannel, "start playing");
                            logger.log(Level.INFO, "MeltedStatus - line: {0}", "start playing");    
                            startTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
                        }
                        
                        // NO ENTRA EN ESTE IF
                        if(cmd[1].equals("stopped") && last_mode.equals("playing")){ // when last clip stopped
                            redisPublisher.publish(mstaChannel, "PLAYLIST END");
                            logger.log(Level.INFO, "MeltedStatus - line: {0}", "PLAYLIST END");
                            endTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
                            ClipLog clip = new ClipLog();
                            clip.setName(last_name);
                            clip.setStart(startTime);
                            clip.setEnd(endTime);
                            System.out.println("clip = " + clip.toString());
                            //POSTing in Resty
                            //Gson gson = new Gson();
                            //String jsonClip = gson.toJson(clip);
                            //Map<String, String> clipMap = gson.fromJson(jsonClip, Map.class);
                            //Resty resty = new Resty(Resty.Option.timeout(4000));
                            //JSONObject jsonObject = new JSONObject(clipMap);
                            //resty.json("http://localhost:8001/api/pieces", Resty.content(jsonObject));                             
                        }
                        
                        if(!last_index.equals(cmd[16]) && cmd[1].equals("playing")){ // Index changed, new clip played
                            logger.log(Level.INFO, "MeltedStatus - line: {0}", line);
                            redisPublisher.publish(mstaChannel, "CLIP CHANGED");
                            logger.log(Level.INFO, "MeltedStatus - line: {0}", "CLIP CHANGED");                            
                            endTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
                            ClipLog clip = new ClipLog();
                            clip.setName(last_name);
                            clip.setStart(startTime);
                            clip.setEnd(endTime);
                            
                            System.out.println("clip = " + clip.toString());
                            
                            //POSTing in Resty
                            //Gson gson = new Gson();
                            //String jsonClip = gson.toJson(clip);
                            //Map<String, String> clipMap = gson.fromJson(jsonClip, Map.class);
                            //Resty resty = new Resty(Resty.Option.timeout(4000));
                            //JSONObject jsonObject = new JSONObject(clipMap);
                            //resty.json("http://localhost:8001/api/pieces", Resty.content(jsonObject));                            
                            
                            startTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()); // new clip start time  
                        }
                        
                        last_name = cmd[2];
                        last_index = cmd[16];
                        last_mode = cmd[1];
                    }
                    
                }
            }
        } catch (IOException ex) {
            //TODO
        }
    }
    
    public void meltedReconnect(){
        //TODO: reconn routine
    }
    
    public void disconnect(){
        try {
            running = false;
            writer.close();
            reader.close();
            socket.close();
        } catch (IOException ex) {
            //TODO
            ex.printStackTrace();
        }
    }
    
    public static boolean connectToRedisPublisher(Logger logger, ConfigurationManager cfg, Jedis redisPublisher){
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
