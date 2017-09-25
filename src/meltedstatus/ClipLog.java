package meltedstatus;

import java.util.ArrayList;
import java.util.List;

/**
 * A Clip class used as a data structure containing relevant data.
 *
 * @author cyberpunx
 */
public class ClipLog {
    private String name;
    private String start;
    private String end;

   

    @Override
    public String toString() {
        return "Clip{" + "name=" + name + ", start=" + start + ", end=" + end + '}';
    }

    /**
     * Initializes the clip with null data
     */
    public ClipLog() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }
    
    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }
    

}

