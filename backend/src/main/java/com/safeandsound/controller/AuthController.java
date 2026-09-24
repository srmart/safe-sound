package com.safeandsound.controller;

import com.safeandsound.dto.RegistroRequest;
import com.safeandsound.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registrar(@Valid @RequestBody RegistroRequest request) {

        usuarioService.registrar(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}