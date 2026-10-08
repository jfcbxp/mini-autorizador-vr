package com.br.vr.miniautorizador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class MiniAutorizadorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniAutorizadorApplication.class, args);
    }
}
