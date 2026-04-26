package com.sliit.paf.smart_campus.repository;

import com.sliit.paf.smart_campus.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findAllByReportedByUserIsNullAndReportedByIsNotNull();

    List<Ticket> findAllByAssignedTechnicianUserIsNullAndAssignedTechnicianIsNotNull();
}
