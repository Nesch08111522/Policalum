package com.policalum.erp.application.service.serviceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.policalum.erp.application.dto.request.UserRequestDTO;
import com.policalum.erp.application.dto.response.UserResponseDTO;
import com.policalum.erp.application.mapper.UserMapper;
import com.policalum.erp.application.service.UserService;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserImpl implements UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    @Override
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        // 1. Conversión limpia y directa de DTO a Entidad
        User user = userMapper.toEntity(userRequestDTO);

        // 2. La capa de servicio aplica la regla de seguridad
        String securedPassword = passwordEncoder.encode(userRequestDTO.passwordHash());
        user.setPasswordHash(securedPassword);

        user.setActivo(true);

        User savedUser = userRepository.save(user);
        return userMapper.toResponseDto(savedUser);
    }

    @Override
    public List<UserResponseDTO> getUsers() {
        return userRepository.findAll().stream().map(userMapper::toResponseDto).toList();
    }

    @Override
    public Optional<UserResponseDTO> findById(UUID id) {
        return userRepository.findById(id).stream().map(userMapper::toResponseDto).findFirst();
    }

    @Transactional
    @Override
    public UserResponseDTO updateUser(UUID id, UserRequestDTO dto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String currentPasswordHash = existingUser.getPasswordHash();

        userMapper.updateEntityFromDto(dto, existingUser);

        if (dto.passwordHash() != null && !dto.passwordHash().isBlank()) {
            existingUser.setPasswordHash(passwordEncoder.encode(dto.passwordHash()));
        } else {
            existingUser.setPasswordHash(currentPasswordHash);
        }

        User updatedUser = userRepository.save(existingUser);
        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    public void DeleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado con el id: " + id);
        }
        userRepository.deleteById(id);
    }

}
