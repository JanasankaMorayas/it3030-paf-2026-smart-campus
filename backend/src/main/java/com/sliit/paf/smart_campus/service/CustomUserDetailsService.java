package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.config.AppSecurityProperties;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AppSecurityProperties securityProperties;
    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(UserRepository userRepository, 
                                    AppSecurityProperties securityProperties,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.securityProperties = securityProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find the user in the database
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(username);
        
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            
            // If the user has a password in DB, use it
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                return org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build();
            }
        }
        
        // Fallback to dev users if DB user doesn't have a password, or user not found
        // but requested username matches a dev user configuration.
        if (securityProperties.getDevUsers().isEnabled()) {
            AppSecurityProperties.DevUsersProperties devUsers = securityProperties.getDevUsers();
            
            if (devUsers.getAdmin().getEmail().equalsIgnoreCase(username)) {
                return buildDevUser(devUsers.getAdmin().getEmail(), devUsers.getAdmin().getPassword(), "ADMIN");
            } else if (devUsers.getUser().getEmail().equalsIgnoreCase(username)) {
                return buildDevUser(devUsers.getUser().getEmail(), devUsers.getUser().getPassword(), "USER");
            } else if (devUsers.getTechnician().getEmail().equalsIgnoreCase(username)) {
                return buildDevUser(devUsers.getTechnician().getEmail(), devUsers.getTechnician().getPassword(), "TECHNICIAN");
            }
        }

        throw new UsernameNotFoundException("User not found with email: " + username);
    }

    private UserDetails buildDevUser(String email, String password, String role) {
        return org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password(passwordEncoder.encode(password))
                .roles(role)
                .build();
    }
}
