package com.ouro.controller;

import com.ouro.dto.ResourceDTO;
import com.ouro.entity.Resource;
import com.ouro.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@CrossOrigin(origins = "*")
public class ResourceController {

    private final ResourceService resourceService;

    @Autowired
    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * Terapeuta sube un archivo — requiere auth. El userId viene del JWT.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> subirArchivo(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestPart("title") String title,
            @RequestPart("category") String category,
            @RequestPart(value = "description", required = false) String description) {
        try {
            Integer userId = currentUserId();
            ResourceDTO.UploadResourceRequest request = new ResourceDTO.UploadResourceRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setCategory(Resource.ResourceCategory.valueOf(category));
            request.setUploadedByUserId(userId);

            ResourceDTO.ResourceResponse response = resourceService.subirArchivo(archivo, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Categoría inválida. Valores permitidos: BIBLIOTECA, FORMACIONES");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Listado de recursos aprobados por categoría — requiere auth. El userId viene del JWT.
     */
    @GetMapping
    public ResponseEntity<Object> listarAprobados(
            @RequestParam Resource.ResourceCategory category) {
        try {
            Integer userId = currentUserId();
            List<ResourceDTO.ResourceResponse> resources = resourceService.listarAprobados(category, userId);
            return new ResponseEntity<>(resources, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Recursos pendientes de aprobación — solo admins.
     */
    @GetMapping("/pending")
    public ResponseEntity<Object> listarPendientes() {
        try {
            Integer adminUserId = currentUserId();
            List<ResourceDTO.ResourceResponse> resources = resourceService.listarPendientes(adminUserId);
            return new ResponseEntity<>(resources, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Admin aprueba un recurso.
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<Object> aprobar(@PathVariable Integer id) {
        try {
            Integer adminUserId = currentUserId();
            ResourceDTO.ResourceResponse response = resourceService.aprobar(id, adminUserId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Admin rechaza un recurso.
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<Object> rechazar(@PathVariable Integer id) {
        try {
            Integer adminUserId = currentUserId();
            ResourceDTO.ResourceResponse response = resourceService.rechazar(id, adminUserId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Descarga un archivo — requiere auth.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Object> descargar(@PathVariable Integer id) {
        try {
            Integer userId = currentUserId();
            Resource resource = resourceService.obtenerParaDescarga(id, userId);
            String url = resourceService.obtenerUrlDescarga(resource);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, url)
                    .build();
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Elimina un recurso — solo el dueño o un admin.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable Integer id) {
        try {
            Integer userId = currentUserId();
            resourceService.eliminar(id, userId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
