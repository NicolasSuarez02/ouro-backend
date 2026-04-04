package com.ouro.controller;

import com.ouro.dto.ClientDTO;
import com.ouro.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        
    @GetMapping
    public ResponseEntity<List<ClientDTO.ClientResponse>> getAllClients() {
        List<ClientDTO.ClientResponse> clients = clientService.getAllClients();
        return new ResponseEntity<>(clients, HttpStatus.OK);
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
}
