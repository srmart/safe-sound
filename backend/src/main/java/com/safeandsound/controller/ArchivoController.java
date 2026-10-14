package com.safeandsound.controller;

import com.safeandsound.model.Archivo;
import com.safeandsound.dto.ArchivoResponseDTO;
import com.safeandsound.service.ArchivoService;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.nio.charset.StandardCharsets;



@RestController
@RequestMapping("/proyectos/{proyectoId}/archivos")
@PreAuthorize("isAuthenticated()")
public class ArchivoController {

    private final ArchivoService archivoService;

    public ArchivoController(ArchivoService archivoService) {
        this.archivoService = archivoService;
    }

    @GetMapping
    public ResponseEntity<List<ArchivoResponseDTO>> listarArchivos(
            @PathVariable Long proyectoId,
            Authentication authentication
    ) {
        Long usuarioId = (Long) authentication.getPrincipal();

        List<ArchivoResponseDTO> archivos = archivoService
                .listarArchivos(usuarioId, proyectoId)
                .stream()
                .map(ArchivoResponseDTO::desdeEntidad)
                .toList();

        return ResponseEntity.ok(archivos);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArchivoResponseDTO> subirArchivo(
            @PathVariable Long proyectoId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws IOException {
        Long usuarioId = (Long) authentication.getPrincipal();

        ArchivoResponseDTO respuesta = ArchivoResponseDTO.desdeEntidad(
                archivoService.subirArchivo(usuarioId, proyectoId, file)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }


    @GetMapping("/{archivoId}")
    public ResponseEntity<Resource> descargarArchivo(
            @PathVariable Long proyectoId,
            @PathVariable Long archivoId,
            Authentication authentication
    ) throws IOException {

        Long usuarioId = (Long) authentication.getPrincipal();

        Archivo archivo = archivoService.obtenerArchivo(
                usuarioId, proyectoId, archivoId
        );

        Resource recurso = archivoService.descargarArchivo(
                usuarioId, proyectoId, archivoId
        );

        String nombreDescarga = archivo.getNombreOriginal()
                .replace("\r", "")
                .replace("\n", "");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(recurso.contentLength())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(nombreDescarga, StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(recurso);
    }



    @DeleteMapping("/{archivoId}")
    public ResponseEntity<Void> eliminarArchivo(
            @PathVariable Long proyectoId,
            @PathVariable Long archivoId,
            Authentication authentication
    ) throws IOException {

        Long usuarioId = (Long) authentication.getPrincipal();

        archivoService.eliminarArchivo(
                usuarioId,
                proyectoId,
                archivoId
        );

        return ResponseEntity.noContent().build();
    }
}