package com.group7.yanki.dto;

import com.group7.yanki.model.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse implements Serializable {
    private String id;
    private String documentType;
    private String documentNumber;
    private Long phone;
    private String IMEI;
    private String email;
    private String debitCard;
    private Double balance;

    private static final long serialVersionUID = 1L;

    public static AccountResponse fromModel(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .documentType(account.getDocumentType())
                .documentNumber(account.getDocumentNumber())
                .phone(account.getPhone())
                .IMEI(account.getIMEI())
                .email(account.getEmail())
                .debitCard(account.getDebitCard())
                .balance(account.getBalance())
                .build();
    }
}
