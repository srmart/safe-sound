package com.safeandsound;

import com.safeandsound.model.Usuario;
import com.safeandsound.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/** Fixtures visibles para la FK de la auditoría independiente (SS-77). */
abstract class UsuariosConfirmadosIntegrationSupport {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<Long> usuariosCreados = new ArrayList<>();

    protected Usuario guardarUsuarioConfirmado(Usuario usuario) {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        transaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Usuario guardado = transaccion.execute(status -> usuarios.saveAndFlush(usuario));
        usuariosCreados.add(guardado.getId());
        return guardado;
    }

    @AfterTransaction
    void limpiarUsuariosConfirmados() {
        // Primero se revierte el negocio del test; luego se limpian solo sus fixtures.
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        transaccion.executeWithoutResult(status -> {
            for (Long id : usuariosCreados) {
                jdbc.update("DELETE FROM intentos_acceso_protegido WHERE usuario_id = ?", id);
                jdbc.update("DELETE FROM usuarios WHERE id = ?", id);
            }
        });
        usuariosCreados.clear();
    }
}
