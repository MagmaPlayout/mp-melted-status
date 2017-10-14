package meltedstatus.adminApi;

import meltedstatus.ClipLog;

/**
 * Interface to mp-admin-api rest module.
 *
 * @author rombus
 */
public interface MPAdminApi {
    void insertLog(ClipLog log);
}
