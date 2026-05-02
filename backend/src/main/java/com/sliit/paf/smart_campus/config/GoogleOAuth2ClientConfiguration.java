package com.sliit.paf.smart_campus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Configuration
@Conditional(GoogleOAuth2ClientConfiguration.GoogleOAuthConfiguredCondition.class)
public class GoogleOAuth2ClientConfiguration {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(AppSecurityProperties securityProperties) {
        return new InMemoryClientRegistrationRepository(buildGoogleClientRegistration(securityProperties));
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    private ClientRegistration buildGoogleClientRegistration(AppSecurityProperties securityProperties) {
        AppSecurityProperties.GoogleProperties google = securityProperties.getOauth2().getGoogle();
        List<String> scopes = Arrays.stream(google.getScope().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();

        return ClientRegistration.withRegistrationId("google")
                .clientId(google.getClientId().trim())
                .clientSecret(google.getClientSecret().trim())
                .clientName(google.getClientName().trim())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(google.getRedirectUri().trim())
                .scope(scopes)
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .build();
    }

    static class GoogleOAuthConfiguredCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String clientId = context.getEnvironment().getProperty("app.security.oauth2.google.client-id");
            String clientSecret = context.getEnvironment().getProperty("app.security.oauth2.google.client-secret");
            return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
        }
    }
}
