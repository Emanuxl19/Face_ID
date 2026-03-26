package com.faceid.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita tarefas agendadas (@Scheduled) e execução assíncrona (@Async).
 * @Async é usado pelo AuditLogService para não bloquear o fluxo principal.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {}
