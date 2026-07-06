package dev.lumen.config;

import dev.lumen.application.auth.TokenService;
import dev.lumen.infrastructure.security.CsrfCookieFilter;
import dev.lumen.infrastructure.security.JwtAuthenticationFilter;
import dev.lumen.infrastructure.security.StatelessSecurityContextRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cookies + CSRF, not Authorization headers — the access token is httpOnly (immune to
 * theft via XSS). Spring Security's own CORS handling replaces Phase 1's WebMvcConfigurer
 * approach: the security filter chain must see and allow preflight requests itself.
 *
 * <p>register/login/refresh/logout don't require the access-token cookie to be valid:
 * logout in particular must still work when the access token already expired but the
 * refresh token has not — its whole job is to revoke that refresh token.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
        "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout"
    };

    private final String[] allowedOrigins;

    public SecurityConfig(@Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokenService) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // The default XorCsrfTokenRequestAttributeHandler expects the
                        // submitted token to be XOR-masked, but a cookie-based SPA only
                        // ever has the plain value to send back — Spring Security's own
                        // docs call this out as needing the plain handler instead.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(PUBLIC_AUTH_ENDPOINTS))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Spring Security's own stateless repository always reports "no context
                // found," which makes SessionManagementFilter treat every JWT-authenticated
                // request as a brand new login and re-run CsrfAuthenticationStrategy —
                // rotating (and clearing) the CSRF cookie after every single request. This
                // repository reports "already handled" instead, since JwtAuthenticationFilter
                // rebuilds the context itself on every request anyway.
                .securityContext(
                        securityContext -> securityContext.securityContextRepository(
                                new StatelessSecurityContextRepository()))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/info")
                        .permitAll()
                        .requestMatchers(PUBLIC_AUTH_ENDPOINTS)
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(tokenService), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
