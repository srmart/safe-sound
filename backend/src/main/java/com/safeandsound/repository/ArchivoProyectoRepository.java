package com.safeandsound.repository;

import com.safeandsound.model.ArchivoProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArchivoProyectoRepository extends JpaRepository<ArchivoProyecto, Long> {
    
    // Spring Data crea este método automáticamente buscando por el ID del proyecto
    List<ArchivoProyecto> findByProyectoId(Long proyectoId);
}