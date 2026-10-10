package com.safeandsound.repository;

import com.safeandsound.model.ProyectoMiembro;
import com.safeandsound.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ProyectoMiembroRepository extends JpaRepository<ProyectoMiembro, Long> {

    List<ProyectoMiembro> findByUsuarioId(Long usuarioId);

    Optional<ProyectoMiembro> findByProyectoIdAndUsuarioId(
            Long proyectoId,
            Long usuarioId
    );

    boolean existsByProyectoIdAndUsuarioId(
            Long proyectoId,
            Long usuarioId
    );

    boolean existsByProyectoIdAndUsuarioIdAndRol(
            Long proyectoId,
            Long usuarioId,
            Rol rol
    );

    // R5 - Listado de proyectos con búsqueda y filtros
    @Query("""
    SELECT pm
    FROM ProyectoMiembro pm
    JOIN FETCH pm.proyecto p
    WHERE pm.usuario.id = :usuarioId
      AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))
      AND p.activo = COALESCE(:activo, p.activo)
    ORDER BY p.nombre ASC
    """)
    List<ProyectoMiembro> listarProyectos(
            @Param("usuarioId") Long usuarioId,
            @Param("nombre") String nombre,
            @Param("activo") Boolean activo
    );
}