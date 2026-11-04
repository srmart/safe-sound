
package com.safeandsound.service;

import com.safeandsound.exception.CredencialesInvalidasException;
import com.safeandsound.exception.LimiteIntentosExcedidoException;
import com.safeandsound.model.ControlIntentosAutenticacion;
import com.safeandsound.model.ControlIntentosAutenticacion.TipoIntento;
import com.safeandsound.repository.ControlIntentosAutenticacionRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.function.Supplier;

@Service
public class ProteccionFuerzaBrutaService {

    private static final int MAX_INTENTOS = 5;
    private static final long DURACION_BLOQUEO_SEGUNDOS = 900;

    private final ControlIntentosAutenticacionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public ProteccionFuerzaBrutaService(
            ControlIntentosAutenticacionRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public <T> Resultado<T> ejecutar(
            String identificador,
            TipoIntento tipo,
            Supplier<T> autenticacion
    ) {
        String clave = tipo.name() + ":" + identificador;

        // Serializa las solicitudes del mismo identificador,
        // incluso cuando todavía no existe una fila.
        entityManager.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(hashtextextended(:clave, 0))"
                )
                .setParameter("clave", clave)
                .getSingleResult();

        // Crear el registro si todavía no existe.
        entityManager.createNativeQuery("""
                INSERT INTO control_intentos_autenticacion
                (identificador, tipo, intentos_fallidos, ultimo_intento)
                VALUES (:identificador, :tipo, 0, CURRENT_TIMESTAMP)
                ON CONFLICT (identificador, tipo) DO NOTHING
                """)
                .setParameter("identificador", identificador)
                .setParameter("tipo", tipo.name())
                .executeUpdate();

        ControlIntentosAutenticacion control =
                repository.buscarParaActualizar(identificador, tipo)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No se pudo obtener el control de autenticación"
                                )
                        );

        Instant ahora = Instant.now();

        if (control.getBloqueadoHasta() != null
                && !control.getBloqueadoHasta().isAfter(ahora)) {
            control.reiniciar(ahora);
        }

        if (control.estaBloqueado(ahora)) {
            throw new LimiteIntentosExcedidoException(
                    control.getBloqueadoHasta()
            );
        }

        try {
            T resultado = autenticacion.get();

            control.reiniciar(ahora);
            return Resultado.exitoso(resultado);

        } catch (CredencialesInvalidasException ex) {

            control.registrarFallo(ahora);
            return Resultado.fallido();
        }
    }

    public static class Resultado<T> {

        private final T valor;
        private final boolean exitoso;

        private Resultado(T valor, boolean exitoso) {
            this.valor = valor;
            this.exitoso = exitoso;
        }

        public static <T> Resultado<T> exitoso(T valor) {
            return new Resultado<>(valor, true);
        }

        public static <T> Resultado<T> fallido() {
            return new Resultado<>(null, false);
        }

        public T getValor() {
            return valor;
        }

        public boolean isExitoso() {
            return exitoso;
        }
    }
}
