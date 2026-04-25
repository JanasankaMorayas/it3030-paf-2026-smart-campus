package com.sliit.paf.smart_campus.config;

import com.sliit.paf.smart_campus.service.CustomOAuth2UserService;
import com.sliit.paf.smart_campus.service.CustomOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
            CustomOAuth2UserService customOAuth2UserService,
            CustomOidcUserService customOidcUserService,
            OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/error", "/login", "/login/**", "/oauth2/**", "/login/oauth2/**", "/oauth2/authorization/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/resources/**").permitAll()
                        .requestMatchers("/api/resources/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/status").hasRole("ADMIN")
                        .requestMatchers("/api/bookings/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/assign").hasRole("ADMIN")
                        .requestMatchers("/api/tickets/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN)));

        if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                            .oidcUserService(customOidcUserService))
                    .successHandler(oAuth2AuthenticationSuccessHandler));
        }

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(AppSecurityProperties securityProperties, PasswordEncoder passwordEncoder) {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();

        if (!securityProperties.getDevUsers().isEnabled()) {
            return manager;
        }

        UserDetails adminUser = org.springframework.security.core.userdetails.User
                .withUsername(securityProperties.getDevUsers().getAdmin().getEmail())
                .password(passwordEncoder.encode(securityProperties.getDevUsers().getAdmin().getPassword()))
                .roles("ADMIN")
                .build();

        UserDetails standardUser = org.springframework.security.core.userdetails.User
                .withUsername(securityProperties.getDevUsers().getUser().getEmail())
                .password(passwordEncoder.encode(securityProperties.getDevUsers().getUser().getPassword()))
                .roles("USER")
                .build();

        manager.createUser(adminUser);
        manager.createUser(standardUser);

        return manager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
