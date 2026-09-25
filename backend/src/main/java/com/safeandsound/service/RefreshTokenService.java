package com.safeandsound.service;

import com.safeandsound.exception.RefreshTokenInvalidoException;
import com.safeandsound.model.RefreshToken;
import com.safeandsound.model.Usuario;
import com.safeandsound.repository.RefreshTokenRepository;
import com.safeandsound.security.TokenHasher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;

@Service
public class RefreshTokenService {

    private static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(7);

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            TokenHasher tokenHasher
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
    }

    public void guardar(Usuario usuario, String token) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUsuario(usuario);
        refreshToken.setTokenHash(tokenHasher.hash(token));
        refreshToken.setExpiresAt(
                Instant.now().plus(REFRESH_TOKEN_EXPIRATION)
        );

        refreshTokenRepository.save(refreshToken);
    }

    public void validarYRevocar(String token) {
        String tokenHash = tokenHasher.hash(token);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(RefreshTokenInvalidoException::new
                );

        if (refreshToken.isRevoked()) {
            throw new RefreshTokenInvalidoException();
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenInvalidoException();
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    //metodo para cerrar sesion
    public void revocar(String token) {
        String tokenHash = tokenHasher.hash(token);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(RefreshTokenInvalidoException::new);

        if (refreshToken.isRevoked()) {
            throw new RefreshTokenInvalidoException();
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}