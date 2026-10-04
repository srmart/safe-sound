package com.safeandsound.service;

import com.safeandsound.dto.RegistroRequest;
import com.safeandsound.dto.LoginRequest;
import com.safeandsound.model.Usuario;
import com.safeandsound.repository.UsuarioRepository;
import com.safeandsound.security.PasswordHasher;

import org.springframework.stereotype.Service;
import com.safeandsound.exception.EmailYaRegistradoException;
import com.safeandsound.exception.UsernameYaRegistradoException;
import com.safeandsound.exception.CredencialesInvalidasException;
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
            String username = request.getUsername();

            if (usuarioRepository.existsByEmail(email)) {
                throw new EmailYaRegistradoException();
            }

            if (usuarioRepository.existsByUsername(username)) {
                throw new UsernameYaRegistradoException();
            }

            String passwordHash = passwordHasher.hash(password);

            Usuario usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setUsername(username);
            usuario.setPasswordHash(passwordHash);

            return usuarioRepository.save(usuario);

            //limpiar el array de la contraseña
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public Usuario login(LoginRequest request) {

        char[] password = request.getPassword();

        try {
            //buscar por email
            Usuario usuario = usuarioRepository.findByEmail(request.getEmail()).orElseThrow(CredencialesInvalidasException::new);

            //match contraseña
            if (!passwordHasher.matches(password, usuario.getPasswordHash())) {
                throw new CredencialesInvalidasException();
            }

            return usuario;

        } finally {
            // limpia la contraseña ingresada una vez finalizada la autenticación.
            Arrays.fill(password, '\0');
        }
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(CredencialesInvalidasException::new);
    }
}