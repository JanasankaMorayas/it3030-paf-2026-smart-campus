package com.sliit.paf.smart_campus.repository;

import com.sliit.paf.smart_campus.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    Optional<Notification> findByIdAndRecipientIdentifierIgnoreCase(Long id, String recipientIdentifier);
}
