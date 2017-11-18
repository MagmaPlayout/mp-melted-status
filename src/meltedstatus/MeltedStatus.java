package meltedstatus;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.statuscmd.StatusCmdProcessor;
import meltedBackend.telnetClient.MeltedTelnetClient;

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
        
        listenStatusForever(logger, cfg);
    }
    
    /**
     * Creates a connection to Melted, sends a STATUS cmd and start's listening to it.
     * If something fails it'll reconnect to Melted, send STATUS cmd and start to listen again, forever.
     * 
     * @param logger
     * @param cfg 
     */
    private void listenStatusForever(Logger logger, ConfigurationManager cfg){
        MeltedTelnetClient melted = new MeltedTelnetClient();
        if(!connectToMelted(logger, cfg, melted)){ //handles retries by its own
            logger.log(Level.SEVERE, "MeltedStatus - ERROR: Could not connect to the Melted server. Retries exhausted. \n");
        }
        
        StatusCmdProcessor sp = new StatusProcessor();
        try {
            
            melted.listenStatus(sp); // Blocking method
            
        } catch (MeltedCommandException ex) {
            logger.log(Level.SEVERE, "MeltedStatus - ERROR: An error occurred while listening to STATUS CMD. \n");
        }
        
        sp.meltedDisonnected();
        melted.disconnect();
        listenStatusForever(logger, cfg); // If a MeltedCommandException is detected, it'll start the process all over again
    }
    
    public boolean connectToMelted(Logger logger, ConfigurationManager cfg, MeltedTelnetClient melted){
        boolean connected = melted.connect(cfg.getMeltedHost(), cfg.getMeltedPort(), logger);
        while(!connected){
            // Handling reconnections
            connected = melted.reconnect(cfg.getMeltedReconnectionTries(), cfg.getMeltedReconnectionTimeout());
        }
        if(!connected){
            logger.log(Level.SEVERE, "MeltedStatus - ERROR: Could not connect to the Melted server. Is it running?");
            return false;
        }
        
        return true;
    }
   
}
