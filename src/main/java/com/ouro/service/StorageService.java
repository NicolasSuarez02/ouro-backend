package com.ouro.service;

import com.ouro.entity.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${ouro.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Guarda el archivo en disco y retorna el nombre almacenado (UUID).
     * Cuando se migre a S3, solo se reemplaza este método.
     */
    public String guardar(MultipartFile archivo, Resource.ResourceCategory categoria) {
        String subcarpeta = categoria.name().toLowerCase();
        Path destino = Paths.get(uploadDir, subcarpeta);

        try {
            Files.createDirectories(destino);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de uploads: " + destino, e);
        }

        String extension = obtenerExtension(archivo.getOriginalFilename());
        String nombreAlmacenado = UUID.randomUUID().toString() + extension;
        Path rutaArchivo = destino.resolve(nombreAlmacenado);

        try {
            Files.copy(archivo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo: " + nombreAlmacenado, e);
        }

        return nombreAlmacenado;
    }

    /**
     * Carga el archivo del disco para ser descargado.
     */
    public org.springframework.core.io.Resource cargar(String subcarpeta, String nombreAlmacenado) {
        Path rutaArchivo = Paths.get(uploadDir, subcarpeta, nombreAlmacenado);
        try {
            org.springframework.core.io.Resource resource = new UrlResource(rutaArchivo.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Archivo no encontrado: " + nombreAlmacenado);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error al leer el archivo: " + nombreAlmacenado, e);
        }
    }

    /**
     * Elimina el archivo del disco.
     */
    public void eliminar(String subcarpeta, String nombreAlmacenado) {
        Path rutaArchivo = Paths.get(uploadDir, subcarpeta, nombreAlmacenado);
        try {
            Files.deleteIfExists(rutaArchivo);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo eliminar el archivo: " + nombreAlmacenado, e);
        }
    }

    public String getRutaRelativa(Resource.ResourceCategory categoria, String nombreAlmacenado) {
        return uploadDir + "/" + categoria.name().toLowerCase() + "/" + nombreAlmacenado;
    }

    /**
     * Guarda una foto de perfil de terapeuta en disco y retorna el nombre almacenado.
     */
    public String guardarFoto(MultipartFile archivo) {
        Path destino = Paths.get(uploadDir, "photos");
        try {
            Files.createDirectories(destino);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de fotos: " + destino, e);
        }

        String extension = obtenerExtension(archivo.getOriginalFilename());
        String nombreAlmacenado = UUID.randomUUID().toString() + extension;
        Path rutaArchivo = destino.resolve(nombreAlmacenado);

        try {
            Files.copy(archivo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la foto: " + nombreAlmacenado, e);
        }

        return nombreAlmacenado;
    }

    /**
     * Carga una foto del disco para ser servida.
     */
    public org.springframework.core.io.Resource cargarFoto(String nombreAlmacenado) {
        Path rutaArchivo = Paths.get(uploadDir, "photos", nombreAlmacenado);
        try {
            org.springframework.core.io.Resource resource = new UrlResource(rutaArchivo.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Foto no encontrada: " + nombreAlmacenado);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error al leer la foto: " + nombreAlmacenado, e);
        }
    }

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "";
        }
        return nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
    }
}
