package com.malek.worker_service.Jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sweeps expired idempotency records out of the table so it doesn't grow forever.
 * Runs once a day, at 03:00 UTC (low-traffic hour).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyCleanupJob {

    private final JdbcTemplate jdbc;

    private static final String SQL = """
            DELETE FROM idempotency_records
            WHERE expires_at < NOW()
            """;

    @Scheduled(cron = "0 0 3 * * *")   // sec min hour day month day-of-week
    public void run() {
        int affected = jdbc.update(SQL);
        log.info("IdempotencyCleanupJob: deleted {} expired idempotency records", affected);
    }
}
