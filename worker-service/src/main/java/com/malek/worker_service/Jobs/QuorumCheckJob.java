package com.malek.worker_service.Jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * For public bookings approaching their slot_start:
 *   - If enough players joined (slots_filled >= min_players) -> CONFIRM.
 *   - Otherwise -> CANCEL.
 *
 * Runs every 30 seconds. Window: bookings starting within the next 30 minutes.
 */

@Component
@RequiredArgsConstructor
@Slf4j

public class QuorumCheckJob {
    private final JdbcTemplate jdbc;

    private static final String CONFIRM_SQL = """
            UPDATE bookings
            SET state = 'CONFIRMED',
                updated_at = NOW()
            WHERE state = 'HELD'
              AND visibility = 'PUBLIC'
              AND slots_filled >= min_players
              AND slot_start <= NOW() + INTERVAL '30 minutes'
            """;

    private static final String CANCEL_SQL = """
            UPDATE bookings
            SET state = 'CANCELLED',
                updated_at = NOW()
            WHERE state = 'HELD'
                AND visibility = 'PUBLIC'
                AND slots_filled < min_players
                AND slot_start <= NOW() + INTERVAL '30 minutes'
            """;

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void run() {
        int confirmed = jdbc.update(CONFIRM_SQL);
        int cancelled = jdbc.update(CANCEL_SQL);

        if (confirmed > 0 || cancelled > 0) {
            log.info("QuorumCheckJob: confirmed={} cancelled={}", confirmed, cancelled);

        }

    }
}