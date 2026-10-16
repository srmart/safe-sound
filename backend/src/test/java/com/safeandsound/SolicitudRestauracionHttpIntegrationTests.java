
package com.safeandsound;

import com.safeandsound.model.Archivo;
import com.safeandsound.model.EstadoSolicitudRestauracion;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.ProyectoMiembro;
import com.safeandsound.model.Rol;
import com.safeandsound.model.SolicitudRestauracion;
import com.safeandsound.model.Usuario;
import com.safeandsound.repository.ArchivoRepository;
import com.safeandsound.repository.ProyectoMiembroRepository;
import com.safeandsound.repository.ProyectoRepository;
import com.safeandsound.repository.SolicitudRestauracionRepository;
import com.safeandsound.repository.UsuarioRepository;
import com.safeandsound.security.JwtService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "archivos.storage.path=./target/test-storage/jit-archivos"
})
@AutoConfigureMockMvc
@Transactional
class SolicitudRestauracionHttpIntegrationTests {

    private static final Path STORAGE =
            Path.of("./target/test-storage/jit-archivos")
                    .toAbsolutePath()
                    .normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private ProyectoMiembroRepository proyectoMiembroRepository;

    @Autowired
    private ArchivoRepository archivoRepository;

    @Autowired
    private SolicitudRestauracionRepository solicitudRepository;

    private Usuario crearUsuario(String prefijo) {
        String id = UUID.randomUUID().toString().substring(0, 8);

        Usuario usuario = new Usuario();
        usuario.setUsername(prefijo + "-" + id);
        usuario.setEmail(prefijo + "-" + id + "@test.com");
        usuario.setPasswordHash("hash-de-prueba");

        return usuarioRepository.saveAndFlush(usuario);
    }

    private Proyecto crearProyecto(Usuario owner) {
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("Proyecto JIT");
        proyecto.setDescripcion("Prueba de restauracion JIT");
        proyecto.setCreadoPor(owner);
        proyecto.setActivo(true);

        return proyectoRepository.saveAndFlush(proyecto);
    }

    private void asignarRol(
            Proyecto proyecto,
            Usuario usuario,
            Rol rol
    ) {
        ProyectoMiembro miembro = new ProyectoMiembro();
        miembro.setProyecto(proyecto);
        miembro.setUsuario(usuario);
        miembro.setRol(rol);

        proyectoMiembroRepository.saveAndFlush(miembro);
    }

    private String token(Usuario usuario) {
        return jwtService.generateAccessToken(usuario);
    }

    /*
     * Prepara un archivo real mediante HTTP y lo elimina lógicamente.
     * Si crearSolicitud es true, también genera una solicitud JIT pendiente.
     */
    private Escenario prepararEscenario(boolean crearSolicitud)
            throws Exception {

        Usuario owner = crearUsuario("owner");
        Usuario collaborator = crearUsuario("collab");
        Usuario viewer = crearUsuario("viewer");

        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);
        asignarRol(proyecto, collaborator, Rol.COLLABORATOR);
        asignarRol(proyecto, viewer, Rol.VIEWER);

        String ownerToken = token(owner);
        String collaboratorToken = token(collaborator);
        String viewerToken = token(viewer);

        MockMultipartFile archivoPrueba = new MockMultipartFile(
                "file",
                "prueba.pdf",
                "application/pdf",
                "contenido de prueba JIT".getBytes()
        );

        mockMvc.perform(multipart(
                        "/proyectos/{proyectoId}/archivos",
                        proyecto.getId()
                )
                        .file(archivoPrueba)
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isCreated());

