package net.server_backup.core;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DropboxCredentialsTest {
    @Test void createsLinkOnlyAfterRealAppKeyAndPreservesTokens() {
        Map<String, String> values = new HashMap<>();
        DropboxCredentials.Store store = new DropboxCredentials.Store() {
            public boolean contains(String key) { return values.containsKey(key) || values.keySet().stream().anyMatch(k -> k.startsWith(key + ".")); }
            public String get(String key) { return values.get(key); }
            public void set(String key, String value) { values.put(key, value); }
        };
        DropboxCredentials credentials = new DropboxCredentials();
        credentials.initialize(store);
        credentials.initialize(store);
        assertFalse(store.contains("Cloud.Dropbox.ActivationLink"));
        values.put("Cloud.Dropbox.AppKey", "a+b");
        values.put("Cloud.Dropbox.AccessToken", "existing");
        credentials.initialize(store);
        assertTrue(values.get("Cloud.Dropbox.ActivationLink").contains("client_id=a%2Bb"));
        assertEquals("existing", values.get("Cloud.Dropbox.AccessToken"));
    }
}
