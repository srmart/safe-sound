package com.safeandsound.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.safeandsound.security.JwtAuthenticationFilter;
import com.safeandsound.security.RegistroIntentoAccesoFilter;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RegistroIntentoAccesoFilter registroIntentoAccesoFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RegistroIntentoAccesoFilter registroIntentoAccesoFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.registroIntentoAccesoFilter = registroIntentoAccesoFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/logout"
                        ).permitAll()

                        // RS19: Reautenticación solo para usuarios autenticados
                        .requestMatchers(HttpMethod.POST, "/auth/reautenticar").authenticated()

                                // RS30: Cada funcionalidad existente debe declararse explícitamente.
                                // Los endpoints nuevos quedan denegados por defecto.

                                // Proyectos
                                .requestMatchers(HttpMethod.POST, "/proyectos").authenticated()
                                .requestMatchers(HttpMethod.GET, "/proyectos").authenticated()
                                .requestMatchers(HttpMethod.GET, "/proyectos/{proyectoId}").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/proyectos/{proyectoId}").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/proyectos/{proyectoId}").authenticated()



                                // Gestión de miembros
                                .requestMatchers(HttpMethod.GET, "/proyectos/{proyectoId}/miembros").authenticated()

                                .requestMatchers(HttpMethod.POST, "/proyectos/{proyectoId}/miembros/{usuarioId}").authenticated()

                                .requestMatchers(HttpMethod.PUT, "/proyectos/{proyectoId}/miembros/{usuarioId}/rol").authenticated()



                                // Auditoría administrativa
                                .requestMatchers(HttpMethod.GET, "/proyectos/{proyectoId}/auditoria").authenticated()

                                // Gestión de archivos
                                .requestMatchers(HttpMethod.GET, "/proyectos/{proyectoId}/archivos").authenticated()
                                .requestMatchers(HttpMethod.POST, "/proyectos/{proyectoId}/archivos").authenticated()
                                .requestMatchers(HttpMethod.GET, "/proyectos/{proyectoId}/archivos/{archivoId}").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/proyectos/{proyectoId}/archivos/{archivoId}").authenticated()

                                // Solicitudes de restauración JIT
                                .requestMatchers(HttpMethod.POST, "/proyectos/{proyectoId}/archivos/{archivoId}/solicitudes-restauracion").authenticated()
                                .requestMatchers(HttpMethod.GET, "/proyectos/{proyectoId}/solicitudes-restauracion").authenticated()
                                .requestMatchers(HttpMethod.POST, "/proyectos/{proyectoId}/solicitudes-restauracion/{solicitudId}/aprobar").authenticated()
                                .requestMatchers(HttpMethod.POST, "/proyectos/{proyectoId}/solicitudes-restauracion/{solicitudId}/rechazar").authenticated()
                                .requestMatchers(HttpMethod.POST, "/proyectos/{proyectoId}/solicitudes-restauracion/{solicitudId}/restaurar").authenticated()

                                // Perfil del usuario autenticado
                                .requestMatchers(HttpMethod.GET, "/perfil").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/perfil").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/perfil/preferencias").authenticated()

                                // RS30: Cualquier endpoint no declarado queda denegado.
                                .anyRequest().denyAll()

                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(
                        registroIntentoAccesoFilter,
                        JwtAuthenticationFilter.class
                );

        return http.build();
    }
}