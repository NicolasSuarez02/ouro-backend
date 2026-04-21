package com.ouro.controller;

import com.ouro.dto.ClientDTO;
import com.ouro.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
public class ClientController {
    
    private final ClientService clientService;
    
    @Autowired
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }
    
    @PostMapping
    public ResponseEntity<ClientDTO.ClientResponse> createClient(
            @Valid @RequestBody ClientDTO.CreateClientRequest request) {
        try {
            ClientDTO.ClientResponse response = clientService.createClient(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO.ClientResponse> getClientById(@PathVariable Integer id) {
        try {
            ClientDTO.ClientResponse response = clientService.getClientById(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ClientDTO.ClientResponse> getClientByUserId(@PathVariable Integer userId) {
        try {
            ClientDTO.ClientResponse response = clientService.getClientByUserId(userId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }
        
    /** Listado paginado de clientes — solo ADMIN. */
    @GetMapping
    public ResponseEntity<Object> getAllClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Integer adminUserId = currentUserId();
            ClientDTO.ClientPageResponse response =
                    clientService.getAllClientsPaginated(adminUserId, page, size);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO.ClientResponse> updateClient(
            @PathVariable Integer id,
            @Valid @RequestBody ClientDTO.UpdateClientRequest request) {
        try {
            ClientDTO.ClientResponse response = clientService.updateClient(id, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Integer id) {
        try {
            clientService.deleteClient(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
