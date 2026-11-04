
package com.safeandsound;

import com.safeandsound.model.Archivo;
import com.safeandsound.model.Proyecto;
import com.safeandsound.model.ProyectoMiembro;
import com.safeandsound.model.Rol;
import com.safeandsound.model.Usuario;
import com.safeandsound.repository.ArchivoRepository;
import com.safeandsound.repository.ProyectoMiembroRepository;
import com.safeandsound.repository.ProyectoRepository;
import com.safeandsound.repository.UsuarioRepository;
import com.safeandsound.security.JwtService;
import com.safeandsound.security.PasswordHasher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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
        "archivos.storage.path=./target/test-storage/inc9-archivos"
})
@AutoConfigureMockMvc
class ProyectoArchivadoHttpIntegrationTests {

    private static final String PASSWORD = "PasswordTest123!";

    private static final Path STORAGE =
            Path.of("./target/test-storage/inc9-archivos")
                    .toAbsolutePath()
                    .normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private ProyectoMiembroRepository proyectoMiembroRepository;

    @Autowired
    private ArchivoRepository archivoRepository;

    private Usuario crearUsuario(String prefijo) {
        String id = UUID.randomUUID().toString().substring(0, 8);

        Usuario usuario = new Usuario();
        usuario.setUsername(prefijo + "-" + id);
        usuario.setEmail(prefijo + "-" + id + "@test.com");
        usuario.setPasswordHash(
                passwordHasher.hash(PASSWORD.toCharArray())
        );

        return usuarioRepository.saveAndFlush(usuario);
    }

    private Proyecto crearProyecto(Usuario owner) {
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("Proyecto INC-9");
        proyecto.setDescripcion("Prueba de proyecto archivado");
        proyecto.setCreadoPor(owner);
        proyecto.setActivo(true);

        return proyectoRepository.saveAndFlush(proyecto);
    }

    private void asignarRol(
            Proyecto proyecto,
            Usuario usuario,
            Rol rol) {

        ProyectoMiembro miembro = new ProyectoMiembro();
        miembro.setProyecto(proyecto);
        miembro.setUsuario(usuario);
        miembro.setRol(rol);

        proyectoMiembroRepository.saveAndFlush(miembro);
    }

    private String token(Usuario usuario) {
        return jwtService.generateAccessToken(usuario);
    }

    private Escenario prepararEscenario() throws Exception {
        Usuario owner = crearUsuario("owner");
        Usuario collaborator = crearUsuario("collab");
        Usuario viewer = crearUsuario("viewer");
        Usuario nuevo = crearUsuario("nuevo");

        Proyecto proyecto = crearProyecto(owner);

        asignarRol(proyecto, owner, Rol.OWNER);
        asignarRol(proyecto, collaborator, Rol.COLLABORATOR);
        asignarRol(proyecto, viewer, Rol.VIEWER);

        String ownerToken = token(owner);
        String collaboratorToken = token(collaborator);
        String viewerToken = token(viewer);

        MockMultipartFile archivoPrueba = new MockMultipartFile(
                "file",
                "inc9.pdf",
                "application/pdf",
                "contenido de prueba INC-9".getBytes()
        );

        mockMvc.perform(multipart(
                        "/proyectos/{proyectoId}/archivos",
                        proyecto.getId())
                        .file(archivoPrueba)
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isCreated());

        Archivo archivo = archivoRepository
                .findByProyectoIdAndEliminadoFalse(proyecto.getId())
                .stream()
                .filter(a -> a.getNombreOriginal().equals("inc9.pdf"))
                .findFirst()
                .orElseThrow();

        return new Escenario(
                owner,
                collaborator,
                viewer,
                nuevo,
                proyecto,
                archivo,
                ownerToken,
                collaboratorToken,
                viewerToken
        );
    }

    private void archivar(Escenario e) {
        e.proyecto().setActivo(false);
        proyectoRepository.saveAndFlush(e.proyecto());
    }

