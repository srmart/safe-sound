package com.safeandsound;

import com.safeandsound.controller.ProyectoController;
import com.safeandsound.exception.GlobalExceptionHandler;
import com.safeandsound.model.Operacion;
import com.safeandsound.service.AutorizacionService;
import com.safeandsound.service.ProyectoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.safeandsound.service.AuditService;

import com.safeandsound.service.UsuarioService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class CrossTenantHttpTests {

    private ProyectoService proyectoService;
    private AutorizacionService autorizacionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        proyectoService = mock(ProyectoService.class);
        autorizacionService = mock(AutorizacionService.class);

        // <-- CAMBIO AQUÍ: Se agrega el mock de AuditService como tercer argumento
        ProyectoController proyectoController =
                new ProyectoController(
                        proyectoService,
                        autorizacionService,
                        mock(AuditService.class),
                        mock(UsuarioService.class)
                );

        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(proyectoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "El usuario no pertenece al proyecto",
            "El rol VIEWER no tiene permiso para VER_PROYECTO",
            "El rol COLLABORATOR no tiene permiso para VER_AUDITORIA",
            "No se pudo verificar la autorización del usuario",
            "La solicitud no pertenece a este proyecto"
    })
    void denegacionesHttpNoRevelanElMotivoInterno(String motivo) throws Exception {

        Long usuarioId = 1L;
        Long proyectoAjenoId = 2L;

        doThrow(new AccessDeniedException(motivo))
                .when(autorizacionService)
                .verificarPermiso(
                        usuarioId,
                        proyectoAjenoId,
                        Operacion.VER_PROYECTO
                );

        var auth =
                new UsernamePasswordAuthenticationToken(
                        usuarioId,
                        null
                );

        mockMvc.perform(
                        get("/proyectos/{proyectoId}", proyectoAjenoId)
                                .principal(auth)
                )
                .andExpect(status().isForbidden())
                .andExpect(content().string(
                        "Acceso denegado"
                ));

        verify(autorizacionService).verificarPermiso(
                usuarioId,
                proyectoAjenoId,
                Operacion.VER_PROYECTO
        );

        verify(proyectoService, never())
                .obtenerProyecto(proyectoAjenoId);
    }
}
