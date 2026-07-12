package com.parkflow.mall.merchant.config;
import org.springframework.context.annotation.*; import org.springframework.web.client.RestTemplate;
@Configuration public class MerchantConfig { @Bean RestTemplate restTemplate(){return new RestTemplate();} }
