package com.safeandsound.model;

public enum Operacion {
    // Operaciones de proyecto
    VER_PROYECTO,
    MODIFICAR_PROYECTO,
    ARCHIVAR_PROYECTO,
    
    // Operaciones de miembros
    VER_MIEMBROS,
    AGREGAR_MIEMBRO,      // <-- AGREGADO PARA RS20
    ELIMINAR_MIEMBRO,     // <-- AGREGADO PARA RS20
    MODIFICAR_ROLES,
    
    // Operaciones de archivos
    VER_ARCHIVOS,         // <-- AGREGADO PARA RS20
    SUBIR_ARCHIVO,        // <-- AGREGADO PARA RS20
    DESCARGAR_ARCHIVO,    // <-- AGREGADO PARA RS20
    ELIMINAR_ARCHIVO      // <-- AGREGADO PARA RS20
}