package com.ouro.service;

import com.ouro.dto.ClientDTO;
import com.ouro.entity.Client;
import com.ouro.entity.User;
import com.ouro.repository.ClientRepository;
import com.ouro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientService {
    
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public ClientService(ClientRepository clientRepository, UserRepository userRepository) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }
    
    @Transactional
    public ClientDTO.ClientResponse createClient(ClientDTO.CreateClientRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (clientRepository.existsByUserId(request.getUserId())) {
            throw new RuntimeException("Ya tienes un perfil de cliente");
        }
        
        Client client = new Client(user);
        
        if (request.getDateOfBirth() != null) {
            try {
                client.setDateOfBirth(Timestamp.valueOf(request.getDateOfBirth()));
            } catch (Exception e) {
                throw new RuntimeException("Formato de fecha inválido. Use: yyyy-MM-dd HH:mm:ss");
            }
        }
        
        if (request.getTimeOfBirth() != null) {
            try {
                client.setTimeOfBirth(LocalTime.parse(request.getTimeOfBirth()));
            } catch (Exception e) {
                throw new RuntimeException("Formato de hora inválido. Use: HH:mm:ss");
            }
        }
        
        return new ClientDTO.ClientResponse(clientRepository.save(client));
    }
    
    @Transactional(readOnly = true)
    public ClientDTO.ClientResponse getClientById(Integer id) {
        return new ClientDTO.ClientResponse(
            clientRepository.findById(id).orElseThrow(() -> new RuntimeException("Cliente no encontrado"))
        );
    }

    @Transactional(readOnly = true)
    public ClientDTO.ClientResponse getClientByUserId(Integer userId) {
        return new ClientDTO.ClientResponse(
            clientRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Cliente no encontrado"))
        );
    }
    
    @Transactional(readOnly = true)
    public List<ClientDTO.ClientResponse> getAllClients() {
        return clientRepository.findAll().stream()
                .map(ClientDTO.ClientResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientDTO.ClientPageResponse getAllClientsPaginated(Integer requestingUserId, int page, int size) {
        User requesting = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (requesting.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Solo un administrador puede listar todos los clientes");
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Client> pageResult = clientRepository.findAll(pageable);

        List<ClientDTO.ClientResponse> content = pageResult.getContent().stream()
                .map(ClientDTO.ClientResponse::new)
                .collect(Collectors.toList());

        return new ClientDTO.ClientPageResponse(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
    
    @Transactional
    public ClientDTO.ClientResponse updateClient(Integer id, ClientDTO.UpdateClientRequest request) {
        Client c = clientRepository.findById(id).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        
        if (request.getDateOfBirth() != null) {
            try {
                c.setDateOfBirth(Timestamp.valueOf(request.getDateOfBirth()));
            } catch (Exception e) {
                throw new RuntimeException("Formato de fecha inválido");
            }
        }
        
        if (request.getTimeOfBirth() != null) {
            try {
                c.setTimeOfBirth(LocalTime.parse(request.getTimeOfBirth()));
            } catch (Exception e) {
                throw new RuntimeException("Formato de hora inválido");
            }
        }
        
        return new ClientDTO.ClientResponse(clientRepository.save(c));
    }
    
    @Transactional
    public void deleteClient(Integer id) {
        clientRepository.deleteById(id);
    }
}
