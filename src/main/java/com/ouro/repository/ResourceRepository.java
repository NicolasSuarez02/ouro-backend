package com.ouro.repository;

import com.ouro.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Integer> {

    List<Resource> findByCategoryAndApprovalStatus(Resource.ResourceCategory category, Resource.ApprovalStatus status);

    List<Resource> findByApprovalStatus(Resource.ApprovalStatus status);

    List<Resource> findByUploadedById(Integer userId);

    void deleteByUploadedById(Integer userId);
}
