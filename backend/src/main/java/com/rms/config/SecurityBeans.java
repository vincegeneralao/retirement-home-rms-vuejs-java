package com.rms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Only the BCrypt password hasher from spring-security-crypto is used (no security filter chain). */
@Configuration
public class SecurityBeans {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BlankTolerantBCryptPasswordEncoder();
    }

    /**
     * Plain BCrypt with one deliberate exception: an empty secret is verifiable.
     *
     * <p>{@link BCryptPasswordEncoder#matches} refuses to compare an empty raw secret, which would
     * lock a worker out of the blank password an administrator hands out on a reset. Everything
     * except that empty-string short circuit is delegated untouched, so strength rules still belong
     * to the request DTOs that create or change passwords.
     */
    static final class BlankTolerantBCryptPasswordEncoder implements PasswordEncoder {

        private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

        @Override
        public String encode(CharSequence rawPassword) {
            return delegate.encode(rawPassword);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            if (rawPassword == null || encodedPassword == null || encodedPassword.isEmpty()) {
                return false;
            }
            if (rawPassword.length() > 0) {
                return delegate.matches(rawPassword, encodedPassword);
            }
            try {
                return BCrypt.checkpw("", encodedPassword);
            } catch (IllegalArgumentException | IllegalStateException malformed) {
                return false;
            }
        }
    }
}
