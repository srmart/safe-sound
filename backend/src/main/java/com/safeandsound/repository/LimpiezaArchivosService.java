
package com.safeandsound.service;

import com.safeandsound.model.Archivo;
import com.safeandsound.repository.ArchivoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LimpiezaArchivosService {

    private static final Logger log =
            LoggerFactory.getLogger(LimpiezaArchivosService.class);

    private final ArchivoRepository archivoRepository;
    private final ArchivoStorageService archivoStorageService;
    private final long tiempoGraciaHoras;

    public LimpiezaArchivosService(
            ArchivoRepository archivoRepository,
            ArchivoStorageService archivoStorageService,
            @Value("${app.archivos.tiempo-gracia-horas:3}")
            long tiempoGraciaHoras
    ) {
        this.archivoRepository = archivoRepository;
        this.archivoStorageService = archivoStorageService;
        this.tiempoGraciaHoras = tiempoGraciaHoras;
    }

    // Ejecutar cada 5 minutos.
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void eliminarArchivosVencidos() {

        Instant limite = Instant.now().minus(
                tiempoGraciaHoras, ChronoUnit.HOURS
        );

        List<Archivo> vencidos =
                archivoRepository
                        .findByEliminadoTrueAndEliminadoEnLessThanEqual(
                                limite
                        );

        for (Archivo archivo : vencidos) {
            try {
                // Primero eliminar el contenido físico.
                archivoStorageService.eliminar(
                        archivo.getNombreAlmacenado()
                );

                // Después eliminar el registro de la base.
                archivoRepository.delete(archivo);

                log.info(
                        "Archivo eliminado definitivamente. ID: {}",
                        archivo.getId()
                );

            } catch (IOException e) {
                // Si falla el borrado físico, conservar el registro
                // para poder reintentar en la próxima ejecución.
                log.error(
                        "No se pudo eliminar el archivo físico. ID: {}",
                        archivo.getId(),
                        e
                );
            }
        }
    }
}
