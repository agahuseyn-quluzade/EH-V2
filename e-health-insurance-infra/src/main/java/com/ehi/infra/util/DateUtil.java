package com.ehi.infra.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

public final class DateUtil {

    public static Instant now() {
        return Instant.now();
    }

    public static boolean isExpired(Instant expiry) {
        return expiry.isBefore(Instant.now());
    }

    public static Instant plusDays(Instant instant, long days) {
        return instant.plus(Duration.ofDays(days));
    }

    public static Instant plusYears(Instant instant, long years) {
        return instant.atZone(ZoneOffset.UTC).plusYears(years).toInstant();
    }

    private DateUtil() {
    }
}
