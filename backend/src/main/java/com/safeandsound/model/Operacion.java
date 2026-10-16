package com.safeandsound.model;

public enum Operacion {

    // Operaciones de proyecto
    VER_PROYECTO,
    MODIFICAR_PROYECTO,
    ARCHIVAR_PROYECTO,

    // Autorización a nivel de campo
    MODIFICAR_NOMBRE_PROYECTO,
    MODIFICAR_DESCRIPCION_PROYECTO,

    // Operaciones de miembros
    VER_MIEMBROS,
    AGREGAR_MIEMBRO,
    ELIMINAR_MIEMBRO,
    MODIFICAR_ROLES,

    // Operaciones de archivos
    VER_ARCHIVOS,
    SUBIR_ARCHIVO,
    DESCARGAR_ARCHIVO,
    ELIMINAR_ARCHIVO,

    // Operaciones de restauración JIT
    SOLICITAR_RESTAURACION,
    APROBAR_SOLICITUD_RESTAURACION,
    RECHAZAR_SOLICITUD_RESTAURACION,
    RESTAURAR_ARCHIVO,

    // Operaciones administrativas (RS19)
    VER_AUDITORIA
}
