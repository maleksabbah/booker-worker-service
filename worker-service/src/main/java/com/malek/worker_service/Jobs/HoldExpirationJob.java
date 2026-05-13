package com.malek.worker_service.Jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * Cancels public bookings whose hold deadline passed before quorum was met.
 * Runs every 5 seconds.
 *
 * State transition: HELD -> CANCELLED (when hold_expires_at < now)
 */
@Component
@RequiredArgsConstructor
@Slf4j

public class HoldExpirationJob {
    private final JdbcTemplate jdbc;

    private static final String SQL = """
            UPDATE bookings
            SET state = 'CANCELLED',
                updated_at = NOW()
            WHERE state = 'HELD'
              AND hold_expires_at IS NOT NULL
              AND hold_expires_at < NOW()
            
            """;


    @Scheduled(fixedDelay = 5_000) // every 5s, measured from end of previous run
    public void run() {
        int affected = jdbc.update(SQL);
        if (affected > 0) {
            log.info("HoldExpirationJob: cancelled {} expired HELD bookings", affected);
        }
    }
    

}
