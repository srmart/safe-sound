
package com.safeandsound;

import com.safeandsound.dto.ModificarProyectoRequest;
import com.safeandsound.dto.ModificarMiembroRequest;
import com.safeandsound.dto.ConfirmarPasswordRequest;
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
                        proyectoAjenoId, crearModificarProyectoRequest(),
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
                        nuevoUsuarioId, crearModificarMiembroRequest(Rol.VIEWER),
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
                        usuarioObjetivoId, crearModificarMiembroRequest(Rol.COLLABORATOR),
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
                        proyectoAjenoId, crearConfirmarPasswordRequest(),
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

    private static ModificarProyectoRequest crearModificarProyectoRequest() {
        ModificarProyectoRequest request = new ModificarProyectoRequest();
        request.setNombre("Nombre manipulado");
        request.setDescripcion("Descripcion manipulada");
        request.setPasswordConfirmacion("password-de-prueba".toCharArray());
        return request;
    }

    private static ModificarMiembroRequest crearModificarMiembroRequest(Rol rol) {
        ModificarMiembroRequest request = new ModificarMiembroRequest();
        request.setRol(rol);
        request.setPasswordConfirmacion("password-de-prueba".toCharArray());
        return request;
    }

    private static ConfirmarPasswordRequest crearConfirmarPasswordRequest() {
        ConfirmarPasswordRequest request = new ConfirmarPasswordRequest();
        request.setPasswordConfirmacion("password-de-prueba".toCharArray());
        return request;
    }

}
