package com.safeandsound.security;

import com.safeandsound.service.UsuarioService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UsuarioService usuarioService
    ) {
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            Claims claims = jwtService.parseToken(token);

            String type = claims.get("type", String.class);

            if (!"access".equals(type)) {
                filterChain.doFilter(request, response);
                return;
            }

            Long usuarioId = Long.valueOf(claims.getSubject());
            usuarioService.buscarPorId(usuarioId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            usuarioId,
                            null,
                            Collections.emptyList()
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (Exception ignored) {
            // El request continúa sin autenticación.
        }

        filterChain.doFilter(request, response);
    }
}