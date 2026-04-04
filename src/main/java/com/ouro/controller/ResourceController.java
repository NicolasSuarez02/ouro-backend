package com.ouro.controller;

import com.ouro.dto.ResourceDTO;
import com.ouro.entity.Resource;
import com.ouro.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
     * Terapeuta sube un archivo.
     * Recibe multipart/form-data con el archivo + campos del request.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> subirArchivo(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestPart("title") String title,
            @RequestPart("category") String category,
            @RequestPart("uploadedByUserId") String uploadedByUserId,
            @RequestPart(value = "description", required = false) String description) {
        try {
            ResourceDTO.UploadResourceRequest request = new ResourceDTO.UploadResourceRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setCategory(Resource.ResourceCategory.valueOf(category));
            request.setUploadedByUserId(Integer.parseInt(uploadedByUserId));

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
     * Listado de recursos aprobados por categoría. Requiere usuario logueado.
     * GET /api/resources?category=BIBLIOTECA&userId=1
     */
    @GetMapping
    public ResponseEntity<Object> listarAprobados(
            @RequestParam Resource.ResourceCategory category,
            @RequestParam Integer userId) {
        try {
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
     * Recursos pendientes de aprobación. Solo admins.
     */
    @GetMapping("/pending")
    public ResponseEntity<Object> listarPendientes(@RequestParam Integer adminUserId) {
        try {
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
    public ResponseEntity<Object> aprobar(
            @PathVariable Integer id,
            @RequestParam Integer adminUserId) {
        try {
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
    public ResponseEntity<Object> rechazar(
            @PathVariable Integer id,
            @RequestParam Integer adminUserId) {
        try {
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
     * Descarga un archivo. Requiere usuario logueado.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Object> descargar(
            @PathVariable Integer id,
            @RequestParam Integer userId) {
        try {
            Resource resource = resourceService.obtenerParaDescarga(id, userId);
            org.springframework.core.io.Resource archivo = resourceService.cargarArchivo(resource);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(resource.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getOriginalFileName() + "\"")
                    .body(archivo);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Elimina un recurso. Solo el dueño o un admin.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(
            @PathVariable Integer id,
            @RequestParam Integer userId) {
        try {
            resourceService.eliminar(id, userId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }
}
