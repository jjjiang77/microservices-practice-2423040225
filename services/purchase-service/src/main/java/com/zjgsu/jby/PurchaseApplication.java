package com.zjgsu.jby;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@org.springframework.cloud.openfeign.EnableFeignClients
@SpringBootApplication
public class PurchaseApplication { public static void main(String[] args) { SpringApplication.run(PurchaseApplication.class,args); } }
