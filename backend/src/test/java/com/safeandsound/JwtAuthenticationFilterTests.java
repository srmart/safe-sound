package com.safeandsound;

import com.safeandsound.model.Usuario;
import com.safeandsound.security.JwtAuthenticationFilter;
import com.safeandsound.security.JwtService;
import com.safeandsound.service.UsuarioService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTests {

    // limpia la autenticación antes de cada test para evitar que el estado de un test afecte al siguiente.
    @BeforeEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    //comprueba que un access token válido autentica al usuario.
    @Test
    void accessTokenValidoDebeAutenticarUsuario() throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        FilterChain filterChain = mock(FilterChain.class);
        Claims claims = mock(Claims.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, usuarioService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(
                "Authorization",
                "Bearer access-token-de-prueba"
        );

        when(jwtService.parseToken("access-token-de-prueba"))
                .thenReturn(claims);

        when(claims.get("type", String.class))
                .thenReturn("access");

        when(claims.getSubject())
                .thenReturn("1");

        when(usuarioService.buscarPorId(1L))
                .thenReturn(new Usuario());

        filter.doFilter(request, response, filterChain);

        assertNotNull(
                org.springframework.security.core.context
                        .SecurityContextHolder.getContext()
                        .getAuthentication()
        );
    }


    // comprueba que un refresh token no puede utilizarse para autenticarse en un endpoint protegido.
    @Test
    void refreshTokenNoDebeAutenticarUsuario() throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        FilterChain filterChain = mock(FilterChain.class);
        Claims claims = mock(Claims.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, usuarioService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(
                "Authorization",
                "Bearer refresh-token-de-prueba"
        );

        when(jwtService.parseToken("refresh-token-de-prueba"))
                .thenReturn(claims);

        when(claims.get("type", String.class))
                .thenReturn("refresh");

        filter.doFilter(request, response, filterChain);

        assertNull(
                org.springframework.security.core.context
                        .SecurityContextHolder.getContext()
                        .getAuthentication()
        );
    }

    //comprueba que un token inválido no autentica al usuario y que el filtro permite que Spring Security gestione el rechazo.
    @Test
    void tokenInvalidoNoDebeAutenticarUsuario() throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        FilterChain filterChain = mock(FilterChain.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, usuarioService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(
                "Authorization",
                "Bearer token-invalido"
        );

        when(jwtService.parseToken("token-invalido"))
                .thenThrow(new IllegalArgumentException("Token inválido"));

        filter.doFilter(request, response, filterChain);

        assertNull(
                org.springframework.security.core.context
                        .SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(filterChain).doFilter(request, response);
    }
}