package com.ouro.controller;

import com.ouro.dto.EmailDTO;
import com.ouro.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*")
public class EmailController {
    
    private final EmailService emailService;
    
    @Autowired
    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @PostMapping("/send")
    public ResponseEntity<EmailDTO.EmailResponse> sendEmail(
            @Valid @RequestBody EmailDTO.SendEmailRequest request) {
        EmailDTO.EmailResponse response = emailService.sendEmail(request);
        
        if (response.isSuccess()) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/appointment-confirmation")
    public ResponseEntity<EmailDTO.EmailResponse> sendAppointmentConfirmation(
            @RequestParam String therapistEmail,
            @RequestParam String clientName,
            @RequestParam String appointmentDate,
            @RequestParam String appointmentTime,
            @RequestParam String zoomLink) {
        
        EmailDTO.EmailResponse response = emailService.sendAppointmentConfirmation(
                therapistEmail, clientName, appointmentDate, appointmentTime, zoomLink);
        
        if (response.isSuccess()) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/appointment-reminder")
    public ResponseEntity<EmailDTO.EmailResponse> sendAppointmentReminder(
            @RequestParam String recipientEmail,
            @RequestParam String recipientName,
            @RequestParam String appointmentDate,
            @RequestParam String appointmentTime,
            @RequestParam String zoomLink) {
        
        EmailDTO.EmailResponse response = emailService.sendAppointmentReminder(
                recipientEmail, recipientName, appointmentDate, appointmentTime, zoomLink);
        
        if (response.isSuccess()) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
