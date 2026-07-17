package com.parkflow.mall.merchant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MerchantConfig {

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}