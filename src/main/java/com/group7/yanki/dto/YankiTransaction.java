package com.group7.yanki.dto;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Slf4j
public class YankiTransaction {
    private Long from;
    private String transaction;
    private Double amount;
}
