package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.model.TicketComment;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.TicketCommentRepository;
import com.sliit.paf.smart_campus.service.AuthenticatedUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class TicketCommentController {

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @GetMapping
    public ResponseEntity<List<TicketComment>> getComments(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId));
    }

    @PostMapping
    public ResponseEntity<?> addComment(@PathVariable Long ticketId, @RequestBody Map<String, String> payload, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        String content = payload.get("content");

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Comment content cannot be empty"));
        }

        TicketComment comment = new TicketComment();
        comment.setTicketId(ticketId);
        comment.setAuthorEmail(currentUser.getEmail());
        comment.setAuthorDisplayName(currentUser.getDisplayName());
        comment.setContent(content.trim());

        return ResponseEntity.ok(ticketCommentRepository.save(comment));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long ticketId, @PathVariable Long commentId, @RequestBody Map<String, String> payload, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        
        return ticketCommentRepository.findById(commentId).map(comment -> {
            if (!comment.getAuthorEmail().equals(currentUser.getEmail()) && !currentUser.getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(403).body(Map.of("message", "You can only edit your own comments."));
            }
            
            String content = payload.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Comment content cannot be empty"));
            }
            
            comment.setContent(content.trim());
            return ResponseEntity.ok(ticketCommentRepository.save(comment));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long ticketId, @PathVariable Long commentId, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        
        return ticketCommentRepository.findById(commentId).map(comment -> {
            if (!comment.getAuthorEmail().equals(currentUser.getEmail()) && !currentUser.getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(403).body(Map.of("message", "You can only delete your own comments."));
            }
            
            ticketCommentRepository.delete(comment);
            return ResponseEntity.ok(Map.of("message", "Comment deleted successfully."));
        }).orElse(ResponseEntity.notFound().build());
    }
}