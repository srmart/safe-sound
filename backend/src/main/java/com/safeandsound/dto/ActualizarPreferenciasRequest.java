package com.safeandsound.dto;

import jakarta.validation.constraints.NotNull;

public class ActualizarPreferenciasRequest {

    @NotNull(message = "La preferencia de notificaciones es obligatoria")
    private Boolean notificacionesHabilitadas;

    @NotNull(message = "La preferencia de modo oscuro es obligatoria")
    private Boolean modoOscuro;

    public Boolean getNotificacionesHabilitadas() {
        return notificacionesHabilitadas;
    }

    public void setNotificacionesHabilitadas(Boolean notificacionesHabilitadas) {
        this.notificacionesHabilitadas = notificacionesHabilitadas;
    }

    public Boolean getModoOscuro() {
        return modoOscuro;
    }

    public void setModoOscuro(Boolean modoOscuro) {
        this.modoOscuro = modoOscuro;
    }
}
