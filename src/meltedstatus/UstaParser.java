package meltedstatus;

import java.util.HashMap;
import java.util.Map;


/**
 * 
 * USTA {unit}
    Get the unit status report.
    The response body contains the following fields delimited by spaces:
    [UNIT] - unit number: U0, U1, U2, or U3 without the "U" prefix
    [MODE] - mode: (offline|not_loaded|playing|stopped|paused|disconnected|unknown)
      "unknown" means the unit has not been added
      "disconnected" means the server has closed the connection to the client.
    [FILENAME] - current clip name: filename
    [CURR_POS] - current position: in absolute frame number units
    [SPEED] - speed: playback rate in (percent * 10)
    [FPS] - fps: frames-per-second of loaded clip
    [CURR_IN] - current in-point: starting frame number
    [CURR_PUT] - current out-point: ending frame number
    [LEN] - length of the clip
    [BUFF_FILENAME] - buffer tail clip name: filename
    [BUFF_CURR_POS] - buffer tail position: in absolute frame number units
    [BUFF_CURR_IN] - buffer tail in-point: starting frame number
    [BUFF_CURR_OUT] - buffer tail out-point: ending frame number
    [BUFF_LEN] - buffer tail length: length of clip in buffer tail
    [SEEKABLE] - seekable flag: indicates if the current clip is seekable (relates to head)
    [PL_GEN] - playlist generation number
    [INDEX] - current clip index (relates to head)

USTA Examples: 
0 playing \"/home/debian/Descargas/videos/output/hdm_rock_solid.avi\" 195 1000 25.00 0 195 196 \"/home/debian/Descargas/videos/output/hdm_rock_solid.avi\" 195 0 195 196 1 2 0
0 playing \"/home/debian/Descargas/videos/output/hdm_rock_solid.avi\" 0 1000 25.00 0 195 196 \"/home/debian/Descargas/videos/output/hdm_rock_solid.avi\" 0 0 195 196 1 2 1
 */


public class UstaParser {

    public String[] getCmdParsed(String line){      
        String[] words = line.split(" ");        
        return words;        
    }
    
    // NOT USED YET
    public Map getUstaMap(String line){
        Map<String, String> map = new HashMap<String, String>();        
        String[] words = line.split(" ");
        map.put("UNIT", words[0]);
        map.put("MODE", words[1]);
        map.put("FILENAME", words[2]);
        map.put("CURR_POS", words[3]);
        map.put("SPEED", words[4]);
        map.put("FPS", words[5]);
        map.put("CURR_IN", words[6]);
        map.put("CURR_OUT", words[7]);
        map.put("LEN", words[8]);
        map.put("BUFF_FILENAME", words[9]);
        map.put("BUFF_CURR_POS", words[10]);
        map.put("BUFF_CURR_IN", words[11]);
        map.put("BUFF_CURR_OUT", words[12]);
        map.put("BUFF_LEN", words[13]);
        map.put("SEEKABLE", words[14]);
        map.put("PL_GEN", words[15]);
        map.put("INDEX", words[16]);        
        return map;        
    }   

}
