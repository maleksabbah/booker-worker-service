package com.malek.worker_service.Jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Auto-flips CONFIRMED bookings to NO_SHOW when nobody seated them
 * within 15 minutes after slot_start.
 *
 * no_show_by_user_id stays NULL to mark this as a system action
 * (vs a staff member manually marking no-show).
 *
 * Runs every minute.
 */

@Component
@RequiredArgsConstructor
@Slf4j

public class NoShowDetectionJob {
    private final JdbcTemplate jdbc;

    private static final String SQL = """
            UPDATE bookings
            SET state = 'NO_SHOW',
                no_show_by_user_id = NULL,
                updated_at = NOW()
            WHERE state = 'CONFIRMED'
              AND slot_start < NOW() - INTERVAL '15 minutes'
            """;

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        int affected = jdbc.update(SQL);
        if (affected > 0) {
            log.info("NoShowDetectionJob: marked {} bookings as NO_SHOW", affected);

        }
    }
}


