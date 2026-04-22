package com.platform.identity.config;

import com.platform.identity.domain.UserAccount;
import com.platform.identity.domain.UserStatus;
import com.platform.identity.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserSeeder implements CommandLineRunner {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(UserAccount.builder()
                    .userId("cust-001")
                    .username("customer@test.com")
                    .passwordHash(passwordEncoder.encode("password"))
                    .roles(Set.of("ROLE_CUSTOMER"))
                    .status(UserStatus.ACTIVE)
                    .externalReference("POL-123")
                    .build());

            userRepository.save(UserAccount.builder()
                    .userId("agent-001")
                    .username("agent@test.com")
                    .passwordHash(passwordEncoder.encode("password"))
                    .roles(Set.of("ROLE_AGENT"))
                    .status(UserStatus.ACTIVE)
                    .externalReference("EMP-001")
                    .build());
            System.out.println(">>> SEED DATA CREATED: Users created successfully.");
        } else {
            System.out.println(">>> SEED SKIP: Database already contains users. Count: " + userRepository.count());
        }
    }
}