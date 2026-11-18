package com.sigesmed.api.config;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigesmed.api.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper,
            CookieCsrfTokenRepository csrfRepository, CorsConfigurationSource corsConfigurationSource,
            Clock applicationClock)
            throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .authorizeHttpRequests(authorize -> authorize
                        // Catálogo necesario para reservar sin iniciar sesión. Las escrituras siguen protegidas abajo.
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/especialidades",
                                "/api/v1/especialidades/*/medicos",
                                "/api/v1/medicos/*/horarios",
                                "/api/v1/servicios-complementarios",
                                "/api/v1/servicios-complementarios/*/horarios").permitAll()
                        .requestMatchers("/api/v1/health", "/api/v1/csrf",
                                "/api/v1/auth/registro", "/api/v1/auth/login",
                                "/api/v1/auth/recuperar", "/api/v1/auth/restablecer").permitAll()
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/cuenta/**").hasAnyRole("PACIENTE", "RECEPCIONISTA", "ADMINISTRADOR", "DOCTOR")
                        .requestMatchers("/api/v1/pacientes/**", "/api/v1/citas/**").hasRole("PACIENTE")
                        .requestMatchers("/api/v1/recepcionista/**").hasRole("RECEPCIONISTA")
                        .requestMatchers("/api/v1/administrador/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/v1/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/api/v1/especialidades/**", "/api/v1/medicos/**")
                                .hasAnyRole("PACIENTE", "RECEPCIONISTA", "ADMINISTRADOR", "DOCTOR")
                        .anyRequest().denyAll())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> writeError(
                                mapper, response, applicationClock, HttpStatus.UNAUTHORIZED, "Autenticación requerida",
                                request.getRequestURI()))
                        .accessDeniedHandler((request, response, exception) -> writeError(
                                mapper, response, applicationClock, HttpStatus.FORBIDDEN, "Acceso denegado",
                                request.getRequestURI())))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .build();
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setCookiePath("/");
        return repository;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${sigesmed.frontend-origin:http://localhost:4200}") String frontendOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(java.util.List.of(frontendOrigin));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    Clock applicationClock() {
        return Clock.system(ZoneId.of("America/Lima"));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService noAccountsUntilDataExists() {
        return username -> {
            throw new UsernameNotFoundException("La autenticación se realiza mediante AuthService");
        };
    }

    private static void writeError(ObjectMapper mapper, HttpServletResponse response, Clock clock,
            HttpStatus status, String message, String path) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(),
                new ApiError(OffsetDateTime.now(clock), status.value(),
                        status.getReasonPhrase(), message, path));
    }
}
