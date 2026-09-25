package com.safeandsound.controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.safeandsound.dto.RegistroRequest;
import com.safeandsound.service.UsuarioService;
import com.safeandsound.dto.LoginRequest;
import com.safeandsound.dto.LoginResponse;
import com.safeandsound.dto.RefreshTokenRequest;
import com.safeandsound.model.Usuario;
import com.safeandsound.service.RefreshTokenService;
import com.safeandsound.security.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UsuarioService usuarioService, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.refreshTokenService= refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registrar(@Valid @RequestBody RegistroRequest request) {

        usuarioService.registrar(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        Usuario usuario = usuarioService.login(request);

        String accessToken = jwtService.generateAccessToken(usuario);
        String refreshToken = jwtService.generateRefreshToken(usuario);

        refreshTokenService.guardar(usuario, refreshToken);

        LoginResponse response = new LoginResponse(
                accessToken,
                refreshToken
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        String oldRefreshToken = request.getRefreshToken();

        Long usuarioId = jwtService.validateRefreshToken(oldRefreshToken);

        refreshTokenService.validarYRevocar(oldRefreshToken);

        Usuario usuario = usuarioService.buscarPorId(usuarioId);

        String accessToken = jwtService.generateAccessToken(usuario);
        String newRefreshToken = jwtService.generateRefreshToken(usuario);

        refreshTokenService.guardar(usuario, newRefreshToken);

        return ResponseEntity.ok(
                new LoginResponse(accessToken, newRefreshToken)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        jwtService.validateRefreshToken(request.getRefreshToken());

        refreshTokenService.revocar(request.getRefreshToken());

        return ResponseEntity.noContent().build();
    }
}