        Archivo archivo = archivoRepository
                .findByProyectoIdAndEliminadoFalse(proyecto.getId())
                .stream()
                .filter(a -> a.getNombreOriginal().equals("prueba.pdf"))
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError("No se encontró el archivo de prueba"));

        // El colaborador puede eliminar el archivo que él mismo subió.
        mockMvc.perform(delete(
                        "/proyectos/{proyectoId}/archivos/{archivoId}",
                        proyecto.getId(),
                        archivo.getId()
                )
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isNoContent());

        SolicitudRestauracion solicitud = null;

        if (crearSolicitud) {
            mockMvc.perform(post(
                            "/proyectos/{proyectoId}/archivos/{archivoId}/solicitudes-restauracion",
                            proyecto.getId(),
                            archivo.getId()
                    )
                            .header("Authorization", "Bearer " + collaboratorToken))
                    .andExpect(status().isCreated());

            solicitud = solicitudRepository
                    .findByProyectoId(proyecto.getId())
                    .stream()
                    .findFirst()
                    .orElseThrow(() ->
                            new AssertionError("No se creó la solicitud JIT"));
        }

        return new Escenario(
                owner,
                collaborator,
                viewer,
                proyecto,
                archivo,
                solicitud,
                ownerToken,
                collaboratorToken,
                viewerToken
        );
    }

    private SolicitudRestauracion aprobar(Escenario e) throws Exception {
        assertNotNull(e.solicitud());

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/solicitudes-restauracion/{solicitudId}/aprobar",
                        e.proyecto().getId(),
                        e.solicitud().getId()
                )
                        .header("Authorization", "Bearer " + e.ownerToken()))
                .andExpect(status().isOk());

        return solicitudRepository
                .findById(e.solicitud().getId())
                .orElseThrow();
    }

    @Test
    void collaboratorPuedeSolicitarRestauracion() throws Exception {
        Escenario e = prepararEscenario(false);

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/archivos/{archivoId}/solicitudes-restauracion",
                        e.proyecto().getId(),
                        e.archivo().getId()
                )
                        .header("Authorization", "Bearer " + e.collaboratorToken()))
                .andExpect(status().isCreated());

        SolicitudRestauracion solicitud = solicitudRepository
                .findByProyectoId(e.proyecto().getId())
                .stream()
                .findFirst()
                .orElseThrow();

        assertEquals(
                EstadoSolicitudRestauracion.PENDIENTE,
                solicitud.getEstado()
        );
        assertEquals(
                e.collaborator().getId(),
                solicitud.getSolicitante().getId()
        );
    }

    @Test
    void viewerNoPuedeSolicitarRestauracion() throws Exception {
        Escenario e = prepararEscenario(false);

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/archivos/{archivoId}/solicitudes-restauracion",
                        e.proyecto().getId(),
                        e.archivo().getId()
                )
                        .header("Authorization", "Bearer " + e.viewerToken()))
                .andExpect(status().isForbidden());

        assertTrue(
                solicitudRepository.findByProyectoId(e.proyecto().getId()).isEmpty()
        );
    }

    @Test
    void ownerPuedeAprobarSolicitudAjena() throws Exception {
        Escenario e = prepararEscenario(true);

        SolicitudRestauracion aprobada = aprobar(e);

        assertEquals(
                EstadoSolicitudRestauracion.APROBADA,
                aprobada.getEstado()
        );
        assertEquals(
                e.owner().getId(),
                aprobada.getAprobador().getId()
        );
        assertNotNull(aprobada.getVenceEn());
    }

    @Test
    void collaboratorNoPuedeAprobarSolicitud() throws Exception {
        Escenario e = prepararEscenario(true);

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/solicitudes-restauracion/{solicitudId}/aprobar",
                        e.proyecto().getId(),
                        e.solicitud().getId()
                )
                        .header("Authorization", "Bearer " + e.collaboratorToken()))
                .andExpect(status().isForbidden());

        SolicitudRestauracion solicitud = solicitudRepository
                .findById(e.solicitud().getId())
                .orElseThrow();

        assertEquals(
                EstadoSolicitudRestauracion.PENDIENTE,
                solicitud.getEstado()
        );
    }

    @Test
    void ownerPuedeRechazarSolicitudAjena() throws Exception {
        Escenario e = prepararEscenario(true);

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/solicitudes-restauracion/{solicitudId}/rechazar",
                        e.proyecto().getId(),
                        e.solicitud().getId()
                )
                        .header("Authorization", "Bearer " + e.ownerToken()))
                .andExpect(status().isOk());

        SolicitudRestauracion rechazada = solicitudRepository
                .findById(e.solicitud().getId())
                .orElseThrow();

        assertEquals(
                EstadoSolicitudRestauracion.RECHAZADA,
                rechazada.getEstado()
        );
    }

    @Test
    void solicitantePuedeRestaurarConAutorizacionVigenteUnaSolaVez()
            throws Exception {

        Escenario e = prepararEscenario(true);

        SolicitudRestauracion aprobada = aprobar(e);

        assertEquals(
                EstadoSolicitudRestauracion.APROBADA,
                aprobada.getEstado()
        );

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/solicitudes-restauracion/{solicitudId}/restaurar",
                        e.proyecto().getId(),
                        e.solicitud().getId()
                )
                        .header("Authorization", "Bearer " + e.collaboratorToken()))
                .andExpect(status().isNoContent());

        Archivo restaurado = archivoRepository
                .findById(e.archivo().getId())
                .orElseThrow();

        assertFalse(restaurado.isEliminado());
        assertNull(restaurado.getEliminadoEn());

        SolicitudRestauracion utilizada = solicitudRepository
                .findById(e.solicitud().getId())
                .orElseThrow();

        assertNotNull(utilizada.getUsadaEn());
        assertEquals(
                EstadoSolicitudRestauracion.UTILIZADA,
                utilizada.getEstado()
        );

        // La misma autorización no puede utilizarse dos veces.
        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/solicitudes-restauracion/{solicitudId}/restaurar",
                        e.proyecto().getId(),
                        e.solicitud().getId()
                )
                        .header("Authorization", "Bearer " + e.collaboratorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerNoPuedeUsarLaAutorizacionJitDeOtroUsuario()
            throws Exception {

        Escenario e = prepararEscenario(true);
        aprobar(e);

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/solicitudes-restauracion/{solicitudId}/restaurar",
                        e.proyecto().getId(),
                        e.solicitud().getId()
                )
                        .header("Authorization", "Bearer " + e.ownerToken()))
                .andExpect(status().isForbidden());

        Archivo archivo = archivoRepository
                .findById(e.archivo().getId())
                .orElseThrow();

        assertTrue(archivo.isEliminado());
    }

    @Test
    void solicitudRechazadaNoPermiteRestaurar() throws Exception {
        Escenario e = prepararEscenario(true);

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/solicitudes-restauracion/{solicitudId}/rechazar",
                        e.proyecto().getId(),
                        e.solicitud().getId()
                )
                        .header("Authorization", "Bearer " + e.ownerToken()))
                .andExpect(status().isOk());

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/solicitudes-restauracion/{solicitudId}/restaurar",
                        e.proyecto().getId(),
                        e.solicitud().getId()
                )
                        .header("Authorization", "Bearer " + e.collaboratorToken()))
                .andExpect(status().isForbidden());

        assertTrue(
                archivoRepository.findById(e.archivo().getId())
                        .orElseThrow()
                        .isEliminado()
        );
    }

    @AfterEach
    void limpiarAlmacenamientoDePruebas() throws IOException {
        // Solo limpiamos el directorio dedicado a esta clase de tests.
        if (Files.isDirectory(STORAGE)) {
            try (Stream<Path> archivos = Files.list(STORAGE)) {
                for (Path archivo : archivos.toList()) {
                    Files.deleteIfExists(archivo);
                }
            }
        }
    }

    private record Escenario(
            Usuario owner,
            Usuario collaborator,
            Usuario viewer,
            Proyecto proyecto,
            Archivo archivo,
            SolicitudRestauracion solicitud,
            String ownerToken,
            String collaboratorToken,
            String viewerToken
    ) {}
}
