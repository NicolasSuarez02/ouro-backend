package com.ouro.service;

import com.ouro.dto.ResourceDTO;
import com.ouro.entity.Resource;
import com.ouro.entity.User;
import com.ouro.repository.ResourceRepository;
import com.ouro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Autowired
    public ResourceService(ResourceRepository resourceRepository,
                           UserRepository userRepository,
                           StorageService storageService) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    /**
     * Un terapeuta sube un archivo. Queda en PENDING hasta que un admin lo apruebe.
     */
    @Transactional
    public ResourceDTO.ResourceResponse subirArchivo(MultipartFile archivo, ResourceDTO.UploadResourceRequest request) {
        User uploader = userRepository.findById(request.getUploadedByUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + request.getUploadedByUserId()));

        if (!uploader.getEmailVerified()) {
            throw new RuntimeException("Debe verificar su email antes de subir archivos");
        }

        if (uploader.getRole() != User.Role.THERAPIST && uploader.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Solo los terapeutas pueden subir archivos");
        }

        if (archivo.isEmpty()) {
            throw new RuntimeException("El archivo está vacío");
        }

        String nombreAlmacenado = storageService.guardar(archivo, request.getCategory());

        Resource resource = new Resource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setCategory(request.getCategory());
        resource.setOriginalFileName(archivo.getOriginalFilename());
        resource.setStoredFileName(nombreAlmacenado);
        resource.setFilePath(storageService.getRutaRelativa(request.getCategory(), nombreAlmacenado));
        resource.setFileSize(archivo.getSize());
        resource.setMimeType(archivo.getContentType());
        resource.setUploadedBy(uploader);
        resource.setApprovalStatus(Resource.ApprovalStatus.PENDING);

        return new ResourceDTO.ResourceResponse(resourceRepository.save(resource));
    }

    /**
     * Listado público de recursos aprobados, filtrado por categoría.
     * Requiere usuario logueado.
     */
    @Transactional(readOnly = true)
    public List<ResourceDTO.ResourceResponse> listarAprobados(Resource.ResourceCategory categoria, Integer requestingUserId) {
        verificarUsuarioLogueado(requestingUserId);
        return resourceRepository
                .findByCategoryAndApprovalStatus(categoria, Resource.ApprovalStatus.APPROVED)
                .stream()
                .map(ResourceDTO.ResourceResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Recursos pendientes de aprobación. Solo admins.
     */
    @Transactional(readOnly = true)
    public List<ResourceDTO.ResourceResponse> listarPendientes(Integer adminUserId) {
        verificarAdmin(adminUserId);
        return resourceRepository
                .findByApprovalStatus(Resource.ApprovalStatus.PENDING)
                .stream()
                .map(ResourceDTO.ResourceResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Admin aprueba un recurso.
     */
    @Transactional
    public ResourceDTO.ResourceResponse aprobar(Integer resourceId, Integer adminUserId) {
        User admin = verificarAdmin(adminUserId);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado con id: " + resourceId));

        resource.setApprovalStatus(Resource.ApprovalStatus.APPROVED);
        resource.setApprovedBy(admin);
        return new ResourceDTO.ResourceResponse(resourceRepository.save(resource));
    }

    /**
     * Admin rechaza un recurso.
     */
    @Transactional
    public ResourceDTO.ResourceResponse rechazar(Integer resourceId, Integer adminUserId) {
        User admin = verificarAdmin(adminUserId);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado con id: " + resourceId));

        resource.setApprovalStatus(Resource.ApprovalStatus.REJECTED);
        resource.setApprovedBy(admin);
        return new ResourceDTO.ResourceResponse(resourceRepository.save(resource));
    }

    /**
     * Retorna el archivo para descarga. Requiere usuario logueado.
     */
    @Transactional(readOnly = true)
    public Resource obtenerParaDescarga(Integer resourceId, Integer requestingUserId) {
        verificarUsuarioLogueado(requestingUserId);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado con id: " + resourceId));

        if (resource.getApprovalStatus() != Resource.ApprovalStatus.APPROVED) {
            throw new RuntimeException("El recurso no está disponible");
        }

        return resource;
    }

    /**
     * Elimina un recurso. Solo el dueño o un admin.
     */
    @Transactional
    public void eliminar(Integer resourceId, Integer requestingUserId) {
        User requesting = verificarUsuarioLogueado(requestingUserId);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado con id: " + resourceId));

        boolean esDueno = resource.getUploadedBy().getId().equals(requestingUserId);
        boolean esAdmin = requesting.getRole() == User.Role.ADMIN;

        if (!esDueno && !esAdmin) {
            throw new RuntimeException("No tenés permiso para eliminar este recurso");
        }

        storageService.eliminar(resource.getCategory().name().toLowerCase(), resource.getStoredFileName());
        resourceRepository.deleteById(resourceId);
    }

    public org.springframework.core.io.Resource cargarArchivo(Resource resource) {
        return storageService.cargar(resource.getCategory().name().toLowerCase(), resource.getStoredFileName());
    }

    private User verificarAdmin(Integer adminUserId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + adminUserId));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Acceso denegado: se requiere rol ADMIN");
        }
        return admin;
    }

    private User verificarUsuarioLogueado(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));
    }
}
