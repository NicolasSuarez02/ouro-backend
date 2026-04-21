package com.ouro.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    // Bucket por IP: 10 intentos por minuto
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    public boolean tryConsumeLogin(String clientIp) {
        Bucket bucket = loginBuckets.computeIfAbsent(clientIp, ip ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                .build()
        );
        return bucket.tryConsume(1);
    }
}
