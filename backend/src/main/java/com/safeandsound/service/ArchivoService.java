
package com.safeandsound.service;

import com.safeandsound.model.Archivo;
import com.safeandsound.model.Operacion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.Rol;
import com.safeandsound.model.Usuario;
import com.safeandsound.repository.ArchivoRepository;
import com.safeandsound.repository.ProyectoMiembroRepository;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.List;



@Service
public class ArchivoService {

    private final ArchivoRepository archivoRepository;
    private final ArchivoStorageService archivoStorageService;
    private final AutorizacionService autorizacionService;
    private final ProyectoService proyectoService;
    private final ProyectoMiembroRepository proyectoMiembroRepository;
    private final UsuarioService usuarioService;
    private final String archivo_no_encontrado = "Archivo no encontrado";

    public ArchivoService(
            ArchivoRepository archivoRepository,
            ArchivoStorageService archivoStorageService,
            AutorizacionService autorizacionService,
            ProyectoService proyectoService,
            ProyectoMiembroRepository proyectoMiembroRepository,
            UsuarioService usuarioService
    ) {
        this.archivoRepository = archivoRepository;
        this.archivoStorageService = archivoStorageService;
        this.autorizacionService = autorizacionService;
        this.proyectoService = proyectoService;
        this.proyectoMiembroRepository = proyectoMiembroRepository;
        this.usuarioService = usuarioService;
    }



    private static final long TAMANIO_MAXIMO = 50L * 1024 * 1024; //tamaño max de archivo 50MB

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "audio/mpeg",
            "audio/wav",
            "audio/x-wav",
            "image/jpeg",
            "image/png",
            "application/pdf"
    );


    //subir archivo
    public Archivo subirArchivo(
            Long usuarioId,
            Long proyectoId,
            MultipartFile archivo
    ) throws IOException {

        // 1. Verificar autorización antes de acceder al archivo.
        autorizacionService.verificarPermiso(
                usuarioId, proyectoId, Operacion.SUBIR_ARCHIVO
        );

        // 2. Validar el archivo recibido.
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        if (archivo.getSize() > TAMANIO_MAXIMO) {
            throw new IllegalArgumentException("El archivo supera los 50 MB");
        }

        String tipoMime = archivo.getContentType();

        if (tipoMime == null || !TIPOS_PERMITIDOS.contains(tipoMime)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido");
        }

        String nombreOriginal = archivo.getOriginalFilename();

        if (nombreOriginal == null || nombreOriginal.isBlank()
                || nombreOriginal.length() > 255
                || nombreOriginal.contains("/")
                || nombreOriginal.contains("\\")
                || nombreOriginal.contains("\r")
                || nombreOriginal.contains("\n")) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }

        // 3. Obtener las entidades relacionadas.
        Proyecto proyecto = proyectoService.obtenerProyecto(proyectoId);
        Usuario usuario = usuarioService.buscarPorId(usuarioId);

        // 4. Guardar el contenido físico.
        UUID identificador = archivoStorageService.guardar(archivo);

        // 5. Registrar los metadatos en PostgreSQL.
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
            // Si falla PostgreSQL, intentamos eliminar el archivo físico.
            try {
                archivoStorageService.eliminar(identificador);
            } catch (IOException limpiezaError) {
                e.addSuppressed(limpiezaError);
            }

            throw e;
        }
    }


    //listar archivos
    public List<Archivo> listarArchivos(Long usuarioId, Long proyectoId) {

        autorizacionService.verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.VER_ARCHIVOS
        );

        return archivoRepository.findByProyectoId(proyectoId);
    }


    //ver archivo
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
                .findByIdAndProyectoId(archivoId, proyectoId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                archivo_no_encontrado
                        )
                );
    }


    //descargar archivo
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
                .findByIdAndProyectoId(archivoId, proyectoId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                archivo_no_encontrado
                        )
                );

        return archivoStorageService.obtener(
                archivo.getNombreAlmacenado()
        );
    }


    //eliminar archivo
    public void eliminarArchivo(
            Long usuarioId,
            Long proyectoId,
            Long archivoId
    ) throws IOException {

        // 1. Verificar permisos según el rol
        autorizacionService.verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.ELIMINAR_ARCHIVO
        );

        // 2. Buscar el archivo dentro del proyecto
        Archivo archivo = archivoRepository
                .findByIdAndProyectoId(archivoId, proyectoId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                archivo_no_encontrado
                        )
                );

        // 3. Comprobar si el usuario es OWNER
        boolean esOwner = proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(proyectoId, usuarioId)
                .map(miembro -> miembro.getRol() == Rol.OWNER)
                .orElse(false);

        // 4. Si no es OWNER, debe ser quien subió el archivo
        if (!esOwner && !archivo.getSubidoPor().getId().equals(usuarioId)) {
            throw new AccessDeniedException(
                    "Solo podés eliminar los archivos que subiste"
            );
        }

        // 5. Eliminar el archivo físico
        archivoStorageService.eliminar(archivo.getNombreAlmacenado());

        // 6. Eliminar los metadatos de PostgreSQL
        archivoRepository.delete(archivo);
    }

}
