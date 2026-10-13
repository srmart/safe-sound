package com.safeandsound.controller;

import com.safeandsound.model.ArchivoProyecto;
import com.safeandsound.model.Operacion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.Usuario;
import com.safeandsound.repository.ArchivoProyectoRepository;
import com.safeandsound.repository.ProyectoRepository;
import com.safeandsound.repository.UsuarioRepository;
import com.safeandsound.service.AutorizacionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/proyectos/{proyectoId}/archivos")
@PreAuthorize("isAuthenticated()")
public class ArchivoController {

    private final AutorizacionService autorizacionService;
    private final ArchivoProyectoRepository archivoRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;

    // Ruta donde guardaremos los archivos físicos
    private final Path rootLocation = Paths.get("uploads");

    public ArchivoController(
            AutorizacionService autorizacionService,
            ArchivoProyectoRepository archivoRepository,
            ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.autorizacionService = autorizacionService;
        this.archivoRepository = archivoRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        
        // Crear carpeta uploads si no existe
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la carpeta de uploads", e);
        }
    }

    @GetMapping
    public ResponseEntity<List<ArchivoProyecto>> listarArchivos(
            @PathVariable Long proyectoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();
        autorizacionService.verificarPermiso(usuarioId, proyectoId, Operacion.VER_ARCHIVOS);

        // Buscar archivos reales en la BD
        List<ArchivoProyecto> archivos = archivoRepository.findByProyectoId(proyectoId);
        return ResponseEntity.ok(archivos);
    }

    @PostMapping
    public ResponseEntity<ArchivoProyecto> subirArchivo(
            @PathVariable Long proyectoId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();
        autorizacionService.verificarPermiso(usuarioId, proyectoId, Operacion.SUBIR_ARCHIVO);

        try {
            // 1. Validar que el proyecto existe
            Proyecto proyecto = proyectoRepository.findById(proyectoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));

            // 2. Obtener usuario
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

            // 3. Guardar archivo físico
            String filename = file.getOriginalFilename();
            // Evitar nombres duplicados o raros (simplificado)
            Path destinationFile = this.rootLocation.resolve(Paths.get(filename))
                    .normalize().toAbsolutePath();
            
            // Seguridad: verificar que no se salga de la carpeta uploads
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new RuntimeException("Ruta de archivo inválida");
            }

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            // 4. Guardar registro en BD
            ArchivoProyecto archivoProyecto = new ArchivoProyecto();
            archivoProyecto.setNombre(filename);
            archivoProyecto.setRuta(destinationFile.toString());
            archivoProyecto.setTamano(file.getSize());
            archivoProyecto.setFechaSubida(LocalDate.now());
            archivoProyecto.setProyecto(proyecto);
            archivoProyecto.setUsuario(usuario);

            ArchivoProyecto guardado = archivoRepository.save(archivoProyecto);

            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al guardar el archivo", e);
        }
    }

    @GetMapping("/{archivoId}")
    public ResponseEntity<byte[]> descargarArchivo(
            @PathVariable Long proyectoId,
            @PathVariable Long archivoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();
        autorizacionService.verificarPermiso(usuarioId, proyectoId, Operacion.DESCARGAR_ARCHIVO);

        ArchivoProyecto archivo = archivoRepository.findById(archivoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado"));

        try {
            Path file = Paths.get(archivo.getRuta());
            byte[] content = Files.readAllBytes(file);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getNombre() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al leer el archivo", e);
        }
    }

    @DeleteMapping("/{archivoId}")
    public ResponseEntity<Void> eliminarArchivo(
            @PathVariable Long proyectoId,
            @PathVariable Long archivoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();
        autorizacionService.verificarPermiso(usuarioId, proyectoId, Operacion.ELIMINAR_ARCHIVO);

        ArchivoProyecto archivo = archivoRepository.findById(archivoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado"));

        // Borrar archivo físico
        try {
            Files.deleteIfExists(Paths.get(archivo.getRuta()));
        } catch (IOException e) {
            // Log error pero continuar
        }

        // Borrar de BD
        archivoRepository.delete(archivo);

        return ResponseEntity.noContent().build();
    }
}