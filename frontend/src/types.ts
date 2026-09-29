// src/types.ts
export type ProjectRole = 'OWNER' | 'COLLABORATOR' | 'VIEWER';
export type ProjectStatus = 'ACTIVE' | 'ARCHIVED';

export interface Project {
  id: string;
  name: string;
  description: string;
  status: ProjectStatus;
  myRole: ProjectRole; // Simula el rol del usuario logueado en ESTE proyecto
}

export interface ProjectFile {
  id: string;
  projectId: string;
  fileName: string;
  fileSize: string; // ej: "2.5 MB"
  uploadedBy: string;
  uploadDate: string;
}