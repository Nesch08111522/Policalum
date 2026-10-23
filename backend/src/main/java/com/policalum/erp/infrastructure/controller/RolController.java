package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import  java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.policalum.erp.application.dto.request.RolRequestDTO;
import com.policalum.erp.application.dto.response.RolResponseDTO;
import com.policalum.erp.application.service.RolService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Administración")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Controller
@PreAuthorize("hasRole('ADMIN')")
public class RolController {

    private final RolService rolService;

    @GetMapping
    public ResponseEntity <List<RolResponseDTO>> getAll(){
        return ResponseEntity.ok(rolService.getRoles());
    }


    @PostMapping
    public ResponseEntity<RolResponseDTO> createRol(@RequestBody @Valid RolRequestDTO rolRequestDTO){
        RolResponseDTO rolResponseDTO = rolService.createRol(rolRequestDTO);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(rolResponseDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolResponseDTO> getRol(@PathVariable UUID id){
       return ResponseEntity.ok(rolService.getRolById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RolResponseDTO> updateRol(@PathVariable UUID id, @RequestBody @Valid RolRequestDTO rolRequestDTO){
        return ResponseEntity.ok(rolService.updateRol(id, rolRequestDTO));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) //devuelve sin cuertpo (204)
    public void deleteRol(@PathVariable UUID id){
        rolService.deleteRol(id);

}
}