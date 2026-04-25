package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void upsertOAuth2User_shouldCreateNewUserWithSafeDefaults() {
        when(userRepository.findByProviderAndProviderId("GOOGLE", "google-sub-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("google-user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.upsertOAuth2User(
                "Google-User@Example.com",
                "Google User",
                "google",
                "google-sub-123"
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("google-user@example.com");
        assertThat(savedUser.getDisplayName()).isEqualTo("Google User");
        assertThat(savedUser.getProvider()).isEqualTo("GOOGLE");
        assertThat(savedUser.getProviderId()).isEqualTo("google-sub-123");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.getActive()).isTrue();
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void upsertOAuth2User_shouldUpdateExistingUserWithoutDroppingRole() {
        User existingUser = User.builder()
                .id(10L)
                .email("admin@example.com")
                .displayName("Old Admin")
                .provider("GOOGLE")
                .providerId("google-sub-999")
                .role(Role.ADMIN)
                .active(false)
                .build();

        when(userRepository.findByProviderAndProviderId("GOOGLE", "google-sub-999"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        User updatedUser = userService.upsertOAuth2User(
                "admin@example.com",
                "Campus Admin",
                "google",
                "google-sub-999"
        );

        assertThat(updatedUser.getDisplayName()).isEqualTo("Campus Admin");
        assertThat(updatedUser.getProvider()).isEqualTo("GOOGLE");
        assertThat(updatedUser.getProviderId()).isEqualTo("google-sub-999");
        assertThat(updatedUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(updatedUser.getActive()).isTrue();
    }
}
