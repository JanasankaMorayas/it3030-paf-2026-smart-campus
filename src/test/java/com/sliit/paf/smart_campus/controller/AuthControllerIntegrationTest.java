package com.sliit.paf.smart_campus.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_shouldReturnHelpfulMessageWhenGoogleOAuthIsNotConfigured() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.googleOauthConfigured").value(false))
                .andExpect(jsonPath("$.message").value("Google OAuth2 login is not configured yet."))
                .andExpect(jsonPath("$.expectedRedirectUri").value("http://localhost:8080/login/oauth2/code/google"))
                .andExpect(jsonPath("$.successRedirectUri").value("http://127.0.0.1:5173/"));
    }

    @Test
    void authStatus_shouldExposeGoogleConfigurationState() throws Exception {
        mockMvc.perform(get("/api/auth/status").param("redirect_uri", "http://127.0.0.1:5174/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleOauthConfigured").value(false))
                .andExpect(jsonPath("$.successRedirectUri").value("http://127.0.0.1:5174/"));
    }
}