    @Test
    void proyectoArchivadoPermiteConsultarProyectoYMiembros()
            throws Exception {

        Escenario e = prepararEscenario();
        archivar(e);

        mockMvc.perform(get("/proyectos/{proyectoId}",
                        e.proyecto().getId())
                        .header("Authorization", "Bearer " + e.viewerToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/proyectos/{proyectoId}/miembros",
                        e.proyecto().getId())
                        .header("Authorization", "Bearer " + e.ownerToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/proyectos")
                        .param("activo", "false")
                        .header("Authorization", "Bearer " + e.ownerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void proyectoArchivadoPermiteConsultarYDescargarArchivos()
            throws Exception {

        Escenario e = prepararEscenario();
        archivar(e);

        mockMvc.perform(get("/proyectos/{proyectoId}/archivos",
                        e.proyecto().getId())
                        .header("Authorization", "Bearer " + e.viewerToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get(
                        "/proyectos/{proyectoId}/archivos/{archivoId}",
                        e.proyecto().getId(),
                        e.archivo().getId())
                        .header("Authorization", "Bearer " + e.viewerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void proyectoArchivadoNoPermiteModificarNombreNiDescripcion()
            throws Exception {

        Escenario e = prepararEscenario();
        archivar(e);

        mockMvc.perform(put("/proyectos/{proyectoId}",
                        e.proyecto().getId())
.contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"nombre\":\"%s\",\"passwordConfirmacion\":\"%s\"}", "Nombre modificado", PASSWORD))
                        .header("Authorization", "Bearer " + e.ownerToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/proyectos/{proyectoId}",
                        e.proyecto().getId())
.contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"descripcion\":\"%s\",\"passwordConfirmacion\":\"%s\"}", "Descripcion modificada", PASSWORD))
                        .header("Authorization", "Bearer " + e.ownerToken()))
                .andExpect(status().isForbidden());

        Proyecto proyecto = proyectoRepository
                .findById(e.proyecto().getId())
                .orElseThrow();

        assertEquals("Proyecto INC-9", proyecto.getNombre());
        assertEquals(
                "Prueba de proyecto archivado",
                proyecto.getDescripcion()
        );
    }

    @Test
    void proyectoArchivadoNoPermiteAgregarNiModificarMiembros()
            throws Exception {

        Escenario e = prepararEscenario();
        archivar(e);

        mockMvc.perform(post(
                        "/proyectos/{proyectoId}/miembros/{usuarioId}",
                        e.proyecto().getId(),
                        e.nuevo().getId())
.contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"rol\":\"%s\",\"passwordConfirmacion\":\"%s\"}", "VIEWER", PASSWORD))
                        .header("Authorization", "Bearer " + e.ownerToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(
                        "/proyectos/{proyectoId}/miembros/{usuarioId}/rol",
                        e.proyecto().getId(),
                        e.collaborator().getId())
.contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"rol\":\"%s\",\"passwordConfirmacion\":\"%s\"}", "VIEWER", PASSWORD))
                        .header("Authorization", "Bearer " + e.ownerToken()))
                .andExpect(status().isForbidden());

        assertTrue(
                proyectoMiembroRepository
                        .findByProyectoIdAndUsuarioId(
                                e.proyecto().getId(),
                                e.nuevo().getId())
                        .isEmpty()
        );

        ProyectoMiembro miembro = proyectoMiembroRepository
                .findByProyectoIdAndUsuarioId(
                        e.proyecto().getId(),
                        e.collaborator().getId())
                .orElseThrow();

        assertEquals(Rol.COLLABORATOR, miembro.getRol());
    }

    @Test
    void proyectoArchivadoNoPermiteSubirNiEliminarArchivos()
            throws Exception {

        Escenario e = prepararEscenario();
        archivar(e);

        MockMultipartFile nuevoArchivo = new MockMultipartFile(
                "file",
                "nuevo.pdf",
                "application/pdf",
                "contenido nuevo".getBytes()
        );

        mockMvc.perform(multipart(
                        "/proyectos/{proyectoId}/archivos",
                        e.proyecto().getId())
                        .file(nuevoArchivo)
                        .header("Authorization", "Bearer " + e.collaboratorToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(
                        "/proyectos/{proyectoId}/archivos/{archivoId}",
                        e.proyecto().getId(),
                        e.archivo().getId())
                        .header("Authorization", "Bearer " + e.collaboratorToken()))
                .andExpect(status().isForbidden());

        assertEquals(
                1,
                archivoRepository
                        .findByProyectoIdAndEliminadoFalse(
                                e.proyecto().getId())
                        .size()
        );

        assertFalse(
                archivoRepository.findById(
                        e.archivo().getId()
                ).orElseThrow().isEliminado()
        );
    }

    @Test
    void proyectoArchivadoNoPermiteArchivarNuevamente()
            throws Exception {

        Escenario e = prepararEscenario();
        archivar(e);

        mockMvc.perform(delete("/proyectos/{proyectoId}",
                        e.proyecto().getId())
.contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"passwordConfirmacion\":\"%s\"}", PASSWORD))
                        .header("Authorization", "Bearer " + e.ownerToken()))
                .andExpect(status().isForbidden());

        assertFalse(
                proyectoRepository.findById(
                        e.proyecto().getId()
                ).orElseThrow().isActivo()
        );
    }

    @Test
    void proyectoActivoPermiteModificarDescripcion()
            throws Exception {

        Escenario e = prepararEscenario();

        mockMvc.perform(put("/proyectos/{proyectoId}",
                        e.proyecto().getId())
.contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"descripcion\":\"%s\",\"passwordConfirmacion\":\"%s\"}", "Descripcion actualizada", PASSWORD))
                        .header("Authorization", "Bearer " + e.ownerToken()))
                .andExpect(status().isOk());

        assertEquals(
                "Descripcion actualizada",
                proyectoRepository.findById(
                        e.proyecto().getId()
                ).orElseThrow().getDescripcion()
        );
    }

    @AfterEach
    void limpiarAlmacenamientoDePruebas() throws IOException {
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
            Usuario nuevo,
            Proyecto proyecto,
            Archivo archivo,
            String ownerToken,
            String collaboratorToken,
            String viewerToken
    ) {}
}
