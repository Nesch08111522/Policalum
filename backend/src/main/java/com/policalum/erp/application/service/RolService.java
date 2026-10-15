package com.policalum.erp.application.service;
import java.util.List;
import java.util.UUID;

import com.policalum.erp.application.dto.request.RolRequestDTO;
import com.policalum.erp.application.dto.response.RolResponseDTO;
public interface RolService {

public RolResponseDTO  createRol(RolRequestDTO rolRequestDTO);

public List<RolResponseDTO> getRoles();

public RolResponseDTO updateRol(UUID id, RolRequestDTO rolRequestDTO);

void deleteRol(UUID id);


public RolResponseDTO getRolById(UUID id);
}
