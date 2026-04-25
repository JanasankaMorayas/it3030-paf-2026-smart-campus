package com.sliit.paf.smart_campus.dto;

import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {

    private Long id;
    private String email;
    private String displayName;
    private String provider;
    private String providerId;
    private Role role;
    private Boolean active;
    private boolean authenticated;

    public static AuthUserResponse from(User user) {
        return AuthUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .role(user.getRole())
                .active(user.getActive())
                .authenticated(true)
                .build();
    }
}
