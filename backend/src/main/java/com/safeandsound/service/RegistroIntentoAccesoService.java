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

    // SS-77: conservar la auditoría aunque la transacción de negocio se revierta.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
