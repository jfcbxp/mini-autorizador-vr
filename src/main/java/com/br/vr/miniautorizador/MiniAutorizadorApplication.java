package com.br.vr.miniautorizador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Mini Autorizador — VR Benefícios
 *
 * <p>Authorizes benefit card transactions with:
 * <ul>
 *   <li>Java 25 Virtual Threads for high-throughput concurrency</li>
 *   <li>Optimistic Locking (@Version) for multi-instance safe balance debit</li>
 *   <li>No-if AuthorizationRule chain for extensible business rules</li>
 * </ul>
 */
@SpringBootApplication
@EnableRetry
public class MiniAutorizadorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniAutorizadorApplication.class, args);
    }
}
