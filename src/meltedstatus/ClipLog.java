package meltedstatus;

/**
 * A Clip class used as a data structure containing relevant data.
 *
 * @author cyberpunx
 */
public class ClipLog {
    private String name;
    private String start;
    private String end;
    private String idRawMedia;

    // Following data needs to be obtained from mp-playout-api
    private String pieceName;
    private String piecePath;
    private String resolution;
    private String duration;
    private int frameCount;
    private int frameRate;
    // -------------------------------------------------------

    @Override
    public String toString() {
        return "Clip{" 
            + "name=" + name + ", start=" + start
            + ", end=" + end + ", idRawMedia=" + idRawMedia
            + ", pieceName="+pieceName + ", piecePath= "+piecePath
            + ", resolution="+resolution + ", duration= "+duration
            + ", frameCount="+frameCount + ", frameRate= "+frameRate
            + '}';
    }

    /**
     * Initializes the clip with null data
     */
    public ClipLog() { }

    /**
     * Creates a clip with local data.
     *
     * @param name path of the media being played
     * @param start datetime when this media started playing
     * @param end datetime when this media has finished playing
     */
    public ClipLog(String name, String start, String end){
        this.name = name;
        this.start = start;
        this.end = end;
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
    
    public String getIdRawMedia() {
        return idRawMedia;
    }

    public void setIdRawMedia(String id) {
        this.idRawMedia = id;
    }

    public void setPieceName(String pieceName) {
        this.pieceName = pieceName;
    }

    public void setPiecePath(String piecePath) {
        this.piecePath = piecePath;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public String getPieceName() {
        return pieceName;
    }

    public String getPiecePath() {
        return piecePath;
    }

    public String getResolution() {
        return resolution;
    }

    public String getDuration() {
        return duration;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public int getFrameRate() {
        return frameRate;
    }
}
