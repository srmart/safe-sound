package com.safeandsound;

import com.safeandsound.controller.ProyectoController;
import com.safeandsound.model.Operacion;
import com.safeandsound.model.Rol;
import com.safeandsound.service.AutorizacionService;
import com.safeandsound.service.ProyectoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class CrossTenantAccessTests {

    private ProyectoService proyectoService;
    private AutorizacionService autorizacionService;
    private ProyectoController proyectoController;

    @BeforeEach
    void setUp() {
        proyectoService = mock(ProyectoService.class);
        autorizacionService = mock(AutorizacionService.class);

        proyectoController = new ProyectoController(
                proyectoService,
                autorizacionService
        );
    }

    @Test
    void usuarioNoPuedeAccederAProyectoDelQueNoEsMiembro() {

        Long usuarioId = 1L;
        Long proyectoAjenoId = 2L;

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        usuarioId,
                        null
                );

        doThrow(new AccessDeniedException(
                "El usuario no pertenece al proyecto"
        ))
                .when(autorizacionService)
                .verificarPermiso(
                        usuarioId,
                        proyectoAjenoId,
                        Operacion.VER_PROYECTO
                );

        assertThrows(
                AccessDeniedException.class,
                () -> proyectoController.obtenerProyecto(
                        proyectoAjenoId,
                        authentication
                )
        );

        verify(autorizacionService).verificarPermiso(
                usuarioId,
                proyectoAjenoId,
                Operacion.VER_PROYECTO
        );

        verify(proyectoService, never())
                .obtenerProyecto(proyectoAjenoId);
    }

    @Test
    void usuarioPuedeAccederAProyectoDelQueEsMiembro() {

        Long usuarioId = 1L;
        Long proyectoId = 1L;

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        usuarioId,
                        null
                );

        assertDoesNotThrow(
                () -> proyectoController.obtenerProyecto(
                        proyectoId,
                        authentication
                )
        );

        verify(autorizacionService).verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.VER_PROYECTO
        );

        verify(proyectoService).obtenerProyecto(proyectoId);
    }

    @Test
    void ownerDeUnProyectoNoPuedeModificarOtroProyecto() {

        Long usuarioId = 1L;
        Long proyectoAjenoId = 2L;

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        usuarioId,
                        null
                );

        doThrow(new AccessDeniedException(
                "El usuario no pertenece al proyecto"
        ))
                .when(autorizacionService)
                .verificarPermiso(
                        usuarioId,
                        proyectoAjenoId,
                        Operacion.MODIFICAR_PROYECTO
                );

        assertThrows(
                AccessDeniedException.class,
                () -> proyectoController.modificarProyecto(
                        proyectoAjenoId,
                        "Nombre manipulado",
                        "Descripción manipulada",
                        authentication
                )
        );

        verify(autorizacionService).verificarPermiso(
                usuarioId,
                proyectoAjenoId,
                Operacion.MODIFICAR_PROYECTO
        );

        verify(proyectoService, never())
                .modificarProyecto(
                        proyectoAjenoId,
                        "Nombre manipulado",
                        "Descripción manipulada"
                );
    }

    @Test
    void ownerDeUnProyectoNoPuedeAgregarMiembroAOtroProyecto() {

        Long usuarioActualId = 1L;
        Long proyectoAjenoId = 2L;
        Long nuevoUsuarioId = 3L;

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        usuarioActualId,
                        null
                );

        doThrow(new AccessDeniedException(
                "El usuario no pertenece al proyecto"
        ))
                .when(autorizacionService)
                .verificarPermiso(
                        usuarioActualId,
                        proyectoAjenoId,
                        Operacion.MODIFICAR_ROLES
                );

        assertThrows(
                AccessDeniedException.class,
                () -> proyectoController.agregarMiembro(
                        proyectoAjenoId,
                        nuevoUsuarioId,
                        Rol.VIEWER,
                        authentication
                )
        );

        verify(autorizacionService).verificarPermiso(
                usuarioActualId,
                proyectoAjenoId,
                Operacion.MODIFICAR_ROLES
        );

        verify(proyectoService, never())
                .agregarMiembro(
                        proyectoAjenoId,
                        nuevoUsuarioId,
                        Rol.VIEWER
                );
    }

    @Test
    void ownerDeUnProyectoNoPuedeModificarRolEnOtroProyecto() {

        Long usuarioActualId = 1L;
        Long proyectoAjenoId = 2L;
        Long usuarioObjetivoId = 3L;

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        usuarioActualId,
                        null
                );

        doThrow(new AccessDeniedException(
                "El usuario no pertenece al proyecto"
        ))
                .when(autorizacionService)
                .verificarPermiso(
                        usuarioActualId,
                        proyectoAjenoId,
                        Operacion.MODIFICAR_ROLES
                );

        assertThrows(
                AccessDeniedException.class,
                () -> proyectoController.modificarRol(
                        proyectoAjenoId,
                        usuarioObjetivoId,
                        Rol.COLLABORATOR,
                        authentication
                )
        );

        verify(autorizacionService).verificarPermiso(
                usuarioActualId,
                proyectoAjenoId,
                Operacion.MODIFICAR_ROLES
        );

        verify(proyectoService, never())
                .modificarRol(
                        proyectoAjenoId,
                        usuarioObjetivoId,
                        Rol.COLLABORATOR
                );
    }

    @Test
    void ownerDeUnProyectoNoPuedeArchivarOtroProyecto() {

        Long usuarioId = 1L;
        Long proyectoAjenoId = 2L;

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        usuarioId,
                        null
                );

        doThrow(new AccessDeniedException(
                "El usuario no pertenece al proyecto"
        ))
                .when(autorizacionService)
                .verificarPermiso(
                        usuarioId,
                        proyectoAjenoId,
                        Operacion.ARCHIVAR_PROYECTO
                );

        assertThrows(
                AccessDeniedException.class,
                () -> proyectoController.archivarProyecto(
                        proyectoAjenoId,
                        authentication
                )
        );

        verify(autorizacionService).verificarPermiso(
                usuarioId,
                proyectoAjenoId,
                Operacion.ARCHIVAR_PROYECTO
        );

        verify(proyectoService, never())
                .archivarProyecto(proyectoAjenoId);
    }
}