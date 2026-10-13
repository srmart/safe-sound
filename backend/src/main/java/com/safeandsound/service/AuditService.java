package com.safeandsound.service;

import com.safeandsound.model.RegistroAuditoria;
import com.safeandsound.repository.RegistroAuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AuditService {

    private final RegistroAuditoriaRepository registroAuditoriaRepository;

    public AuditService(RegistroAuditoriaRepository registroAuditoriaRepository) {
        this.registroAuditoriaRepository = registroAuditoriaRepository;
    }

    @Transactional
    public void registrarAccionAdministrativa(Long usuarioId, Long proyectoId, String operacion, String detalles) {
        RegistroAuditoria registro = new RegistroAuditoria(usuarioId, proyectoId, operacion, detalles);
        registroAuditoriaRepository.save(registro);
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoria> obtenerAuditoriaProyecto(Long proyectoId) {
        return registroAuditoriaRepository.findByProyectoIdOrderByFechaHoraDesc(proyectoId);
    }
}