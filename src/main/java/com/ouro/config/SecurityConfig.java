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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",
            "http://localhost:3001",
            frontendUrl
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
