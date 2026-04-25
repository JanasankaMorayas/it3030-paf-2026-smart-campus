package com.sliit.paf.smart_campus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private DevUsersProperties devUsers = new DevUsersProperties();

    @Getter
    @Setter
    public static class DevUsersProperties {
        private boolean enabled = true;
        private DevUserProperties admin = new DevUserProperties(
                "dev-admin@smartcampus.local",
                "Development Admin",
                "dev-admin-pass"
        );
        private DevUserProperties user = new DevUserProperties(
                "dev-user@smartcampus.local",
                "Development User",
                "dev-user-pass"
        );
    }

    @Getter
    @Setter
    public static class DevUserProperties {
        private String email;
        private String displayName;
        private String password;

        public DevUserProperties() {
        }

        public DevUserProperties(String email, String displayName, String password) {
            this.email = email;
            this.displayName = displayName;
            this.password = password;
        }
    }
}
