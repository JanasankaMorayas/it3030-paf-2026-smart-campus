package com.sliit.paf.smart_campus.repository;

import com.sliit.paf.smart_campus.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
