package com.safeandsound.repository;

import com.safeandsound.model.ArchivoProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArchivoProyectoRepository
        extends JpaRepository<ArchivoProyecto, Long> {
    
    // Spring Data crea este método automáticamente buscando por el ID del proyecto
    List<ArchivoProyecto> findByProyectoIdAndEliminadoFalse(Long proyectoId);

    Optional<ArchivoProyecto> findByIdAndProyectoIdAndEliminadoFalse(
            Long id,
            Long proyectoId
    );

    Optional<ArchivoProyecto> findByIdAndProyectoIdAndEliminadoTrue(
            Long id,
            Long proyectoId
    );


}