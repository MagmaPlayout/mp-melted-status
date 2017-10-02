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
        
        
        StatusProcessor sp = new StatusProcessor();
        UstaParser parser = new UstaParser();
        String[] lastCmd = null;
        String[] currentCmd = null;
        
        
        
        try {
            socket = new Socket(cfg.getMeltedHost(), cfg.getMeltedPort());
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
            writer.println("STATUS"); // Send telnet command
            running=true;
            while(running){               
                String line = reader.readLine(); // Blocking method
                if(line != null){
                    
                    currentCmd = parser.getCmdParsed(line);                    
                    sp.eventHandler(currentCmd, lastCmd, line);
                    lastCmd = currentCmd;
                    
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
   
}
