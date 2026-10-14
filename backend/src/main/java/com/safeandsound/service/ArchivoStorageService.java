
package com.safeandsound.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class ArchivoStorageService {

    private final Path directorio;

    public ArchivoStorageService(
            @Value("${archivos.storage.path}") String ruta
    ) throws IOException {

        directorio = Path.of(ruta).toAbsolutePath().normalize();

        Files.createDirectories(directorio);

        // Evitamos trabajar sobre una ruta simbólica.
        if (!directorio.toRealPath().equals(directorio)) {
            throw new IOException("Directorio de almacenamiento no permitido");
        }
    }

    // Guarda el contenido físico con un identificador generado.
    public UUID guardar(MultipartFile archivo) throws IOException {

        UUID identificador = UUID.randomUUID();
        Path destino = resolverRuta(identificador);

        try (var entrada = archivo.getInputStream()) {
            Files.copy(entrada, destino);
        }

        return identificador;
    }

    // Recupera el archivo para visualizarlo o descargarlo.
    public Resource obtener(UUID identificador) throws IOException {

        Path ruta = resolverRuta(identificador);

        if (!Files.isRegularFile(ruta) || Files.isSymbolicLink(ruta)) {
            throw new IOException("Archivo físico no encontrado");
        }

        return new UrlResource(ruta.toUri());
    }

    // Elimina el archivo físico.
    public void eliminar(UUID identificador) throws IOException {

        Path ruta = resolverRuta(identificador);

        if (Files.isSymbolicLink(ruta)) {
            throw new IOException("Ruta de archivo no permitida");
        }

        Files.deleteIfExists(ruta);
    }

    private Path resolverRuta(UUID identificador) {

        if (identificador == null) {
            throw new IllegalArgumentException("Identificador inválido");
        }

        Path ruta = directorio.resolve(identificador.toString()).normalize();

        if (!ruta.getParent().equals(directorio)) {
            throw new SecurityException("Ruta fuera del almacenamiento");
        }

        return ruta;
    }
}
