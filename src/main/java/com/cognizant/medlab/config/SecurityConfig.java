package com.cognizant.medlab.config;

import com.cognizant.medlab.domain.identity.Role;
import com.cognizant.medlab.security.CustomUserDetailsService;
import com.cognizant.medlab.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;

/**
 * Spring Security configuration.
 *
 * Two filter chains:
 *  1. @Order(1) REST API (/api/**) — stateless JWT, CSRF disabled.
 *  2. @Order(2) UI  (/**        ) — session form-login, CSRF enabled.
 *
 * Both chains explicitly wire DaoAuthenticationProvider → CustomUserDetailsService
 * → BCrypt so that form-login password verification works correctly.
 *
 * H2 console (/h2-console/**) is permitted in the UI chain and frame options
 * are relaxed for same-origin so the H2 iframe renders.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter            jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    // ── Shared beans ─────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Chain 1 : REST API (/api/**) ─────────────────────────────

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasAuthority(Role.ADMIN)
                .requestMatchers("/api/admin/**").hasAuthority(Role.ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/users")
                    .hasAuthority(Role.ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/users/**")
                    .hasAuthority(Role.ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/tests/**")
                    .hasAnyAuthority(Role.ADMIN, Role.LAB_MANAGER)
                .requestMatchers(HttpMethod.POST, "/api/panels/**")
                    .hasAnyAuthority(Role.ADMIN, Role.LAB_MANAGER)
                .anyRequest().authenticated()
            );

        return http.build();
    }

    // ── Chain 2 : Thymeleaf UI (/**) ─────────────────────────────

    @Bean
    @Order(2)
    public SecurityFilterChain uiSecurityChain(HttpSecurity http) throws Exception {
        http
            // Wire the same provider so form-login uses CustomUserDetailsService + BCrypt
            .authenticationProvider(authenticationProvider())
            // Allow H2 console iframe (same-origin only)
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            // Disable CSRF for H2 console path only
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            )
            .authorizeHttpRequests(auth -> auth
                // Public — login, static assets
                .requestMatchers(
                    "/login", "/logout", "/error",
                    "/css/**", "/js/**", "/images/**",
                    "/webjars/**", "/favicon.ico").permitAll()
                // Swagger
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html").permitAll()
                // H2 console (dev only — disabled in prod via h2.console.enabled=false)
                .requestMatchers("/h2-console/**").permitAll()
                // Actuator health
                .requestMatchers("/actuator/health").permitAll()
                // Admin UI
                .requestMatchers("/ui/admin/**").hasAuthority(Role.ADMIN)
                // Everything else — must be authenticated
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/ui/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );

        return http.build();
    }
}
