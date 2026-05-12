package com.ouro.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MercadoPagoConfig {

    @Value("${mercadopago.client-secret}")
    private String accessToken;

    @PostConstruct
    public void init() {
        com.mercadopago.MercadoPagoConfig.setAccessToken(accessToken);
    }
}
