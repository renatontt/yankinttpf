package com.group7.yanki.repository;

import com.group7.yanki.model.Yanki;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface YankiRepository extends ReactiveMongoRepository<Yanki,String> {
    Flux<Yanki> findYankiByFromOrTo(Long phoneFrom, Long phoneTo);
}
