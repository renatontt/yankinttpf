package com.group7.yanki;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLongReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
class YankiApplicationTests {

    @Autowired
    private ReactiveStringRedisTemplate template;

    @Autowired
    private RedissonReactiveClient client;

    @RepeatedTest(3)
    void springDataRedisTest() {
        ReactiveValueOperations<String, String> valueOperations = this.template.opsForValue();

        Long before = System.currentTimeMillis();

        Mono<Void> mono = Flux.range(1, 500000)
                .flatMap(i -> valueOperations.increment("user:1:visit"))
                .then();

        StepVerifier.create(mono)
                .verifyComplete();

        Long after = System.currentTimeMillis();

        System.out.println(after - before);

    }

    @RepeatedTest(3)
    void redissonTest() {
        RAtomicLongReactive atomicLong = this.client.getAtomicLong("user:2:visit");
        ReactiveValueOperations<String, String> valueOperations = this.template.opsForValue();

        Long before = System.currentTimeMillis();

        Mono<Void> mono = Flux.range(1, 500000)
                .flatMap(i -> atomicLong.incrementAndGet())
                .then();

        StepVerifier.create(mono)
                .verifyComplete();

        Long after = System.currentTimeMillis();

        System.out.println(after - before);

    }

}
