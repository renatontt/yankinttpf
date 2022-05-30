package com.group7.yanki.serviceimpl;

import com.group7.yanki.dto.AccountRequest;
import com.group7.yanki.dto.AccountResponse;
import com.group7.yanki.exception.account.AccountCreationException;
import com.group7.yanki.exception.account.AccountNotFoundException;
import com.group7.yanki.model.Account;
import com.group7.yanki.repository.AccountRepository;
import com.group7.yanki.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    private static final String NOT_FOUND_MESSAGE = "Account not found with phone: ";
    private static final String NOT_FOUND_MESSAGE_WITH_PHONE = "Account not found with phone: {}";

    @Autowired
    private AccountRepository accountRepository;

    private RMapReactive<Long, Account> accountMap;

    public AccountServiceImpl(RedissonReactiveClient client) {
        this.accountMap = client.getMap("account", new TypedJsonJacksonCodec(Long.class, Account.class));
    }

    @Override
    public Flux<AccountResponse> getAll() {
        return accountRepository.findAll()
                .map(AccountResponse::fromModel)
                .doOnComplete(() -> log.info("Retrieving all Accounts"));
    }

    @Override
    public Mono<AccountResponse> getByPhone(Long phone) {
        return accountMap.get(phone)
                .switchIfEmpty(
                        accountRepository.findAccountByPhone(phone)
                                .switchIfEmpty(Mono.error(new AccountNotFoundException(NOT_FOUND_MESSAGE + phone)))
                                .doOnError(ex -> log.error(NOT_FOUND_MESSAGE_WITH_PHONE, phone, ex))
                                .next()
                                .flatMap(a -> accountMap.fastPut(phone, a).thenReturn(a))
                ).map(AccountResponse::fromModel);
    }

    @Override
    public Mono<Void> deleteByPhone(Long phone) {
        return accountRepository.findAccountByPhone(phone)
                .switchIfEmpty(Mono.error(new AccountNotFoundException(NOT_FOUND_MESSAGE + phone)))
                .doOnError(ex -> log.error(NOT_FOUND_MESSAGE_WITH_PHONE, phone, ex))
                .next()
                .flatMap(existingAccount -> accountMap.fastRemove(phone).thenReturn(existingAccount))
                .flatMap(existingAccount ->
                        accountRepository.delete(existingAccount)
                )
                .doOnSuccess(ex -> log.info("Delete account with phone: {}", phone));
    }

    @Override
    public Mono<Void> deleteAll() {
        return accountMap.delete()
                .then(accountRepository.deleteAll())
                .doOnSuccess(ex -> log.info("Delete all accounts"));
    }

    @Override
    public Mono<AccountResponse> updateAmountByPhone(Long phone, Double amount) {
        return accountRepository.findAccountByPhone(phone)
                .next()
                .flatMap(accountFound -> {
                    accountFound.setBalance(accountFound.getBalance()+amount);
                    return accountMap.fastPut(accountFound.getPhone(),accountFound)
                            .thenReturn(accountFound);
                })
                .flatMap(account -> accountRepository.save(account))
                .doOnSuccess(x -> System.out.println("Account saved" + x))
                .map(AccountResponse::fromModel)
                .onErrorMap(ex -> new AccountCreationException(ex.getMessage()))
                .doOnSuccess(res -> log.info("Updated account with phone: {}", res.getPhone()))
                .doOnError(ex -> log.error("Error updating account ", ex));
    }

    @Override
    public Mono<AccountResponse> save(AccountRequest accountRequest) {
        return accountRepository.findAccountByPhone(accountRequest.getPhone())
                .hasElements()
                .flatMap(hasElement -> hasElement ?
                        Mono.error(new AccountCreationException("Phone already created")) :
                        Mono.just(accountRequest))
                .map(AccountRequest::toModel)
                .flatMap(account -> accountRepository.save(account))
                .map(AccountResponse::fromModel)
                .onErrorMap(ex -> new AccountCreationException(ex.getMessage()))
                .doOnSuccess(res -> log.info("Created new account with phone: {}", res.getPhone()))
                .doOnError(ex -> log.error("Error creating new Account ", ex));
    }

    @Override
    public Mono<AccountResponse> updateByPhone(Long phone, AccountRequest accountRequest) {
        return accountRepository.findAccountByPhone(accountRequest.getPhone())
                .next()
                .flatMap(accountFound -> {
                    accountFound.setDocumentType(accountRequest.getDocumentType());
                    accountFound.setDocumentNumber(accountRequest.getDocumentNumber());
                    accountFound.setEmail(accountRequest.getEmail());
                    return accountMap.fastPut(accountFound.getPhone(),accountFound)
                            .thenReturn(accountFound);
                })
                .flatMap(account -> accountRepository.save(account))
                .map(AccountResponse::fromModel)
                .onErrorMap(ex -> new AccountCreationException(ex.getMessage()))
                .doOnSuccess(res -> log.info("Updated account with phone: {}", res.getPhone()))
                .doOnError(ex -> log.error("Error updating account ", ex));
    }
}
