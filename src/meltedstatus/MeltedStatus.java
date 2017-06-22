package meltedstatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import playoutCore.ConfigurationManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

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
            while(connectToRedisPublisher(logger, cfg, redisPublisher)){
                logger.log(Level.SEVERE, "MeltedStatus - ERROR: Could not connect to the Redis server. Is it running?\nExiting...");
                Thread.sleep(5000);
            }
        } catch (InterruptedException ex) {
            System.exit(1);
        }
        String mstaChannel = cfg.getRedisMstaChannel();
        
        
        try {
            socket = new Socket(cfg.getMeltedHost(), cfg.getMeltedPort());
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
            writer.println("STATUS");        // Send telnet command
            running=true;
            while(running){
                String line = reader.readLine(); // Blocking method
                if(line != null){
                    logger.log(Level.INFO, "MeltedStatus - ERROR: Could not connect to the Melted server. Retries exhausted. \nExiting...");
                    // Do something with line
                    // check if melted is running, what clip is playing, how many clips remains ...
                    // check telnet connection
                    // redisPublisher.publish(mstaChannel, "blabal");
                }
            }
        } catch (IOException ex) {
            //TODO
        }
    }
    
    public void reconnect(){
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
        logger.log(Level.INFO, "MeltedStatus - Attempt to connect to Redis Pub/Sub server...");
        try {
            redisPublisher.connect();
        }
        catch(JedisConnectionException e){
            logger.log(Level.SEVERE, "MeltedStatus - ERROR: Could not connect to the Redis server. Is it running?\nExiting...");
            return false;
        }
        
        return true;
    }
}
