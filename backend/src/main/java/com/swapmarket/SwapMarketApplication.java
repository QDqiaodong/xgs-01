package com.swapmarket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.swapmarket.mapper")
public class SwapMarketApplication {
    public static void main(String[] args) {
        SpringApplication.run(SwapMarketApplication.class, args);
    }
}
