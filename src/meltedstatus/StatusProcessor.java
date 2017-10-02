/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package meltedstatus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 *
 * @author debian
 */
public class StatusProcessor {
    
    Jedis redisPublisher;
    String mstaChannel;
    Logger logger;
    String startTime;
    String endTime;
    
    
    /**
     * CONSTRUCTOR INIT
     * 
     */
    public StatusProcessor(){
        this.logger = Logger.getLogger(MeltedStatus.class.getName());
        ConfigurationManager cfg = ConfigurationManager.getInstance();
        this.redisPublisher = new Jedis(cfg.getRedisHost(), cfg.getRedisPort(), cfg.getRedisReconnectionTimeout());
        
        // Connects to redis server
        Jedis redisPublisher = new Jedis(cfg.getRedisHost(), cfg.getRedisPort(), cfg.getRedisReconnectionTimeout());
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
    
    
    /**
     * Detects events and calls the right response.
     * 
     * @param lastCmd
     * @param currentCmd
     * @return 
     */
    public String eventHandler(String[] currentCmd, String[] lastCmd, String line){
        
        // Prints line to logger
        //logger.log(Level.INFO, "MeltedStatus - line: {0}", line); // COMENTO ESTO PARA NO SPAMMEAR LA CONSOLA
        
        
        
        if(lastCmd != null){
            if(lastCmd.length == 17){
                if(currentCmd.length == 17){

                    // Publish line to redis
                    redisPublisher.publish(mstaChannel, line);
        
                    // check and prints current mode  
                    //checkMode(currentCmd);     
                    
                    // checks mode change
                    checkChangeMode(currentCmd, lastCmd);    

                    // checks for index change
                    checkChangeIndex(currentCmd, lastCmd);



                }else{
                    wrongLenghtException(currentCmd);
                    return "Wrong Lenght Exception";
                }            
            }else{
                wrongLenghtException(lastCmd);
                return "Wrong Lenght Exception";
            }            
        }else{
            return "Last Cmd is Null";
        }
        return "";
    }    
    
    private void checkChangeMode(String[] currentCmd, String[] lastCmd){
        
        if(!lastCmd[1].equals(currentCmd[1])){
            redisPublisher.publish(mstaChannel, "CHANGE MODE FROM "+lastCmd[1]+" TO "+currentCmd[1]);
            logger.log(Level.INFO, "MeltedStatus - line: {0}", "CHANGE MODE FROM "+lastCmd[1]+" TO "+currentCmd[1]);
        }
        
        if(lastCmd[1].equals("playing") && currentCmd[1].equals("paused")){
            redisPublisher.publish(mstaChannel, "PLAYBACK PAUSED");
            logger.log(Level.INFO, "MeltedStatus - line: {0}", "PLAYBACK PAUSED"); 
            this.endTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
            sendClipToLog(lastCmd);            
            // sets new clip start time  
            this.startTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());  
        }
        if(lastCmd[1].equals("playing") && currentCmd[1].equals("stopped")){
            redisPublisher.publish(mstaChannel, "PLAYBACK STOPPED");
            logger.log(Level.INFO, "MeltedStatus - line: {0}", "PLAYBACK STOPPED");
            this.endTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
            sendClipToLog(lastCmd);            
            // sets new clip start time  
            this.startTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());  
        }
        
        if(!(lastCmd[1].equals("playing")) && currentCmd[1].equals("playing")){
            redisPublisher.publish(mstaChannel, "PLAYBACK STARTED");
            logger.log(Level.INFO, "MeltedStatus - line: {0}", "PLAYBACK STARTED"); 
            this.startTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
        }
    }
    
    private void checkChangeIndex(String[] currentCmd, String[] lastCmd){
        if(!lastCmd[16].equals(currentCmd[16])){ // Index changed
            redisPublisher.publish(mstaChannel, "INDEX CHANGED FROM "+lastCmd[16]+" TO "+currentCmd[16]);
            logger.log(Level.INFO, "MeltedStatus - line: {0}", "INDEX CHANGED FROM "+lastCmd[16]+" TO "+currentCmd[16]);
            
            this.endTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
            sendClipToLog(lastCmd);            
            // sets new clip start time  
            this.startTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());            
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
    
    private void sendClipToLog(String[] cmd){            
        // Creates json and sends it to playout log
        ClipLog clip = new ClipLog();
        clip.setName(cmd[2]);
        clip.setStart(this.startTime);
        clip.setEnd(this.endTime);
        System.out.println("clip = " + clip.toString());

        //POSTing in Resty
        //Gson gson = new Gson();
        //String jsonClip = gson.toJson(clip);
        //Map<String, String> clipMap = gson.fromJson(jsonClip, Map.class);
        //Resty resty = new Resty(Resty.Option.timeout(4000));
        //JSONObject jsonObject = new JSONObject(clipMap);
        //resty.json("http://localhost:8001/api/pieces", Resty.content(jsonObject));
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
