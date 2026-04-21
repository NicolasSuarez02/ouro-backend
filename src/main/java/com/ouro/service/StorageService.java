package com.ouro.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ouro.entity.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class StorageService {

    private final Cloudinary cloudinary;

    public StorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Sube un archivo de recurso a Cloudinary.
     * Retorna el public_id para gestión posterior (eliminación).
     */
    public String guardar(MultipartFile archivo, Resource.ResourceCategory categoria) {
        try {
            Map resultado = cloudinary.uploader().upload(archivo.getBytes(),
                ObjectUtils.asMap(
                    "folder", "ouro/" + categoria.name().toLowerCase(),
                    "resource_type", "auto"
                ));
            return (String) resultado.get("public_id");
        } catch (IOException e) {
            throw new RuntimeException("No se pudo subir el archivo a Cloudinary", e);
        }
    }

    /**
     * Sube una foto de perfil de terapeuta a Cloudinary.
     * Retorna la URL segura directamente para guardar en DB.
     */
    public String guardarFoto(MultipartFile foto) {
        try {
            Map resultado = cloudinary.uploader().upload(foto.getBytes(),
                ObjectUtils.asMap(
                    "folder", "ouro/photos",
                    "resource_type", "image"
                ));
            return (String) resultado.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("No se pudo subir la foto a Cloudinary", e);
        }
    }

    /**
     * Retorna la URL pública de un archivo por su public_id.
     * Usado por ResourceService para guardar filePath en DB.
     */
    public String getRutaRelativa(Resource.ResourceCategory categoria, String publicId) {
        return cloudinary.url().secure(true).resourceType("auto").generate(publicId);
    }

    /**
     * Elimina un archivo de Cloudinary.
     * Usa el mimeType para determinar el resource_type correcto.
     */
    public void eliminar(String publicId, String mimeType) {
        String resourceType = resolverResourceType(mimeType);
        try {
            cloudinary.uploader().destroy(publicId,
                ObjectUtils.asMap("resource_type", resourceType));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo eliminar el archivo de Cloudinary: " + publicId, e);
        }
    }

    private String resolverResourceType(String mimeType) {
        if (mimeType == null) return "raw";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        return "raw";
    }
}
