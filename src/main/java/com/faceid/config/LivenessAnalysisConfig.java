package com.faceid.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configura o pool de threads dedicado à análise passiva de vivacidade.
 *
 * Os 3 algoritmos (Laplaciano, LBP, Sobel) são completamente independentes e
 * operam em read-only sobre o mesmo Mat — podem rodar em paralelo com segurança.
 * Um pool fixo de 3 threads garante que cada análise tenha seu próprio thread
 * sem overhead de criação.
 *
 * Threads são daemon (setDaemon = true) para não bloquear o shutdown da aplicação.
 * Identificadas por nome ("liveness-analyzer-N") para facilitar diagnóstico em
 * thread dumps.
 */
@Configuration
public class LivenessAnalysisConfig {

    @Bean("livenessExecutor")
    public ExecutorService livenessExecutor() {
        AtomicInteger counter = new AtomicInteger(1);
        return Executors.newFixedThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable, "liveness-analyzer-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }
}
