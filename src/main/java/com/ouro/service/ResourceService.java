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
    public ResourceDTO.ResourceResponse uploadFile(MultipartFile file, ResourceDTO.UploadResourceRequest request) {
        User uploader = userRepository.findById(request.getUploadedByUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + request.getUploadedByUserId()));

        if (!uploader.getEmailVerified()) {
            throw new RuntimeException("Debe verificar su email antes de subir archivos");
        }

        if (uploader.getRole() != User.Role.THERAPIST && uploader.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Solo los terapeutas pueden subir archivos");
        }

        if (file.isEmpty()) {
            throw new RuntimeException("El archivo está vacío");
        }

        String storedName = storageService.save(file, request.getCategory());

        Resource resource = new Resource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setCategory(request.getCategory());
        resource.setOriginalFileName(file.getOriginalFilename());
        resource.setStoredFileName(storedName);
        resource.setFilePath(storageService.getRelativePath(request.getCategory(), storedName, file.getContentType()));
        resource.setFileSize(file.getSize());
        resource.setMimeType(file.getContentType());
        resource.setUploadedBy(uploader);
        resource.setApprovalStatus(Resource.ApprovalStatus.PENDING);

        return new ResourceDTO.ResourceResponse(resourceRepository.save(resource));
    }

    /**
     * Listado público de recursos aprobados, filtrado por categoría.
     * Requiere usuario logueado.
     */
    @Transactional(readOnly = true)
    public List<ResourceDTO.ResourceResponse> listApproved(Resource.ResourceCategory category, Integer requestingUserId) {
        verifyLoggedIn(requestingUserId);
        return resourceRepository
                .findByCategoryAndApprovalStatus(category, Resource.ApprovalStatus.APPROVED)
                .stream()
                .map(ResourceDTO.ResourceResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Recursos pendientes de aprobación. Solo admins.
     */
    @Transactional(readOnly = true)
    public List<ResourceDTO.ResourceResponse> listPending(Integer adminUserId) {
        verifyAdmin(adminUserId);
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
    public ResourceDTO.ResourceResponse approve(Integer resourceId, Integer adminUserId) {
        User admin = verifyAdmin(adminUserId);
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
    public ResourceDTO.ResourceResponse reject(Integer resourceId, Integer adminUserId) {
        User admin = verifyAdmin(adminUserId);
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
    public Resource getForDownload(Integer resourceId, Integer requestingUserId) {
        verifyLoggedIn(requestingUserId);
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
    public void delete(Integer resourceId, Integer requestingUserId) {
        User requesting = verifyLoggedIn(requestingUserId);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado con id: " + resourceId));

        boolean isOwner = resource.getUploadedBy().getId().equals(requestingUserId);
        boolean isAdmin = requesting.getRole() == User.Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("No tenés permiso para eliminar este recurso");
        }

        storageService.delete(resource.getStoredFileName(), resource.getMimeType());
        resourceRepository.deleteById(resourceId);
    }

    public String getDownloadUrl(Resource resource) {
        return storageService.getSecureUrl(resource.getStoredFileName(), resource.getMimeType());
    }

    private User verifyAdmin(Integer adminUserId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + adminUserId));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Acceso denegado: se requiere rol ADMIN");
        }
        return admin;
    }

    private User verifyLoggedIn(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));
    }
}
