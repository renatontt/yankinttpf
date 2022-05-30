package com.group7.yanki.dto;

import com.group7.yanki.model.Account;
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
public class AccountRequest {
    private String documentType;
    private String documentNumber;
    private Long phone;
    private String IMEI;
    private String email;
    private String debitCard;

    public Account toModel() {
        return Account.builder()
                .documentType(this.documentType)
                .documentNumber(this.documentNumber)
                .phone(this.phone)
                .IMEI(this.IMEI)
                .email(this.email)
                .debitCard(this.debitCard)
                .balance(0.0)
                .build();
    }
}
