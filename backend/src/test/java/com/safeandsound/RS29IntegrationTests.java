
package com.safeandsound;

import com.safeandsound.controller.ProyectoController;
import com.safeandsound.exception.GlobalExceptionHandler;
import com.safeandsound.model.Operacion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.Usuario;
import com.safeandsound.service.AuditService;
import com.safeandsound.service.AutorizacionService;
import com.safeandsound.service.ProyectoService;
import com.safeandsound.service.UsuarioService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RS29IntegrationTests {

    private static final String PASSWORD_TEST = "PasswordTest!2026";

    private ProyectoService proyectoService;
    private AutorizacionService autorizacionService;
    private UsuarioService usuarioService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proyectoService = mock(ProyectoService.class);
        autorizacionService = mock(AutorizacionService.class);
        usuarioService = mock(UsuarioService.class);

        ProyectoController controller = new ProyectoController(
                proyectoService,
                autorizacionService,
                mock(AuditService.class),
                usuarioService
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        SecurityContextHolder.clearContext();
    }

    private UsernamePasswordAuthenticationToken autenticar(Long usuarioId) {
        return new UsernamePasswordAuthenticationToken(
                usuarioId,
                null,
                Collections.emptyList()
        );
    }

    private Proyecto crearProyectoSimulado(String nombre) {
        Usuario propietario = new Usuario();
        propietario.setUsername("usuario-test");

        Proyecto proyecto = new Proyecto();
        proyecto.setNombre(nombre);
        proyecto.setDescripcion("Proyecto de prueba");
        proyecto.setCreadoPor(propietario);
        proyecto.setActivo(true);

        return proyecto;
    }

    @Test
    void parametrosDelClienteNoDebenModificarIdentidad() throws Exception {
        Long usuarioAutenticado = 12L;
        Long proyectoId = 4L;

        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService)
                .verificarPermiso(
                        usuarioAutenticado,
                        proyectoId,
                        Operacion.VER_PROYECTO
                );

        mockMvc.perform(
                        get("/proyectos/{proyectoId}", proyectoId)
                                .principal(autenticar(usuarioAutenticado))
                                .param("usuarioId", "11")
                                .param("rol", "OWNER")
                )
                .andExpect(status().isForbidden());

        verify(autorizacionService).verificarPermiso(
                usuarioAutenticado,
                proyectoId,
                Operacion.VER_PROYECTO
        );

        verify(proyectoService, never()).obtenerProyecto(anyLong());
    }

    @Test
    void identidadAutenticadaDebeLlegarAlServicioDeAutorizacion()
            throws Exception {

        Long usuarioAutenticado = 11L;
        Long proyectoId = 4L;

        Proyecto proyecto = crearProyectoSimulado("BandaRock");

        when(proyectoService.obtenerProyecto(proyectoId))
                .thenReturn(proyecto);

        mockMvc.perform(
                        get("/proyectos/{proyectoId}", proyectoId)
                                .principal(autenticar(usuarioAutenticado))
                )
                .andExpect(status().isOk());

        verify(autorizacionService).verificarPermiso(
                usuarioAutenticado,
                proyectoId,
                Operacion.VER_PROYECTO
        );

        verify(proyectoService).obtenerProyecto(proyectoId);
    }

    @Test
    void usuarioSinPermisoNoDebeModificarProyecto() throws Exception {
        Long usuarioId = 12L;
        Long proyectoId = 4L;

        doThrow(new AccessDeniedException("Sin permisos"))
                .when(autorizacionService)
                .verificarPermiso(
                        usuarioId,
                        proyectoId,
                        Operacion.MODIFICAR_NOMBRE_PROYECTO
                );

        mockMvc.perform(
                        put("/proyectos/{proyectoId}", proyectoId)
                                .principal(autenticar(usuarioId))
                                .param("nombre", "Proyecto modificado")
                                .param("passwordConfirmacion", PASSWORD_TEST)
                )
                .andExpect(status().isForbidden());

        verify(usuarioService).verificarPassword(
                eq(usuarioId),
                any(char[].class)
        );

        verify(autorizacionService).verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.MODIFICAR_NOMBRE_PROYECTO
        );

        verify(proyectoService, never())
                .modificarProyecto(anyLong(), any(), any());
    }

    @Test
    void modificacionDebeAutorizarseSobreProyectoSolicitado()
            throws Exception {

        Long usuarioId = 11L;
        Long proyectoId = 4L;

        Proyecto proyecto = crearProyectoSimulado("BandaRock");

        when(proyectoService.modificarProyecto(
                eq(proyectoId),
                eq("BandaRock"),
                isNull()
        )).thenReturn(proyecto);

        mockMvc.perform(
                        put("/proyectos/{proyectoId}", proyectoId)
                                .principal(autenticar(usuarioId))
                                .param("nombre", "BandaRock")
                                .param("passwordConfirmacion", PASSWORD_TEST)
                )
                .andExpect(status().isOk());

        verify(usuarioService).verificarPassword(
                eq(usuarioId),
                any(char[].class)
        );

        verify(autorizacionService).verificarPermiso(
                usuarioId,
                proyectoId,
                Operacion.MODIFICAR_NOMBRE_PROYECTO
        );

        verify(proyectoService).modificarProyecto(
                proyectoId,
                "BandaRock",
                null
        );
    }
}
