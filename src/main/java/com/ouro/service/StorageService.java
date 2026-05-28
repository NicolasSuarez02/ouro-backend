package com.ouro.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ouro.entity.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class StorageService {

    private final Cloudinary cloudinary;

    public StorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Sube un archivo de recurso a Cloudinary.
     * Retorna map con "publicId" (para borrar) y "secureUrl" (para guardar en DB).
     */
    public Map<String, String> save(MultipartFile file, Resource.ResourceCategory category) {
        try {
            Map result = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "folder", "ouro/" + category.name().toLowerCase(),
                    "resource_type", "auto"
                ));
            Map<String, String> uploadResult = new HashMap<>();
            uploadResult.put("publicId", (String) result.get("public_id"));
            uploadResult.put("secureUrl", (String) result.get("secure_url"));
            return uploadResult;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo subir el archivo a Cloudinary", e);
        }
    }

    /**
     * Sube una foto de perfil de terapeuta a Cloudinary.
     * Retorna la URL segura directamente para guardar en DB.
     */
    public String savePhoto(MultipartFile photo) {
        try {
            Map result = cloudinary.uploader().upload(photo.getBytes(),
                ObjectUtils.asMap(
                    "folder", "ouro/photos",
                    "resource_type", "image"
                ));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("No se pudo subir la foto a Cloudinary", e);
        }
    }

    /**
     * Genera una URL de descarga privada via api.cloudinary.com (no CDN).
     * Usa API key + firma → funciona sin importar restricciones de acceso de la cuenta.
     */
    public String getPrivateDownloadUrl(String publicId, String originalFileName, String storedFileUrl) {
        String resourceType = extractSegment(storedFileUrl, 1, "raw");
        String format = extractFormat(originalFileName);
        try {
            return cloudinary.privateDownload(publicId, format,
                    ObjectUtils.asMap("resource_type", resourceType));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar URL de descarga: " + e.getMessage(), e);
        }
    }

    public String getRelativePath(Resource.ResourceCategory category, String publicId, String mimeType) {
        return cloudinary.url().secure(true).resourceType(resolveResourceType(mimeType)).generate(publicId);
    }

    /**
     * Elimina un archivo de Cloudinary.
     * Usa el mimeType para determinar el resource_type correcto.
     */
    public void delete(String publicId, String mimeType) {
        String resourceType = resolveResourceType(mimeType);
        try {
            cloudinary.uploader().destroy(publicId,
                ObjectUtils.asMap("resource_type", resourceType));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo eliminar el archivo de Cloudinary: " + publicId, e);
        }
    }

    private String resolveResourceType(String mimeType) {
        if (mimeType == null) return "raw";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        return "raw";
    }

    // Extrae segmento de una URL Cloudinary: https://res.cloudinary.com/{cloud}/{seg1}/{seg2}/...
    private String extractSegment(String url, int index, String fallback) {
        if (url == null) return fallback;
        try {
            String path = url.replace("https://res.cloudinary.com/", "");
            String[] segments = path.split("/");
            return (segments.length > index) ? segments[index] : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private String extractFormat(String originalFileName) {
        if (originalFileName == null) return null;
        int lastDot = originalFileName.lastIndexOf('.');
        return (lastDot >= 0) ? originalFileName.substring(lastDot + 1).toLowerCase() : null;
    }
}
