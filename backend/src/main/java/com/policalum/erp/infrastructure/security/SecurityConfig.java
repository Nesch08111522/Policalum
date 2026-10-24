// src/main/java/com/policalum/erp/infraestructure/security/SecurityConfig.java
package com.policalum.erp.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // activa @PreAuthorize en los controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ✅ Permitir login sin autenticación
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // ✅ Documentación OpenAPI / Swagger UI (deshabilitable con springdoc.*.enabled=false)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()

                        // ✅ Marca (nombre comercial + logo) para la pantalla de login
                        .requestMatchers(HttpMethod.GET, "/api/v1/configuracion/publica", "/api/v1/configuracion/logo").permitAll()

                        // QR de kiosco. La marcación autenticada y el listado de empleados exigen sesión.
                        .requestMatchers(HttpMethod.GET, "/api/v1/rrhh/asistencias/qr-publico").permitAll()

                        .requestMatchers("/api/v1/rrhh/asistencias/configurar-totp/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/rrhh/asistencias/qr-config/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/rrhh/asistencias/empleados").hasRole("ADMIN")

                        // ✅ El resto requiere autenticación
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();


    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        //  Agregamos la IP local (192.168.18.9) y patrones para cualquier IP local
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:4200",
                "http://127.0.0.1:4200",
                "http://10.189.117.157:4200",
                "http://10.189.117.157:8080",
                "https://192.168.18.9:4200",
                "http://192.168.18.9:4200", // 👈 Tu IP exacta
                "http://192.168.*:*",       // 👈 Cualquier dispositivo en tu red local
                "http://localhost:8080",
                "https://*.ngrok-free.dev",  // 👈 PERMITIR TODOS LOS NGROK
                "https://quake-spender-taunt.ngrok-free.dev" ,
                "https://jujitsu-freeway-crying.ngrok-free.dev",  // Tu frontend
                    "https://otra-url.ngrok-free.dev" // 👈 O EL ESPECÍFICO
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}