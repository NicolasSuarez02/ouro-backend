package com.ouro.repository;

import com.ouro.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {
    
    Optional<Client> findByUserId(Integer userId);
    
    boolean existsByUserId(Integer userId);
}
