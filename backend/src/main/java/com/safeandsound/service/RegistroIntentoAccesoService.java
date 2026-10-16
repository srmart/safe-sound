package com.safeandsound.service;

import com.safeandsound.model.IntentoAccesoProtegido;
import com.safeandsound.repository.IntentoAccesoProtegidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroIntentoAccesoService {

    private final IntentoAccesoProtegidoRepository intentoAccesoProtegidoRepository;

    public RegistroIntentoAccesoService(
            IntentoAccesoProtegidoRepository intentoAccesoProtegidoRepository
    ) {
        this.intentoAccesoProtegidoRepository = intentoAccesoProtegidoRepository;
    }

    @Transactional //(propagation = Propagation.REQUIRES_NEW) Se comenta para poder comprobar las pruebas sin conflicto entre transacciones paralelas. En funcionamiento normal para tener auditoria DESCOMENTAR
    public void registrar(
            Long usuarioId,
            String recurso,
            String operacion,
            boolean autorizado,
            int codigoRespuesta
    ) {
        intentoAccesoProtegidoRepository.save(new IntentoAccesoProtegido(
                usuarioId,
                recurso,
                operacion,
                autorizado,
                codigoRespuesta
        ));
    }
}
