package com.group7.yanki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class TransactionEvent {
    private String transactionId;
    private String state;
    private String number;
    private String userId;
    private Double amount;
    private String typeAccount;
}
