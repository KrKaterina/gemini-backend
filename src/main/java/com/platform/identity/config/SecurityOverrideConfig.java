package com.platform.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // ΕΝΕΡΓΟΠΟΙΗΣΗ ΡΗΤΗΣ ΡΥΘΜΙΣΗΣ
public class SecurityOverrideConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. ΤΟ ΠΙΟ ΣΗΜΑΝΤΙΚΟ: Απενεργοποίηση CSRF για να δουλέψουν τα POST
                .csrf(csrf -> csrf.disable())

                // 2. Ορίζουμε ότι δεν θα έχουμε Sessions (αφού έχουμε JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. Επιτρέπουμε σε όλα τα requests να περάσουν από το Spring Security
                // Η πραγματική ασφάλεια θα γίνει από τον δικό μας Interceptor
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}