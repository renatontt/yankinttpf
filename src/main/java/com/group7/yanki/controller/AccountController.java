package com.group7.yanki.controller;

import com.group7.yanki.dto.AccountRequest;
import com.group7.yanki.dto.AccountResponse;
import com.group7.yanki.service.AccountService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/yanki/accounts")
@AllArgsConstructor
@Slf4j
public class AccountController {
    private AccountService service;

    @GetMapping
    public Flux<AccountResponse> getAllAccounts() {
        return service.getAll();
    }

    @GetMapping("{phone}")
    public Mono<AccountResponse> getAccount(@PathVariable final Long phone) {
        return service.getByPhone(phone);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AccountResponse> saveAccount(@RequestBody final AccountRequest accountRequest) {
        return service.save(accountRequest);
    }

    @PutMapping("{phone}")
    public Mono<AccountResponse> updateAccount(@PathVariable final Long phone,
                                               @RequestBody final AccountRequest accountRequest) {
        return service.updateByPhone(phone, accountRequest);
    }

    @DeleteMapping("{phone}")
    public Mono<Void> deleteAccount(@PathVariable final Long phone) {
        return service.deleteByPhone(phone);
    }

    @DeleteMapping
    public Mono<Void> deleteAllAccounts() {
        return service.deleteAll();
    }
}
