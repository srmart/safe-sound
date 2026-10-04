package com.safeandsound.security;

import com.safeandsound.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import javax.crypto.SecretKey;
import java.util.Date;
import java.time.Duration;

@Service
public class JwtService {

    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(7);

    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // genera un access token que identifica al usuario autenticado
    public String generateAccessToken(Usuario usuario) {

        Date now = new Date();
        Date expiration = new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION.toMillis());

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    // genera un refresh token utilizado para renovar el access token
    public String generateRefreshToken(Usuario usuario) {

        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + REFRESH_TOKEN_EXPIRATION.toMillis()
        );

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long validateRefreshToken(String token) {

        Claims claims = parseToken(token);

        String type = claims.get("type", String.class);

        if (!"refresh".equals(type)) {
            throw new IllegalArgumentException("El token no es un refresh token");
        }

        return Long.valueOf(claims.getSubject());
    }
}