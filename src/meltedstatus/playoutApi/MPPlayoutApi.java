package meltedstatus.playoutApi;

import meltedstatus.ClipLog;

/**
 * Interface to mp-playout-api rest module.
 *
 * @author rombus
 */
public interface MPPlayoutApi {
    String getIdRawMedia(int pieceId);
    ClipLog loadPieceData(ClipLog clip);
}
