package com.policalum.erp.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.policalum.erp.application.dto.request.UserRequestDTO;
import com.policalum.erp.application.dto.response.UserResponseDTO;


public interface UserService {

    UserResponseDTO createUser(UserRequestDTO userRequestDTO);

    List<UserResponseDTO> getUsers();

    Optional<UserResponseDTO> findById(UUID id);

    void DeleteUser(UUID id);

    UserResponseDTO updateUser(UUID id, UserRequestDTO userRequestDTO);
}
