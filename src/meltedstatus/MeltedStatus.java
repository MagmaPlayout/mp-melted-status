package meltedstatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import meltedBackend.responseParser.parsers.SingleLineStatusParser;
import meltedBackend.responseParser.responses.UstaResponse;

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
//        UstaParser parser = new UstaParser();
        String[] lastCmd = null;
        String[] currentCmd = null;
        
        try {
            socket = new Socket(cfg.getMeltedHost(), cfg.getMeltedPort());
//            socket.setSoTimeout(5000);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
            writer.println("STATUS"); // Send telnet command
            running=true;

            SingleLineStatusParser parser = new SingleLineStatusParser(new UstaResponse());
            UstaResponse previousFrame = null;
            while(running){               
                String line = reader.readLine(); // Blocking method
                if(line != null){
                    UstaResponse currentFrame = (UstaResponse) parser.parse(line);
                    sp.eventHandler(currentFrame, previousFrame, line);
                    previousFrame = currentFrame.createCopy(UstaResponse.class);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("MELTED DISCONECTED!");
            sp.meltedDisonnected();
            disconnect();
        } catch (InstantiationException ex) {
            Logger.getLogger(MeltedStatus.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MeltedStatus.class.getName()).log(Level.SEVERE, null, ex);
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
             System.out.println("EXCEPTION WTF: " +ex.toString());
            ex.printStackTrace();
        }
    }   
   
}
