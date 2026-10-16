
package com.safeandsound;

import com.safeandsound.controller.ProyectoController;
import com.safeandsound.model.Operacion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.Rol;
import com.safeandsound.model.Usuario;
import com.safeandsound.service.AuditService;
import com.safeandsound.service.AutorizacionService;
import com.safeandsound.service.ProyectoService;
import com.safeandsound.service.UsuarioService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CrossTenantAccessTests {

    private ProyectoService proyectoService;
    private AutorizacionService autorizacionService;
    private UsuarioService usuarioService;
    private ProyectoController proyectoController;

    @BeforeEach
    void setUp() {
        proyectoService = mock(ProyectoService.class);
        autorizacionService = mock(AutorizacionService.class);
        usuarioService = mock(UsuarioService.class);

        proyectoController = new ProyectoController(
                proyectoService,
                autorizacionService,
                mock(AuditService.class),
                usuarioService
        );
    }

    @Test
    void usuarioNoPuedeAccederAProyectoDelQueNoEsMiembro() {

        Long usuarioId = 1L;
        Long proyectoAjenoId = 2L;

        var authentication = new UsernamePasswordAuthenticationToken(
                usuarioId, null
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

        var authentication = new UsernamePasswordAuthenticationToken(
                usuarioId, null
        );

        Usuario propietario = new Usuario();
        propietario.setUsername("usuario-test");

        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("Proyecto de prueba");
        proyecto.setDescripcion("Descripcion de prueba");
        proyecto.setCreadoPor(propietario);
        proyecto.setActivo(true);

        when(proyectoService.obtenerProyecto(proyectoId))
                .thenReturn(proyecto);

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

        var authentication = new UsernamePasswordAuthenticationToken(
                usuarioId, null
        );

        doThrow(new AccessDeniedException(
                "El usuario no pertenece al proyecto"
        ))
                .when(autorizacionService)
                .verificarPermiso(
                        usuarioId,
                        proyectoAjenoId,
                        Operacion.MODIFICAR_NOMBRE_PROYECTO
                );

        assertThrows(
                AccessDeniedException.class,
                () -> proyectoController.modificarProyecto(
                        proyectoAjenoId,
                        "Nombre manipulado",
                        "Descripción manipulada",
                        "password-de-prueba",
                        authentication
                )
        );

        verify(usuarioService).verificarPassword(
                eq(usuarioId),
                any(char[].class)
        );

        verify(autorizacionService).verificarPermiso(
                usuarioId,
                proyectoAjenoId,
                Operacion.MODIFICAR_NOMBRE_PROYECTO
        );

        verify(proyectoService, never())
                .modificarProyecto(
                        anyLong(),
                        any(),
                        any()
                );
    }

    @Test
    void ownerDeUnProyectoNoPuedeAgregarMiembroAOtroProyecto() {

        Long usuarioActualId = 1L;
        Long proyectoAjenoId = 2L;
        Long nuevoUsuarioId = 3L;

        var authentication = new UsernamePasswordAuthenticationToken(
                usuarioActualId, null
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
                        "password-de-prueba",
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

        var authentication = new UsernamePasswordAuthenticationToken(
                usuarioActualId, null
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
                        "password-de-prueba",
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

        var authentication = new UsernamePasswordAuthenticationToken(
                usuarioId, null
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
                        "password-de-prueba",
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
