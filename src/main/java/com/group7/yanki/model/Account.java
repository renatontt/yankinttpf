package com.group7.yanki.model;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Slf4j
@Document(collection = "accounts")
public class Account {
    @Id
    private String id;
    private String documentType;
    private String documentNumber;
    private Long phone;
    private String IMEI;
    private String email;
    private String debitCard;
    private Double balance;
}
