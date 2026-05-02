package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.model.PasswordResetToken;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.PasswordResetTokenRepository;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Transactional
    public void createPasswordResetTokenForUser(String email) {
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            // Delete old tokens
            tokenRepository.deleteByUser(user);
            
            // 6 digit token
            String token = String.format("%06d", new java.util.Random().nextInt(999999));
            
            PasswordResetToken myToken = new PasswordResetToken();
            myToken.setUser(user);
            myToken.setToken(token);
            // Token valid for 15 minutes
            myToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
            tokenRepository.save(myToken);
            
            sendVerificationEmail(user.getEmail(), token);
        }
    }
    
    private void sendVerificationEmail(String email, String token) {
        String subject = "Smart Campus - Password Reset Verification Code";
        String message = "Hello,\n\nYour password reset verification code is: " + token + "\n\nPlease enter this 6-digit code in the application to reset your password.\n\nThis code will expire in 15 minutes.\n\nThank you,\nSmart Campus Team";
        
        System.out.println("=================================================");
        System.out.println("MOCK EMAIL SEND (To: " + email + ")");
        System.out.println("Code: " + token);
        System.out.println("=================================================");
        
        if (mailSender != null) {
            try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setTo(email);
                mailMessage.setSubject(subject);
                mailMessage.setText(message);
                mailSender.send(mailMessage);
            } catch (Exception e) {
                System.out.println("Failed to send real email. Error: " + e.getMessage());
            }
        }
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);
        
        if (tokenOptional.isPresent()) {
            PasswordResetToken passToken = tokenOptional.get();
            
            if (passToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                return false; // Token expired
            }
            
            User user = passToken.getUser();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            tokenRepository.deleteByUser(user);
            return true;
        }
        return false;
    }
}