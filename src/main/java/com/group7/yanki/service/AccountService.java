package com.group7.yanki.service;

import com.group7.yanki.dto.AccountRequest;
import com.group7.yanki.dto.AccountResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface AccountService {
    Flux<AccountResponse> getAll();

    Mono<AccountResponse> getByPhone(Long phone);

    Mono<Void> deleteByPhone(Long phone);

    Mono<Void> deleteAll();

    Mono<AccountResponse> updateAmountByPhone(Long phone, Double amount);

    Mono<AccountResponse> save(AccountRequest accountRequest);

    Mono<AccountResponse> updateByPhone(Long phone, AccountRequest accountRequest);
}
