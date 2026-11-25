package net.server_backup.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Initializes the existing Dropbox authorization settings without overwriting user data. */
public final class DropboxCredentials {
    public interface Store {
        boolean contains(String key);
        String get(String key);
        void set(String key, String value);
    }
    public void initialize(Store store) {
        if (!store.contains("Cloud.Dropbox")) {
            store.set("Cloud.Dropbox.AppKey", "appKey");
            store.set("Cloud.Dropbox.AppSecret", "appSecret");
            return;
        }
        String key = store.get("Cloud.Dropbox.AppKey");
        if (key != null && !key.isBlank() && !key.equals("appKey") && !store.contains("Cloud.Dropbox.ActivationLink")) {
            store.set("Cloud.Dropbox.ActivationLink", "https://www.dropbox.com/oauth2/authorize?client_id="
                    + URLEncoder.encode(key, StandardCharsets.UTF_8) + "&response_type=code&token_access_type=offline");
            if (!store.contains("Cloud.Dropbox.AccessToken")) store.set("Cloud.Dropbox.AccessToken", "accessToken");
        }
    }
}
