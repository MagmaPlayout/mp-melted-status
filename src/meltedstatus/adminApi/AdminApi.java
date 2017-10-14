package meltedstatus.adminApi;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import meltedstatus.ClipLog;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

/**
 *
 * @author rombus
 */
public class AdminApi implements MPAdminApi{
    private final Resty resty;
    private final Gson gson;
    private final ConfigurationManager cfg;
    private final String baseUrl;

    public AdminApi(Resty resty, Gson gson, ConfigurationManager cfg) {
        this.resty = resty;
        this.gson = gson;
        this.cfg = cfg;
        this.baseUrl = cfg.getAdminAPIRestBaseUrl();
    }

    @Override
    public void insertLog(ClipLog log) {
        try {
            String jsonClip = gson.toJson(log);
            Map<String, String> clipMap = gson.fromJson(jsonClip, Map.class);
            JSONObject jsonObject = new JSONObject(clipMap);

            resty.json(baseUrl+"playoutLog", Resty.content(jsonObject));
        } catch (IOException ex) {
            //TODO: HANDLE
            Logger.getLogger(AdminApi.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
