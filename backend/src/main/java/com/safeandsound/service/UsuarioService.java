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
import com.safeandsound.dto.ActualizarPerfilRequest;
import com.safeandsound.dto.ActualizarPreferenciasRequest;
import java.util.Arrays;
import java.util.regex.Pattern;




@Service
public class UsuarioService {

    private static final Pattern FOTO_PERFIL_VALIDA = Pattern.compile(
            "^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/]+={0,2}$"
    );

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

    public Usuario actualizarPerfil(Long usuarioId, ActualizarPerfilRequest request) {
        Usuario usuario = buscarPorId(usuarioId);
        String username = request.getUsername().trim();

        if (!usuario.getUsername().equals(username) && usuarioRepository.existsByUsername(username)) {
            throw new UsernameYaRegistradoException();
        }

        String fotoPerfil = normalizarFotoPerfil(request.getFotoPerfil());
        usuario.setUsername(username);
        usuario.setFotoPerfil(fotoPerfil);

        return usuarioRepository.save(usuario);
    }

    public Usuario actualizarPreferencias(Long usuarioId, ActualizarPreferenciasRequest request) {
        Usuario usuario = buscarPorId(usuarioId);
        usuario.setNotificacionesHabilitadas(request.getNotificacionesHabilitadas());
        usuario.setModoOscuro(request.getModoOscuro());

        return usuarioRepository.save(usuario);
    }

    private String normalizarFotoPerfil(String fotoPerfil) {
        if (fotoPerfil == null || fotoPerfil.isBlank()) {
            return null;
        }

        if (!FOTO_PERFIL_VALIDA.matcher(fotoPerfil).matches()) {
            throw new IllegalArgumentException("La foto de perfil debe ser una imagen PNG, JPEG o WEBP válida");
        }

        return fotoPerfil;
    }
}
