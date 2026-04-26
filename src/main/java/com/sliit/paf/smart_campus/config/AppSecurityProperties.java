package com.sliit.paf.smart_campus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private DevUsersProperties devUsers = new DevUsersProperties();
    private OAuth2Properties oauth2 = new OAuth2Properties();

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
        private DevUserProperties technician = new DevUserProperties(
                "dev-tech@smartcampus.local",
                "Development Technician",
                "dev-tech-pass"
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

    @Getter
    @Setter
    public static class OAuth2Properties {
        private String successRedirectUri = "/api/users/me";
        private GoogleProperties google = new GoogleProperties();
    }

    @Getter
    @Setter
    public static class GoogleProperties {
        private String clientId;
        private String clientSecret;
        private String clientName = "Google";
        private String redirectUri = "{baseUrl}/login/oauth2/code/{registrationId}";
        private String scope = "openid,profile,email";
    }
}
