// src/components/Files/UploadFileModal.tsx
import React, { useState } from 'react';

interface UploadFileModalProps {
  projectId: string;
  onClose: () => void;
  onUpload: (fileName: string, fileSize: string) => void;
}

export const UploadFileModal: React.FC<UploadFileModalProps> = ({ projectId, onClose, onUpload }) => {
  const [file, setFile] = useState<File | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (file) {
      // Simular tamaño en MB
      const sizeInMB = (file.size / (1024 * 1024)).toFixed(2) + ' MB';
      onUpload(file.name, sizeInMB);
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
        <h2 className="text-2xl font-bold mb-4 text-gray-800">Subir Archivo al Proyecto</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Seleccionar Archivo</label>
            <input
              type="file"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-green-500"
              required
            />
          </div>
          <div className="flex justify-end gap-3 mt-6">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-gray-700 bg-gray-200 rounded hover:bg-gray-300 transition"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-white bg-green-600 rounded hover:bg-green-700 transition"
            >
              Subir Archivo
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};