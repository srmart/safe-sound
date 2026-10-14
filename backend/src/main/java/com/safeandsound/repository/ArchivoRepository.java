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
}