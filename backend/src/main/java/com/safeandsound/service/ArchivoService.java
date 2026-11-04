
package com.safeandsound.service;

import com.safeandsound.model.Archivo;
import com.safeandsound.model.Operacion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.Rol;
import com.safeandsound.model.Usuario;
import com.safeandsound.repository.ArchivoRepository;
import com.safeandsound.repository.ProyectoMiembroRepository;
import com.safeandsound.repository.ProyectoRepository;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ArchivoService {

    private static final long TAMANIO_MAXIMO = 50L * 1024 * 1024;

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "audio/mpeg",
            "audio/wav",
            "audio/x-wav",
            "image/jpeg",
            "image/png",
            "application/pdf"
    );

    private static final String ARCHIVO_NO_ENCONTRADO =
            "Archivo no encontrado";

    private final ArchivoRepository archivoRepository;
    private final ArchivoStorageService archivoStorageService;
    private final AutorizacionService autorizacionService;
    private final ProyectoService proyectoService;
    private final ProyectoMiembroRepository proyectoMiembroRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioService usuarioService;

    public ArchivoService(
            ArchivoRepository archivoRepository,
            ArchivoStorageService archivoStorageService,
            AutorizacionService autorizacionService,
            ProyectoService proyectoService,
            ProyectoMiembroRepository proyectoMiembroRepository,
            ProyectoRepository proyectoRepository,
            UsuarioService usuarioService
    ) {
        this.archivoRepository = archivoRepository;
        this.archivoStorageService = archivoStorageService;
        this.autorizacionService = autorizacionService;
        this.proyectoService = proyectoService;
        this.proyectoMiembroRepository = proyectoMiembroRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioService = usuarioService;
    }

    // INC-9 / RS13:
    // Verificar que el proyecto esté activo utilizando
    // un bloqueo pesimista dentro de la transacción.
    private Proyecto obtenerProyectoActivoConBloqueo(Long proyectoId) {
        Proyecto proyecto = proyectoRepository
                .buscarPorIdConBloqueo(proyectoId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Proyecto no encontrado"
                        )
                );

        if (!proyecto.isActivo()) {
            throw new AccessDeniedException(
                    "No se permiten modificaciones en un proyecto archivado"
            );
        }

        return proyecto;
    }

    // Subir archivo.
    @Transactional
    public Archivo subirArchivo(
            Long usuarioId,
            Long proyectoId,
            MultipartFile archivo
    ) throws IOException {

        // 1. Verificar permisos por rol y estado del proyecto.
        autorizacionService.verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.SUBIR_ARCHIVO
        );

        // 2. Bloquear el proyecto y comprobar que sigue activo.
        Proyecto proyecto = obtenerProyectoActivoConBloqueo(proyectoId);

        // 3. Validar el archivo recibido.
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "El archivo está vacío"
            );
        }

        if (archivo.getSize() > TAMANIO_MAXIMO) {
            throw new IllegalArgumentException(
                    "El archivo supera los 50 MB"
            );
        }

        String tipoMime = archivo.getContentType();

        if (tipoMime == null || !TIPOS_PERMITIDOS.contains(tipoMime)) {
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido"
            );
        }

        String nombreOriginal = archivo.getOriginalFilename();

        if (nombreOriginal == null
                || nombreOriginal.isBlank()
                || nombreOriginal.length() > 255
                || nombreOriginal.contains("/")
                || nombreOriginal.contains("\\")
                || nombreOriginal.contains("\r")
                || nombreOriginal.contains("\n")) {

            throw new IllegalArgumentException(
                    "Nombre de archivo inválido"
            );
        }

        // 4. Obtener el usuario.
        Usuario usuario = usuarioService.buscarPorId(usuarioId);

        // 5. Guardar el contenido físico.
        UUID identificador = archivoStorageService.guardar(archivo);

        // 6. Registrar los metadatos en PostgreSQL.
        try {
            Archivo nuevoArchivo = new Archivo();
            nuevoArchivo.setProyecto(proyecto);
            nuevoArchivo.setSubidoPor(usuario);
            nuevoArchivo.setNombreOriginal(nombreOriginal);
            nuevoArchivo.setNombreAlmacenado(identificador);
            nuevoArchivo.setTipoMime(tipoMime);
            nuevoArchivo.setTamanio(archivo.getSize());

            return archivoRepository.saveAndFlush(nuevoArchivo);

        } catch (Exception e) {
            // Si falla PostgreSQL durante el guardado,
            // intentar eliminar el archivo físico.
            try {
                archivoStorageService.eliminar(identificador);
            } catch (IOException limpiezaError) {
                e.addSuppressed(limpiezaError);
            }

            throw e;
        }
    }

    // Listar archivos.
    @Transactional(readOnly = true)
    public List<Archivo> listarArchivos(
            Long usuarioId,
            Long proyectoId
    ) {
        autorizacionService.verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.VER_ARCHIVOS
        );

        return archivoRepository
                .findByProyectoIdAndEliminadoFalse(proyectoId);
    }

    // Obtener información de un archivo.
    @Transactional(readOnly = true)
    public Archivo obtenerArchivo(
            Long usuarioId,
            Long proyectoId,
            Long archivoId
    ) {
        autorizacionService.verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.VER_ARCHIVOS
        );

        return archivoRepository
                .findByIdAndProyectoIdAndEliminadoFalse(
                        archivoId,
                        proyectoId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                ARCHIVO_NO_ENCONTRADO
                        )
                );
    }

    // Descargar archivo.
    @Transactional(readOnly = true)
    public Resource descargarArchivo(
            Long usuarioId,
            Long proyectoId,
            Long archivoId
    ) throws IOException {

        autorizacionService.verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.DESCARGAR_ARCHIVO
        );

        Archivo archivo = archivoRepository
                .findByIdAndProyectoIdAndEliminadoFalse(
                        archivoId,
                        proyectoId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                ARCHIVO_NO_ENCONTRADO
                        )
                );

        return archivoStorageService.obtener(
                archivo.getNombreAlmacenado()
        );
    }

    // Eliminar archivo.
    @Transactional
    public void eliminarArchivo(
            Long usuarioId,
            Long proyectoId,
            Long archivoId
    ) throws IOException {

        // 1. Verificar permisos por rol y estado del proyecto.
        autorizacionService.verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.ELIMINAR_ARCHIVO
        );

        // 2. Bloquear el proyecto y comprobar que sigue activo.
        obtenerProyectoActivoConBloqueo(proyectoId);

        // 3. Buscar el archivo dentro del proyecto.
        Archivo archivo = archivoRepository
                .findByIdAndProyectoIdAndEliminadoFalse(
                        archivoId,
                        proyectoId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                ARCHIVO_NO_ENCONTRADO
                        )
                );

        // 4. Comprobar si el usuario es OWNER.
        boolean esOwner = proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(proyectoId, usuarioId)
                .map(miembro -> miembro.getRol() == Rol.OWNER)
                .orElse(false);

        // 5. Si no es OWNER, debe ser quien subió el archivo.
        if (!esOwner
                && !archivo.getSubidoPor().getId().equals(usuarioId)) {

            throw new AccessDeniedException(
                    "Solo podés eliminar los archivos que subiste"
            );
        }

        // 6. Marcar el archivo como eliminado.
        archivo.setEliminado(true);
        archivo.setEliminadoEn(Instant.now());

        // 7. Guardar el nuevo estado en PostgreSQL.
        archivoRepository.save(archivo);
    }
}
