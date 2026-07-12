package com.parkflow.mall.merchant.security;
import java.util.List;
public record MerchantUser(String id,String username,List<String> roles) {}
