package com.safeandsound.service;

import com.safeandsound.dto.RegistroRequest;
import com.safeandsound.model.Usuario;
import com.safeandsound.repository.UsuarioRepository;
import com.safeandsound.security.PasswordHasher;

import org.springframework.stereotype.Service;
import com.safeandsound.exception.EmailYaRegistradoException;
import java.util.Arrays;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordHasher passwordHasher) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHasher = passwordHasher;
    }

    public Usuario registrar(RegistroRequest request) {

        char[] password = request.getPassword();

        try {
            String email = request.getEmail();

            if (usuarioRepository.existsByEmail(email)) {
                throw new EmailYaRegistradoException();
            }

            String passwordHash = passwordHasher.hash(password);

            Usuario usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setPasswordHash(passwordHash);

            return usuarioRepository.save(usuario);

        } finally {
            Arrays.fill(password, '\0');
        }
    }
}