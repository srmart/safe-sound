// src/pruebas/data.ts
import type { Project, ProjectFile } from '../types';

// Simulamos que el usuario logueado tiene diferentes roles en diferentes proyectos
export const mockProjects: Project[] = [
  {
    id: '1',
    name: 'Álbum Debut - Demo',
    description: 'Pruebas de sonido y mezclas iniciales',
    status: 'ACTIVE',
    myRole: 'OWNER', // Puede editar y archivar
  },
  {
    id: '2',
    name: 'Colaboración con Banda X',
    description: 'Stems de batería y guitarra',
    status: 'ACTIVE',
    myRole: 'COLLABORATOR', // Puede subir/bajar archivos, pero NO archivar el proyecto
  },
  {
    id: '3',
    name: 'Proyecto Archivado 2023',
    description: 'Material de referencia',
    status: 'ARCHIVED', // Baja lógica
    myRole: 'VIEWER', // Solo puede ver y descargar
  },
];

export const mockFiles: ProjectFile[] = [
  {
    id: 'f1',
    projectId: '1',
    fileName: 'bateria_v3.wav',
    fileSize: '15.2 MB',
    uploadedBy: 'Martín Bentura',
    uploadDate: '2026-09-28',
  },
  {
    id: 'f2',
    projectId: '1',
    fileName: 'mezcla_master_v1.mp3',
    fileSize: '4.8 MB',
    uploadedBy: 'Agustina Pereyra',
    uploadDate: '2026-09-29',
  },
];