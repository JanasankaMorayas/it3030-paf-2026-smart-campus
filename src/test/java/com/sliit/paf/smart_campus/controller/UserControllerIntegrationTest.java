package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.dto.UpdateUserRoleRequest;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void getCurrentUser_shouldReturnAuthenticatedUser() throws Exception {
        userRepository.save(User.builder()
                .email("member@example.com")
                .displayName("Member User")
                .provider("LOCAL_DEV")
                .providerId("member@example.com")
                .role(Role.USER)
                .active(true)
                .build());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void getCurrentUser_shouldReturnAuthenticatedOAuth2User() throws Exception {
        userRepository.save(User.builder()
                .email("google-user@example.com")
                .displayName("Google User")
                .provider("GOOGLE")
                .providerId("google-sub-123")
                .role(Role.USER)
                .active(true)
                .build());

        mockMvc.perform(get("/api/users/me")
                        .with(oauth2Login()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .attributes(attributes -> {
                                    attributes.put("email", "google-user@example.com");
                                    attributes.put("name", "Google User");
                                    attributes.put("sub", "google-sub-123");
                                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("google-user@example.com"))
                .andExpect(jsonPath("$.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void getCurrentUser_shouldProvisionMissingAuthenticatedOidcUser() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .idToken(token -> {
                                    token.claim("email", "new-google-user@example.com");
                                    token.claim("name", "New Google User");
                                    token.claim("sub", "google-sub-456");
                                })
                                .userInfoToken(userInfo -> {
                                    userInfo.claim("email", "new-google-user@example.com");
                                    userInfo.claim("name", "New Google User");
                                    userInfo.claim("sub", "google-sub-456");
                                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new-google-user@example.com"))
                .andExpect(jsonPath("$.authenticated").value(true));

        User savedUser = userRepository.findByEmailIgnoreCase("new-google-user@example.com").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(savedUser.getDisplayName()).isEqualTo("New Google User");
        org.assertj.core.api.Assertions.assertThat(savedUser.getActive()).isTrue();
    }

    @Test
    void getCurrentUser_shouldReturnUnauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getAllUsers_shouldReturnUsersForAdmin() throws Exception {
        userRepository.save(User.builder()
                .email("user-one@example.com")
                .displayName("User One")
                .provider("LOCAL_DEV")
                .providerId("user-one@example.com")
                .role(Role.USER)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .email("user-two@example.com")
                .displayName("User Two")
                .provider("LOCAL_DEV")
                .providerId("user-two@example.com")
                .role(Role.ADMIN)
                .active(true)
                .build());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email").value("user-one@example.com"));
    }

    @Test
    @WithMockUser(username = "basic@example.com", roles = "USER")
    void getAllUsers_shouldReturnForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void updateUserRole_shouldUpdateRoleForAdmin() throws Exception {
        User savedUser = userRepository.save(User.builder()
                .email("change-role@example.com")
                .displayName("Change Role")
                .provider("LOCAL_DEV")
                .providerId("change-role@example.com")
                .role(Role.USER)
                .active(true)
                .build());

        UpdateUserRoleRequest request = UpdateUserRoleRequest.builder()
                .role("ADMIN")
                .build();

        mockMvc.perform(patch("/api/users/{id}/role", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
