package com.group7.yanki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkRequest {
    private Long phone;
    private String debitCard;
    private String state;
    private Double amount;
}
