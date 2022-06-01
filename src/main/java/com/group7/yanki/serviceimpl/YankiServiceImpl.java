package com.group7.yanki.serviceimpl;

import com.group7.yanki.dto.*;
import com.group7.yanki.exception.account.AccountCreationException;
import com.group7.yanki.exception.account.AccountNotFoundException;
import com.group7.yanki.model.Account;
import com.group7.yanki.model.Yanki;
import com.group7.yanki.repository.AccountRepository;
import com.group7.yanki.repository.YankiRepository;
import com.group7.yanki.service.AccountService;
import com.group7.yanki.service.YankiService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class YankiServiceImpl implements YankiService {

    @Autowired
    private YankiRepository yankiRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private MessageServiceImpl messageService;

    private RMapReactive<String, Transaction> transactionMap;

    public YankiServiceImpl(RedissonReactiveClient client) {
        this.transactionMap = client.getMap("transaction", new TypedJsonJacksonCodec(String.class, Transaction.class));
    }

    @Override
    public Flux<Yanki> getByPhone(Long phone) {
        return yankiRepository.findYankiByFromOrTo(phone, phone)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Not found phone:" + phone)))
                .doOnError(ex -> log.error("Not found phone: {}", phone, ex));
    }

    @Override
    public Mono<String> makeYanki(Yanki yanki) {
        return accountService.getByPhone(yanki.getFrom())
                .flatMap(accountFrom -> {
                    log.info("Account from:{}", accountFrom);
                    if (!Objects.isNull(accountFrom.getDebitCard())) {
                        return accountService.getByPhone(yanki.getTo())
                                .then(Mono.just(messageService.sendToAccount(yanki)));
                    }

                    if (accountFrom.getBalance() < yanki.getAmount()) {
                        return Mono.just(messageService.sendResult(Result.builder()
                                .to(accountFrom.getPhone())
                                .status("Failed")
                                .message("Not enough money")
                                .build()));
                    }

                    return accountService.updateAmountByPhone(accountFrom.getPhone(), -yanki.getAmount())
                            .then(Mono.just(messageService.sendResult(Result.builder()
                                    .to(accountFrom.getPhone())
                                    .status("Success")
                                    .message("You make a Yanki of " + yanki.getAmount() + " to " + yanki.getTo())
                                    .build())))
                            .then(accountService.getByPhone(yanki.getTo()))
                            .flatMap(accountTo -> {
                                if (!Objects.isNull(accountTo.getDebitCard())) {
                                    return Mono.just(messageService.sendToAccount(yanki));
                                }

                                return accountService.updateAmountByPhone(accountTo.getPhone(), yanki.getAmount())
                                        .then(yankiRepository.save(yanki))
                                        .then(Mono.just(messageService.sendResult(Result.builder()
                                                .to(accountTo.getPhone())
                                                .status("Success")
                                                .message("You received a Yanki of " + yanki.getAmount() + " from " + yanki.getFrom())
                                                .build())));
                            });
                }).map(result -> result ? "Transferring..." : "Failed!");
    }

    public Mono<String> yankiTransaction(YankiTransaction yankiTransaction) {
        return accountService.getByPhone(yankiTransaction.getFrom())
                .flatMap(accountFrom -> {
                    log.info("Account from:{}", accountFrom);
                    if (!Objects.isNull(accountFrom.getDebitCard())) {
                        Yanki yanki = new Yanki("", yankiTransaction.getFrom(), 0L, yankiTransaction.getAmount(), LocalDate.now());
                        return Mono.just(messageService.sendToAccount(yanki));
                    }

                    if (accountFrom.getBalance() < yankiTransaction.getAmount()) {
                        return Mono.just(messageService.sendResult(Result.builder()
                                .to(accountFrom.getPhone())
                                .status("Failed")
                                .message("Not enough money")
                                .build()));
                    }

                    return accountService.updateAmountByPhone(accountFrom.getPhone(), -yankiTransaction.getAmount())
                            .then(Mono.just(messageService.sendResult(Result.builder()
                                    .to(accountFrom.getPhone())
                                    .status("Success")
                                    .message("You make a Yanki of " + yankiTransaction.getAmount() + " to " + yankiTransaction.getTransaction())
                                    .build())));
                }).map(result -> result ? "Transferring..." : "Failed!");
    }

    @Override
    public Mono<String> payTransaction(YankiTransaction yankiTransaction) {
        Mono<Account> accountFrom = accountRepository.findAccountByPhone(yankiTransaction.getFrom())
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account does not exist")))
                .next();

        Mono<Transaction> transactionMono = transactionMap.get(yankiTransaction.getTransaction())
                .switchIfEmpty(Mono.error(new AccountNotFoundException("There is not transaction with this ID")));

        return accountFrom
                .zipWith(transactionMono)
                .flatMap(accounts -> {
                    Account from = accounts.getT1();
                    Transaction transaction = accounts.getT2();

                    if (transaction.getState().equalsIgnoreCase("Expired") ||
                            transaction.getExpiration().isBefore(LocalDateTime.now())
                    ) {
                        return Mono.error(new AccountCreationException("The transaction request has expired"));
                    }

                    if (!Objects.equals(transaction.getAmountFx(), yankiTransaction.getAmount()))
                        return Mono.error(new AccountCreationException("The transaction is for this amount:" + yankiTransaction.getAmount()));

                    if (!Objects.equals(transaction.getNumber(), yankiTransaction.getFrom().toString()))
                        return Mono.error(new AccountCreationException("Incorrect account for source transaction"));

                    messageService.sendTransaction(TransactionEvent.builder()
                            .transactionId(yankiTransaction.getTransaction())
                            .state("Paid")
                            .amount(yankiTransaction.getAmount())
                            .build());

                    return yankiTransaction(yankiTransaction);
                });
    }

    @Override
    public Mono<String> linkYanki(LinkRequest linkRequest) {
        linkRequest.setState("request");
        return accountService.getByPhone(linkRequest.getPhone())
                .flatMap(accountResponse -> {
                    linkRequest.setAmount(accountResponse.getBalance());
                    return Mono.just(messageService.sendToLink(linkRequest));
                })
                .map(result -> result ? "Linking Yanki to debit card..." : "Failed!");
    }
}
