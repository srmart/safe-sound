
package com.safeandsound.service;

import com.safeandsound.exception.ProyectoDebeTenerOwnerException;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.ProyectoMiembro;
import com.safeandsound.model.Rol;
import com.safeandsound.model.Usuario;
import com.safeandsound.repository.ProyectoMiembroRepository;
import com.safeandsound.repository.ProyectoRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final ProyectoMiembroRepository proyectoMiembroRepository;
    private final UsuarioService usuarioService;

    public ProyectoService(
            ProyectoRepository proyectoRepository,
            ProyectoMiembroRepository proyectoMiembroRepository,
            UsuarioService usuarioService
    ) {
        this.proyectoRepository = proyectoRepository;
        this.proyectoMiembroRepository = proyectoMiembroRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public Proyecto crearProyecto(
            Long usuarioId,
            String nombre,
            String descripcion
    ) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId);

        Proyecto proyecto = new Proyecto();
        proyecto.setNombre(nombre);
        proyecto.setDescripcion(descripcion);
        proyecto.setCreadoPor(usuario);
        proyecto.setActivo(true);

        Proyecto proyectoGuardado = proyectoRepository.save(proyecto);

        ProyectoMiembro miembro = new ProyectoMiembro();
        miembro.setProyecto(proyectoGuardado);
        miembro.setUsuario(usuario);
        miembro.setRol(Rol.OWNER);

        proyectoMiembroRepository.save(miembro);

        return proyectoGuardado;
    }

    public Proyecto obtenerProyecto(Long proyectoId) {
        return proyectoRepository.findById(proyectoId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Proyecto no encontrado"
                        )
                );
    }

    // INC-9 / RS13:
    // Impide escrituras en proyectos archivados.
    // El bloqueo se mantiene durante la transacción.
    private Proyecto obtenerProyectoActivoConBloqueo(Long proyectoId) {
        Proyecto proyecto = proyectoRepository
                .buscarPorIdConBloqueo(proyectoId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Proyecto no encontrado"
                        )
                );

        if (!proyecto.isActivo()) {
            throw new AccessDeniedException(
                    "No se permiten modificaciones en un proyecto archivado"
            );
        }

        return proyecto;
    }

    @Transactional
    public Proyecto modificarProyecto(
            Long proyectoId,
            String nombre,
            String descripcion
    ) {
        // INC-9: Verificar estado y bloquear el proyecto.
        Proyecto proyecto = obtenerProyectoActivoConBloqueo(proyectoId);

        // RS16: Modificar únicamente los campos proporcionados.
        if (nombre != null) {
            proyecto.setNombre(nombre);
        }

        if (descripcion != null) {
            proyecto.setDescripcion(descripcion);
        }

        return proyectoRepository.save(proyecto);
    }

    @Transactional
    public void archivarProyecto(Long proyectoId) {
        // INC-9: No permitir archivar un proyecto ya archivado.
        Proyecto proyecto = obtenerProyectoActivoConBloqueo(proyectoId);

        proyecto.setActivo(false);

        proyectoRepository.save(proyecto);
    }

    @Transactional
    public void agregarMiembro(
            Long proyectoId,
            Long usuarioId,
            Rol rol
    ) {
        // INC-9: No agregar miembros a proyectos archivados.
        Proyecto proyecto = obtenerProyectoActivoConBloqueo(proyectoId);

        Usuario usuario = usuarioService.buscarPorId(usuarioId);

        if (proyectoMiembroRepository.existsByProyectoIdAndUsuarioId(
                proyectoId,
                usuarioId
        )) {
            throw new IllegalArgumentException(
                    "El usuario ya pertenece al proyecto"
            );
        }

        ProyectoMiembro miembro = new ProyectoMiembro();
        miembro.setProyecto(proyecto);
        miembro.setUsuario(usuario);
        miembro.setRol(rol);

        proyectoMiembroRepository.save(miembro);
    }

    @Transactional
    public void modificarRol(
            Long proyectoId,
            Long usuarioId,
            Rol nuevoRol
    ) {
        // INC-9: Verificar estado y bloquear el proyecto.
        // También evita modificaciones simultáneas que puedan
        // dejar el proyecto sin un OWNER.
        obtenerProyectoActivoConBloqueo(proyectoId);

        ProyectoMiembro miembro = proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(proyectoId, usuarioId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El usuario no pertenece al proyecto"
                        )
                );

        // Evitar que el último OWNER pierda su rol.
        if (miembro.getRol() == Rol.OWNER && nuevoRol != Rol.OWNER) {
            long cantidadOwners = proyectoMiembroRepository
                    .countByProyectoIdAndRol(proyectoId, Rol.OWNER);

            if (cantidadOwners <= 1) {
                throw new ProyectoDebeTenerOwnerException(
                        "El proyecto debe tener al menos un OWNER"
                );
            }
        }

        miembro.setRol(nuevoRol);

        proyectoMiembroRepository.save(miembro);
    }

    public List<Proyecto> obtenerProyectosDelUsuario(Long usuarioId) {
        return proyectoMiembroRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(ProyectoMiembro::getProyecto)
                .toList();
    }

    // R5: Listado de proyectos con búsqueda y filtros.
    @Transactional(readOnly = true)
    public List<ProyectoMiembro> listarProyectos(
            Long usuarioId,
            String nombre,
            Boolean activo
    ) {
        if (nombre == null) {
            nombre = "";
        }

        return proyectoMiembroRepository.listarProyectos(
                usuarioId,
                nombre,
                activo
        );
    }

    // Obtener los miembros de un proyecto.
    // Se permite consultar proyectos archivados.
    @Transactional(readOnly = true)
    public List<ProyectoMiembro> listarMiembros(Long proyectoId) {
        return proyectoMiembroRepository.listarMiembros(proyectoId);
    }
}
