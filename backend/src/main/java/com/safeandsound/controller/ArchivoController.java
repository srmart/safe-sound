package com.safeandsound.controller;

import com.safeandsound.model.Operacion;
import com.safeandsound.service.AutorizacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/proyectos/{proyectoId}/archivos")
@PreAuthorize("isAuthenticated()")
public class ArchivoController {

    private final AutorizacionService autorizacionService;

    public ArchivoController(AutorizacionService autorizacionService) {
        this.autorizacionService = autorizacionService;
    }

    @GetMapping
    public ResponseEntity<String> listarArchivos(
            @PathVariable Long proyectoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        autorizacionService.verificarPermiso(usuarioId, proyectoId, Operacion.VER_ARCHIVOS);

        return ResponseEntity.ok("Lista de archivos (pendiente de implementación del servicio)");
    }

    @PostMapping
    public ResponseEntity<String> subirArchivo(
            @PathVariable Long proyectoId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        autorizacionService.verificarPermiso(usuarioId, proyectoId, Operacion.SUBIR_ARCHIVO);

        return ResponseEntity.status(201).body("Archivo subido (pendiente de implementación del servicio)");
    }

    @GetMapping("/{archivoId}")
    public ResponseEntity<String> descargarArchivo(
            @PathVariable Long proyectoId,
            @PathVariable Long archivoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        autorizacionService.verificarPermiso(usuarioId, proyectoId, Operacion.DESCARGAR_ARCHIVO);

        return ResponseEntity.ok("Descarga de archivo (pendiente de implementación del servicio)");
    }

    @DeleteMapping("/{archivoId}")
    public ResponseEntity<Void> eliminarArchivo(
            @PathVariable Long proyectoId,
            @PathVariable Long archivoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        autorizacionService.verificarPermiso(usuarioId, proyectoId, Operacion.ELIMINAR_ARCHIVO);

        return ResponseEntity.noContent().build();
    }
}