package com.ouro.dto;

import com.ouro.entity.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ResourceDTO {

    public static class UploadResourceRequest {
        @NotBlank(message = "El título es obligatorio")
        private String title;

        private String description;

        @NotNull(message = "La categoría es obligatoria")
        private Resource.ResourceCategory category;

        @NotNull(message = "El ID del usuario es obligatorio")
        private Integer uploadedByUserId;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Resource.ResourceCategory getCategory() {
            return category;
        }

        public void setCategory(Resource.ResourceCategory category) {
            this.category = category;
        }

        public Integer getUploadedByUserId() {
            return uploadedByUserId;
        }

        public void setUploadedByUserId(Integer uploadedByUserId) {
            this.uploadedByUserId = uploadedByUserId;
        }
    }

    public static class ResourceResponse {
        private Integer id;
        private String title;
        private String description;
        private Resource.ResourceCategory category;
        private String originalFileName;
        private Long fileSize;
        private String mimeType;
        private String uploadedByName;
        private Integer uploadedByUserId;
        private Resource.ApprovalStatus approvalStatus;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ResourceResponse(Resource resource) {
            this.id = resource.getId();
            this.title = resource.getTitle();
            this.description = resource.getDescription();
            this.category = resource.getCategory();
            this.originalFileName = resource.getOriginalFileName();
            this.fileSize = resource.getFileSize();
            this.mimeType = resource.getMimeType();
            this.uploadedByName = resource.getUploadedBy().getFullName();
            this.uploadedByUserId = resource.getUploadedBy().getId();
            this.approvalStatus = resource.getApprovalStatus();
            this.createdAt = resource.getCreatedAt();
            this.updatedAt = resource.getUpdatedAt();
        }

        public Integer getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public Resource.ResourceCategory getCategory() {
            return category;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getUploadedByName() {
            return uploadedByName;
        }

        public Integer getUploadedByUserId() {
            return uploadedByUserId;
        }

        public Resource.ApprovalStatus getApprovalStatus() {
            return approvalStatus;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }
    }
}
