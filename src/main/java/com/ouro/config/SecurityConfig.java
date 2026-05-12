package com.ouro.config;

import com.ouro.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Autowired
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // ── Usuarios: endpoints públicos ──────────────────
                .requestMatchers(HttpMethod.POST,
                    "/api/users/register",
                    "/api/users/login",
                    "/api/users/forgot-password",
                    "/api/users/reset-password",
                    "/api/users/resend-verification"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/verify-email").permitAll()

                // ── Terapeutas: lectura pública ──────────────────
                .requestMatchers(HttpMethod.GET,
                    "/api/therapists",
                    "/api/therapists/**"
                ).permitAll()

                // ── Calendario / slots: lectura pública ───────────
                .requestMatchers(HttpMethod.GET,
                    "/api/appointments/available-days",
                    "/api/appointments/available-slots"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/availability/therapist/**").permitAll()

                // ── Calificaciones: listado público (no /estado) ──
                .requestMatchers(HttpMethod.GET, "/api/ratings/therapist/*").permitAll()

                // ── Webhooks Mercado Pago: siempre públicos ───────
                .requestMatchers("/api/payments/webhook", "/api/payments/webhook/**").permitAll()

                // ── Todo lo demás requiere autenticación ──────────
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
