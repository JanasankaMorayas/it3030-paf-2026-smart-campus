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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
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
