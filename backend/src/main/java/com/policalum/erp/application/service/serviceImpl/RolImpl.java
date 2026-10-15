package com.policalum.erp.application.service.serviceImpl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.policalum.erp.application.dto.request.RolRequestDTO;
import com.policalum.erp.application.dto.response.RolResponseDTO;
import com.policalum.erp.application.mapper.RolMapper;
import com.policalum.erp.application.service.RolService;
import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.infrastructure.repository.RolRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
@Transactional
@RequiredArgsConstructor
@Service

public class RolImpl implements RolService {
    
    private final RolRepository rolRepository;
    private final RolMapper rolMapper;

    @Transactional
    @Override
    public RolResponseDTO createRol(RolRequestDTO rolRequestDTO) {
       Rol rol = rolMapper.toEntity(rolRequestDTO);
        Rol rolGuardado = rolRepository.save(rol);
       return rolMapper.toResponseDto(rolGuardado);
    }


    @Override
    public List<RolResponseDTO> getRoles() {
        return rolRepository.findAll().stream()
                .map(rolMapper::toResponseDto)
                .collect(Collectors.toList());
    }


    
    @Transactional
    @Override
    public RolResponseDTO updateRol(UUID id, RolRequestDTO rolRequestDTO) {
        Rol rolExistente = rolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + id));

        rolMapper.updateEntityFromDto(rolRequestDTO, rolExistente);

        Rol rolActualizado = rolRepository.save(rolExistente);
        return rolMapper.toResponseDto(rolActualizado);
    }

    @Transactional
    @Override
    public void deleteRol(UUID id) {
        if(!rolRepository.existsById(id)) {
            throw new RuntimeException("Rol no encontrado con id: " + id);
        }
        rolRepository.deleteById(id);
    }

    @Override
    public RolResponseDTO getRolById (UUID id) {

     
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + id));

        return rolMapper.toResponseDto(rol);
    }

}
