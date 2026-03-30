package com.sliit.paf.smart_campus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String type; // e.g., ROOM, LAB, EQUIPMENT

    private Integer capacity;

    private String location;

    private String status; // e.g., ACTIVE, OUT_OF_SERVICE
}
