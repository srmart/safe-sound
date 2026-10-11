package com.safeandsound.security;

import com.safeandsound.service.RegistroIntentoAccesoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class RegistroIntentoAccesoFilter extends OncePerRequestFilter {

    private static final Set<String> RECURSOS_PUBLICOS = Set.of(
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/auth/logout"
    );

    private final RegistroIntentoAccesoService registroIntentoAccesoService;

    public RegistroIntentoAccesoFilter(
            RegistroIntentoAccesoService registroIntentoAccesoService
    ) {
        this.registroIntentoAccesoService = registroIntentoAccesoService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || RECURSOS_PUBLICOS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            int codigoRespuesta = response.getStatus();
            boolean autorizado = codigoRespuesta != HttpServletResponse.SC_UNAUTHORIZED
                    && codigoRespuesta != HttpServletResponse.SC_FORBIDDEN;

            registroIntentoAccesoService.registrar(
                    usuarioIdAutenticado(),
                    request.getRequestURI(),
                    request.getMethod(),
                    autorizado,
                    codigoRespuesta
            );
        }
    }

    private Long usuarioIdAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Long usuarioId)) {
            return null;
        }

        return usuarioId;
    }
}
