package com.group7.yanki.serviceimpl;

import com.group7.yanki.dto.LinkRequest;
import com.group7.yanki.dto.Result;
import com.group7.yanki.dto.TransactionEvent;
import com.group7.yanki.model.Account;
import com.group7.yanki.model.Yanki;
import com.group7.yanki.repository.AccountRepository;
import com.group7.yanki.repository.YankiRepository;
import com.group7.yanki.service.AccountService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

@Service
@Slf4j
public class ListenService {

    private RMapReactive<Long, Account> accountMap;

    @Autowired
    private YankiRepository yankiRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MessageServiceImpl messageService;

    @Autowired
    private AccountService accountService;

    public ListenService(RedissonReactiveClient client) {
        this.accountMap = client.getMap("account", new TypedJsonJacksonCodec(Long.class, Account.class));
    }

    @Bean
    Consumer<Yanki> toyanki() {
        return yanki -> accountMap.get(yanki.getTo())
                .flatMap(accountTo -> {
                    if (!Objects.isNull(accountTo.getDebitCard())) {
                        return yankiRepository.save(yanki);
                    }
                    accountTo.setBalance(accountTo.getBalance() + yanki.getAmount());
                    return accountMap.fastPut(accountTo.getPhone(), accountTo)
                            .thenReturn(accountTo)
                            .flatMap(account -> accountRepository.save(accountTo))
                            .then(Mono.just(messageService.sendResult(Result.builder()
                                    .to(accountTo.getPhone())
                                    .status("Success")
                                    .message("You received a Yanki of " + yanki.getAmount() + " from " + yanki.getFrom())
                                    .build())))
                            .then(yankiRepository.save(yanki));
                })
                .doOnSuccess(x -> log.info("Account from: {}", x))
                .subscribe();
    }

    @Bean
    Consumer<LinkRequest> link() {
        return linkRequest -> {
            if (linkRequest.getState().equals("true")) {
                accountMap.get(linkRequest.getPhone())
                        .flatMap(account -> {
                            account.setBalance(0.0);
                            account.setDebitCard(linkRequest.getDebitCard());
                            return accountMap.fastPut(account.getPhone(), account)
                                    .thenReturn(account)
                                    .flatMap(accountAux -> accountRepository.save(accountAux))
                                    .then(Mono.just(messageService.sendResult(Result.builder()
                                            .to(account.getPhone())
                                            .status("Success")
                                            .message("You linked your card successfully")
                                            .build())));
                        })
                        .subscribe();
            } else if (linkRequest.getState().equals("false")) {
                messageService.sendResult(Result.builder()
                        .to(linkRequest.getPhone())
                        .status("Failed")
                        .message("There is not debit card with that ID")
                        .build());
            }
        };
    }

    @Bean
    Consumer<TransactionEvent> transaction() {
        return transactionEvent -> {

            Yanki yankiAux = Yanki.builder()
                    .from(0L)
                    .amount(transactionEvent.getAmount())
                    .to(Long.parseLong(transactionEvent.getNumber()))
                    .date(LocalDate.now())
                    .build();

            if (transactionEvent.getState().equals("Yanki")) {
                accountRepository.findAccountByPhone(Long.parseLong(transactionEvent.getNumber()))
                        .next()
                        .flatMap(account -> yankiRepository.save(yankiAux)
                                .thenReturn(account))
                        .flatMap(accountAux -> accountService.getByPhone(accountAux.getPhone())
                                .flatMap(accountTo -> {

                                    if (!Objects.isNull(accountTo.getDebitCard())) {
                                        return Mono.just(messageService.sendToAccount(Yanki.builder()
                                                .from(null)
                                                .to(accountAux.getPhone())
                                                .amount(transactionEvent.getAmount())
                                                .build()));
                                    }

                                    transactionEvent.setState("Completed");
                                    messageService.sendTransaction(transactionEvent);

                                    return accountService.updateAmountByPhone(accountTo.getPhone(), transactionEvent.getAmount())
                                            .then(yankiRepository.save(yankiAux))
                                            .then(Mono.just(messageService.sendResult(Result.builder()
                                                    .to(accountTo.getPhone())
                                                    .status("Success")
                                                    .message("You received a Yanki of " + transactionEvent.getAmount() + " from a transaction")
                                                    .build())));
                                }))
                        .subscribe();
            }
        };
    }

}
