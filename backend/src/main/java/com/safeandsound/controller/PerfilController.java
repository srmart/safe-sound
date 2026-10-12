package com.safeandsound.controller;

import com.safeandsound.dto.ActualizarPerfilRequest;
import com.safeandsound.dto.ActualizarPreferenciasRequest;
import com.safeandsound.dto.PerfilResponse;
import com.safeandsound.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // <-- AGREGADO PARA RS20
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/perfil")
@PreAuthorize("isAuthenticated()") // <-- AGREGADO PARA RS20: Declara explícitamente que TODOS los endpoints requieren autenticación
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // RS20: El usuario solo puede acceder a su propio perfil porque el ID se extrae del token JWT autenticado.
    // No se acepta un parámetro usuarioId desde el cliente para evitar vulnerabilidades de acceso no autorizado.
    @GetMapping
    public ResponseEntity<PerfilResponse> obtenerPerfil(Authentication authentication) {
        return ResponseEntity.ok(
                PerfilResponse.from(usuarioService.buscarPorId(usuarioId(authentication)))
        );
    }

    // RS20: El usuario solo puede actualizar su propio perfil porque el ID se extrae del token JWT autenticado.
    @PutMapping
    public ResponseEntity<PerfilResponse> actualizarPerfil(
            @Valid @RequestBody ActualizarPerfilRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                PerfilResponse.from(usuarioService.actualizarPerfil(usuarioId(authentication), request))
        );
    }

    // RS20: El usuario solo puede actualizar sus propias preferencias porque el ID se extrae del token JWT autenticado.
    @PutMapping("/preferencias")
    public ResponseEntity<PerfilResponse> actualizarPreferencias(
            @Valid @RequestBody ActualizarPreferenciasRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                PerfilResponse.from(usuarioService.actualizarPreferencias(usuarioId(authentication), request))
        );
    }

    private Long usuarioId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}