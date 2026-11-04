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
import com.safeandsound.model.ControlIntentosAutenticacion.TipoIntento;
import java.util.Locale;

@Service
public class UsuarioService {

    private static final Pattern FOTO_PERFIL_VALIDA = Pattern.compile(
            "^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/]+={0,2}$"
    );

    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;
    private final ProteccionFuerzaBrutaService proteccionFuerzaBrutaService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordHasher passwordHasher,
            ProteccionFuerzaBrutaService proteccionFuerzaBrutaService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHasher = passwordHasher;
        this.proteccionFuerzaBrutaService = proteccionFuerzaBrutaService;
    }

    public Usuario registrar(RegistroRequest request) {
        char[] password = request.getPassword();
        try {
            String email = request.getEmail()
                    .trim()
                    .toLowerCase(Locale.ROOT);
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
        } finally {
            // limpiar el array de la contraseña
            Arrays.fill(password, '\0');
        }
    }

    public Usuario login(LoginRequest request) {
        char[] password = request.getPassword();

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        try {
            ProteccionFuerzaBrutaService.Resultado<Usuario> resultado =
                    proteccionFuerzaBrutaService.ejecutar(
                            email,
                            TipoIntento.LOGIN,
                            () -> {
                                Usuario usuario = usuarioRepository
                                        .findByEmail(email)
                                        .orElseThrow(
                                                CredencialesInvalidasException::new
                                        );

                                if (!passwordHasher.matches(
                                        password,
                                        usuario.getPasswordHash()
                                )) {
                                    throw new CredencialesInvalidasException();
                                }

                                return usuario;
                            }
                    );

            if (!resultado.isExitoso()) {
                throw new CredencialesInvalidasException();
            }

            return resultado.getValor();

        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
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

    /**
     * Verifica la contraseña de un usuario para operaciones de alto riesgo (Reautenticación).
     * Mantiene las prácticas de seguridad de manejo de memoria con char[].
     */
    public void verificarPassword(
            Long usuarioId,
            char[] passwordIngresada
    ) {
        try {
            ProteccionFuerzaBrutaService.Resultado<Boolean> resultado =
                    proteccionFuerzaBrutaService.ejecutar(
                            String.valueOf(usuarioId),
                            TipoIntento.REAUTENTICACION,
                            () -> {
                                Usuario usuario = buscarPorId(usuarioId);

                                if (!passwordHasher.matches(
                                        passwordIngresada,
                                        usuario.getPasswordHash()
                                )) {
                                    throw new CredencialesInvalidasException();
                                }

                                return true;
                            }
                    );

            if (!resultado.isExitoso()) {
                throw new CredencialesInvalidasException();
            }

        } finally {
            if (passwordIngresada != null) {
                Arrays.fill(passwordIngresada, '\0');
            }
        }
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