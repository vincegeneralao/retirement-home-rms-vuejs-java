package com.rms.config;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Application settings bound from the {@code rms.*} configuration tree. */
@Getter
@Setter
@ConfigurationProperties(prefix = "rms")
public class AppProperties {

    /** Signing settings for issued JWTs. */
    private Jwt jwt = new Jwt();

    /** Browser origins allowed to call the API. */
    private Cors cors = new Cors();

    /** Site timezone used to resolve "today" and day boundaries. */
    private String siteTimezone = "Australia/Brisbane";

    public ZoneId siteZone() {
        return ZoneId.of(siteTimezone);
    }

    @Getter
    @Setter
    public static class Jwt {
        /** HS256 signing secret; must be at least 32 bytes. */
        private String secret = "change-me-in-production-please-32-bytes-min";
        /** Lifetime of an issued token. */
        private Duration ttl = Duration.ofHours(8);
        /** Issuer claim written into tokens. */
        private String issuer = "retirement-home-rms";
    }

    @Getter
    @Setter
    public static class Cors {
        /** Allowed origins; the dev Vite server is http://localhost:5175. */
        private List<String> allowedOrigins = List.of("http://localhost:5175");
    }
}
