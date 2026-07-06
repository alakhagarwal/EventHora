package com.eventHora.backend.config;


import com.eventHora.backend.Enum.Role;
import com.eventHora.backend.model.SystemUser;
import com.eventHora.backend.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds the database with initial system users on first startup.
 * Only runs when the 'dev' profile is active — safe to remove before production.
 *
 * Seed credentials:
 *   ADMIN → admin@eventhora.com  / Admin@1234
 *   STAFF → staff@eventhora.com  / Staff@1234
 */
@Slf4j
@Configuration
@Profile("dev")   // only seeds on local dev, not in production
@RequiredArgsConstructor
public class DataInitializer {

    private final SystemUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedUsers() {
        return args -> {

            // ── ADMIN ──────────────────────────────────────────────────────
            if (!userRepository.existsByEmail("admin@eventhora.com")) {
                SystemUser admin = SystemUser.builder()
                        .name("EventHora Admin")
                        .email("admin@eventhora.com")
                        .password(passwordEncoder.encode("Admin@1234"))
                        .role(Role.ADMIN)
                        .active(true)
                        .build();
                userRepository.save(admin);
                log.info("✅ Seed user created → ADMIN  | admin@eventhora.com / Admin@1234");
            } else {
                log.info("ℹ️  Seed ADMIN already exists — skipping");
            }

            // ── STAFF ──────────────────────────────────────────────────────
            if (!userRepository.existsByEmail("staff@eventhora.com")) {
                SystemUser staff = SystemUser.builder()
                        .name("EventHora Staff")
                        .email("staff@eventhora.com")
                        .password(passwordEncoder.encode("Staff@1234"))
                        .role(Role.STAFF)
                        .active(true)
                        .build();
                userRepository.save(staff);
                log.info("✅ Seed user created → STAFF  | staff@eventhora.com / Staff@1234");
            } else {
                log.info("ℹ️  Seed STAFF already exists — skipping");
            }
        };
    }
}
