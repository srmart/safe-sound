package com.safeandsound;

import com.safeandsound.model.Usuario;
import com.safeandsound.repository.UsuarioRepository;
import com.safeandsound.security.JwtService;
import com.safeandsound.service.RegistroIntentoAccesoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RegistroIntentoAccesoIntegrationTests {

    @Autowired private RegistroIntentoAccesoService registro;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    private Usuario usuario;
    private String recurso;

    @BeforeEach
    void prepararUsuarioConfirmado() {
        String identificador = UUID.randomUUID().toString();
        recurso = "/incidencias-test/" + identificador;
        usuario = new Usuario();
        usuario.setUsername("audit-" + identificador.substring(0, 8));
        usuario.setEmail(identificador + "@test.com");
        usuario.setPasswordHash("hash-de-prueba");
        usuario = usuarios.saveAndFlush(usuario);
    }

    @AfterEach
    void limpiarFixtures() {
        jdbc.update("DELETE FROM intentos_acceso_protegido WHERE usuario_id = ? OR recurso = ?",
                usuario.getId(), recurso);
        usuarios.deleteById(usuario.getId());
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 403})
    void auditoriaSobreviveAlRollbackDelNegocio(int codigo) {
        Long proyectoId = new TransactionTemplate(transactionManager).execute(status -> {
            Long id = jdbc.queryForObject(
                    "INSERT INTO proyectos (nombre, creado_por) VALUES (?, ?) RETURNING id",
                    Long.class, "Proyecto revertido SS-77", usuario.getId());
            registro.registrar(usuario.getId(), recurso, "GET", codigo == 200, codigo);
            status.setRollbackOnly();
            return id;
        });

        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM proyectos WHERE id = ?", Integer.class, proyectoId));
        verificarRegistro(recurso, usuario.getId(), codigo);
    }

    @Test
    void auditoriaSobreviveAUnaExcepcionDelNegocio() {
        assertThrows(IllegalStateException.class, () ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    registro.registrar(usuario.getId(), recurso, "GET", false, 403);
                    throw new IllegalStateException("Fallo posterior a la auditoría");
                }));

        verificarRegistro(recurso, usuario.getId(), 403);
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 403, 401})
    void filtroRegistraElResultadoHttpAunqueSeReviertaLaTransaccion(int codigo) {
        String ruta = switch (codigo) {
            case 200 -> "/proyectos";
            case 403 -> "/proyectos/" + Long.MAX_VALUE;
            default -> recurso;
        };

        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            try {
                var peticion = get(ruta);
                if (codigo != 401) {
                    peticion.header("Authorization", "Bearer " + jwtService.generateAccessToken(usuario));
                }
                var resultado = mockMvc.perform(peticion).andExpect(status().is(codigo));
                if (codigo == 403) {
                    resultado.andExpect(content().string("Acceso denegado"));
                }
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            transaction.setRollbackOnly();
        });

        verificarRegistro(ruta, codigo == 401 ? null : usuario.getId(), codigo);
    }

    private void verificarRegistro(String ruta, Long usuarioId, int codigo) {
        var registros = jdbc.queryForList(
                "SELECT usuario_id, operacion, autorizado, codigo_respuesta, fecha_hora "
                        + "FROM intentos_acceso_protegido WHERE recurso = ? "
                        + "AND usuario_id IS NOT DISTINCT FROM ?", ruta, usuarioId);
        assertEquals(1, registros.size());
        var intento = registros.getFirst();
        assertEquals(usuarioId, intento.get("usuario_id"));
        assertEquals("GET", intento.get("operacion"));
        assertEquals(codigo == 200, intento.get("autorizado"));
        assertEquals(codigo, intento.get("codigo_respuesta"));
        assertNotNull(intento.get("fecha_hora"));
    }
}
