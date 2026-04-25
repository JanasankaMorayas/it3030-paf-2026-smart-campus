package com.sliit.paf.smart_campus.config;

import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class LocalDevUserBootstrap implements ApplicationRunner {

    private final AppSecurityProperties securityProperties;
    private final UserService userService;

    public LocalDevUserBootstrap(AppSecurityProperties securityProperties, UserService userService) {
        this.securityProperties = securityProperties;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!securityProperties.getDevUsers().isEnabled()) {
            return;
        }

        AppSecurityProperties.DevUserProperties admin = securityProperties.getDevUsers().getAdmin();
        AppSecurityProperties.DevUserProperties user = securityProperties.getDevUsers().getUser();

        userService.ensureLocalUserExists(admin.getEmail(), admin.getDisplayName(), Role.ADMIN);
        userService.ensureLocalUserExists(user.getEmail(), user.getDisplayName(), Role.USER);
    }
}
