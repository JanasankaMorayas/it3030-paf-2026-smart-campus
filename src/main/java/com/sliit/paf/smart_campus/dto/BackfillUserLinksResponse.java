package com.sliit.paf.smart_campus.dto;

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
public class BackfillUserLinksResponse {

    private int recordsScanned;
    private int recordsLinked;
    private int recordsSkipped;
    private int bookingsLinked;
    private int ticketReportersLinked;
    private int ticketTechniciansLinked;
}
