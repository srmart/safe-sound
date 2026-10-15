package com.safeandsound.repository;

import com.safeandsound.model.Archivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArchivoRepository extends JpaRepository<Archivo, Long> {

    // Listar los archivos de un proyecto
    List<Archivo> findByProyectoId(Long proyectoId);

    // Buscar un archivo verificando su proyecto
    Optional<Archivo> findByIdAndProyectoId(
            Long archivoId,
            Long proyectoId
    );

    // Listar únicamente archivos activos
    List<Archivo> findByProyectoIdAndEliminadoFalse(Long proyectoId);

    // Buscar un archivo activo dentro de un proyecto
    Optional<Archivo> findByIdAndProyectoIdAndEliminadoFalse(
            Long archivoId,
            Long proyectoId
    );

    // Buscar un archivo eliminado para su restauración
    Optional<Archivo> findByIdAndProyectoIdAndEliminadoTrue(
            Long archivoId,
            Long proyectoId
    );
}