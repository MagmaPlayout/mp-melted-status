package meltedstatus.playoutApi;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import meltedstatus.ClipLog;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

/**
 *
 * @author rombus
 */
public class PlayoutApi implements MPPlayoutApi {
    private final Resty resty;
    private final Gson gson;
    private final ConfigurationManager cfg;
    private final String baseUrl;

    public PlayoutApi(Resty resty, Gson gson, ConfigurationManager cfg){
        this.resty = resty;
        this.gson = gson;
        this.cfg = cfg;
        this.baseUrl = cfg.getPlayoutAPIRestBaseUrl();
    }

    /**
     * Returns the idRawMedia that corresponds to the clipPath piece.
     * 
     * @param pieceId
     * @return
     */
    @Override
    public String getIdRawMedia(int pieceId) {
        String idRawMedia = "";
        
        try {
            JSONObject jobj = resty.json(baseUrl+"pieces/"+pieceId).toObject();
            idRawMedia = (String) jobj.get("mediaId");
            
        } catch (UnsupportedEncodingException ex) {
            // TODO: HANDLE
            Logger.getLogger(PlayoutApi.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            // TODO: HANDLE
            Logger.getLogger(PlayoutApi.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            // TODO: HANDLE
            Logger.getLogger(PlayoutApi.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return idRawMedia;
    }

    /**
     * Loads all the "piece" data into the ClipLog.
     * 
     * @param clip
     * @return
     */
    @Override
    public ClipLog loadPieceData(ClipLog clip) {
        try {
            String clipName = clip.getName().replace("\"", "");
            int pieceId = Integer.parseInt(clipName); // The "name" holds the piece ID of the table Pieces. This is a convention
            JSONObject piece = resty.json(baseUrl+"pieces/"+pieceId).toObject();

            clip.setResolution(piece.get("resolution").toString());
            clip.setDuration(piece.get("duration").toString());
            clip.setFrameCount(Integer.parseInt(piece.get("frameCount").toString()));
            clip.setFrameRate(Integer.parseInt(piece.get("frameRate").toString()));
            clip.setPieceName(piece.get("name").toString());
            clip.setPiecePath(piece.get("path").toString());
            
        } catch (IOException ex) {
            //TODO HANDLE
            Logger.getLogger(PlayoutApi.class.getName()).log(Level.SEVERE, null, ex);
        } catch(NumberFormatException e){
            //TODO HANDLE
            Logger.getLogger(PlayoutApi.class.getName()).log(Level.SEVERE, null, e);
        } catch (JSONException ex) {
            //TODO HANDLE
            Logger.getLogger(PlayoutApi.class.getName()).log(Level.SEVERE, null, ex);
        }

        return clip;
    }
}
