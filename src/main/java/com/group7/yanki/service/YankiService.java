package com.group7.yanki.service;

import com.group7.yanki.dto.AccountRequest;
import com.group7.yanki.dto.AccountResponse;
import com.group7.yanki.dto.LinkRequest;
import com.group7.yanki.dto.YankiTransaction;
import com.group7.yanki.model.Yanki;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface YankiService {

    Flux<Yanki> getByPhone(Long phone);

    Mono<String> makeYanki(Yanki yanki);

    Mono<String> payTransaction(YankiTransaction yankiTransaction);

    Mono<String> linkYanki(LinkRequest linkRequest);
}
