package com.policalum.erp.application.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.policalum.erp.application.dto.request.ClienteRequestDTO;
import com.policalum.erp.application.dto.response.ClienteResponseDTO;
import com.policalum.erp.application.mapper.ClienteMapper;
import com.policalum.erp.application.service.ClienteService;
import com.policalum.erp.domain.model.Cliente;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.ClienteRepository;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    private static final String ENTITY_NAME = "Cliente";
    private static final String FIELD_ID = "ID";
    private static final String FIELD_DOCUMENTO = "número de documento";

    @Override
    public ClienteResponseDTO create(ClienteRequestDTO requestDTO) {
        log.info("Creando nuevo cliente con documento: {}", requestDTO.numeroDocumento());

        // Validaciones de negocio
        validateClientCreation(requestDTO);

        Cliente cliente = clienteMapper.toEntity(requestDTO);
        if (cliente.getActivo() == null) {
            cliente.setActivo(true);
        }
        cliente = clienteRepository.save(cliente);

        log.info("Cliente creado exitosamente con ID: {}", cliente.getId());
        return clienteMapper.toResponseDto(cliente);
    }

    @Override
    public ClienteResponseDTO update(UUID id, ClienteRequestDTO requestDTO) {
        log.info("Actualizando cliente con ID: {}", id);

        Cliente cliente = findClienteById(id);
        validateClientUpdate(requestDTO, cliente);

        clienteMapper.updateEntityFromDto(requestDTO, cliente);
        cliente = clienteRepository.save(cliente);

        log.info("Cliente actualizado exitosamente: {}", cliente.getId());
        return clienteMapper.toResponseDto(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO findById(UUID id) {
        log.debug("Buscando cliente por ID: {}", id);
        Cliente cliente = findClienteById(id);
        return clienteMapper.toResponseDto(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO findByNumeroDocumento(String numeroDocumento) {
        log.debug("Buscando cliente por documento: {}", numeroDocumento);
        Cliente cliente = clienteRepository.findByNumeroDocumento(numeroDocumento)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("%s no encontrado con %s: %s", ENTITY_NAME, FIELD_DOCUMENTO, numeroDocumento)));
        return clienteMapper.toResponseDto(cliente);
    }

    @Override
    public ClienteResponseDTO delete(UUID id) {
        log.info("Eliminando cliente con ID: {}", id);

        Cliente cliente = findClienteById(id);
        if (clienteRepository.tieneHistorial(id)) {
            if (Boolean.FALSE.equals(cliente.getActivo())) {
                throw new ValidationException("El cliente tiene historial de ventas y permanece inactivo");
            }
            cliente.setActivo(false);
            log.info("Cliente desactivado para conservar el historial: {}", id);
            return clienteMapper.toResponseDto(clienteRepository.save(cliente));
        }

        clienteRepository.delete(cliente);
        log.info("Cliente eliminado porque no tenía historial: {}", id);
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> search(String searchTerm, Pageable pageable) {
        log.debug("Buscando clientes con término: '{}'", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAll(pageable);
        }

        return clienteRepository.searchByNombreOrDocumento(searchTerm.trim(), pageable)
                .map(clienteMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> findAll(Pageable pageable) {
        log.debug("Obteniendo todos los clientes paginados");
        return clienteRepository.findAll(pageable)
                .map(clienteMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> findByTipoDocumento(String tipoDocumento, Pageable pageable) {
        log.debug("Buscando clientes por tipo de documento: {}", tipoDocumento);
        return clienteRepository.findByTipoDocumento(tipoDocumento, pageable)
                .map(clienteMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> findTop10Recent() {
        log.debug("Obteniendo los 10 clientes más recientes");
        Pageable top10 = Pageable.ofSize(10);
        return clienteRepository.findTop10ByOrderByCreatedAtDesc(top10)
                .stream()
                .map(clienteMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> findClientesWithIncompleteData() {
        log.debug("Obteniendo clientes con datos incompletos");
        return clienteRepository.findClientesConDatosIncompletos()
                .stream()
                .map(clienteMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> findClientesWithPurchasesSince(LocalDateTime fechaDesde) {
        log.debug("Obteniendo clientes con compras desde: {}", fechaDesde);
        return clienteRepository.findClientesConComprasDesde(fechaDesde)
                .stream()
                .map(clienteMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> findClientesWithoutPurchasesSince(LocalDateTime fechaDesde) {
        log.debug("Obteniendo clientes sin compras desde: {}", fechaDesde);
        return clienteRepository.findClientesSinComprasDesde(fechaDesde)
                .stream()
                .map(clienteMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalClientes() {
        return clienteRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countClientesByDateRange(LocalDateTime start, LocalDateTime end) {
        return clienteRepository.countByCreatedAtBetween(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public long countClientesRegistradosHoy() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return countClientesByDateRange(startOfDay, endOfDay);
    }

    @Override
    @Transactional(readOnly = true)
    public long countClientesByTipoDocumento(String tipoDocumento) {
        Pageable pageable = Pageable.unpaged();
        return clienteRepository.findByTipoDocumento(tipoDocumento, pageable).getTotalElements();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getEstadisticasPorTipoDocumento() {
        List<Object[]> results = clienteRepository.countGroupByTipoDocumento();
        Map<String, Long> estadisticas = new HashMap<>();

        for (Object[] result : results) {
            String tipoDocumento = (String) result[0];
            Long count = ((Number) result[1]).longValue();
            estadisticas.put(tipoDocumento != null ? tipoDocumento : "SIN_TIPO", count);
        }

        return estadisticas;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalClientes", countTotalClientes());
        stats.put("clientesRegistradosHoy", countClientesRegistradosHoy());
        stats.put("distribucionPorTipo", getEstadisticasPorTipoDocumento());
        stats.put("clientesSinCompletar", findClientesWithIncompleteData().size());
        stats.put("clientesRecientes", findTop10Recent());

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNumeroDocumento(String numeroDocumento) {
        return clienteRepository.existsByNumeroDocumento(numeroDocumento);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return email != null && clienteRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateDocumentoUniqueness(String numeroDocumento, UUID excludeId) {
        if (excludeId != null) {
            return clienteRepository.findByNumeroDocumento(numeroDocumento)
                    .map(cliente -> cliente.getId().equals(excludeId))
                    .orElse(true);
        }
        return !clienteRepository.existsByNumeroDocumento(numeroDocumento);
    }

    // ============ PRIVATE METHODS ============

    private Cliente findClienteById(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("%s no encontrado con %s: %s", ENTITY_NAME, FIELD_ID, id)));
    }

    private void validateClientCreation(ClienteRequestDTO requestDTO) {
        // Validar documento único
        if (clienteRepository.existsByNumeroDocumento(requestDTO.numeroDocumento())) {
            throw new ValidationException(
                    String.format("Ya existe un cliente con el %s: %s", FIELD_DOCUMENTO, requestDTO.numeroDocumento()));
        }

        // Validar email único si existe
        if (requestDTO.email() != null && !requestDTO.email().isEmpty() &&
                clienteRepository.existsByEmail(requestDTO.email())) {
            throw new ValidationException(
                    String.format("Ya existe un cliente con el email: %s", requestDTO.email()));
        }
    }

    private void validateClientUpdate(ClienteRequestDTO requestDTO, Cliente existingCliente) {
        // Validar que el documento no esté siendo usado por otro cliente
        if (!existingCliente.getNumeroDocumento().equals(requestDTO.numeroDocumento()) &&
                clienteRepository.existsByNumeroDocumento(requestDTO.numeroDocumento())) {
            throw new ValidationException(
                    String.format("El %s ya está registrado por otro cliente", FIELD_DOCUMENTO));
        }

        // Validar email único si cambia y no está vacío
        if (requestDTO.email() != null && !requestDTO.email().isEmpty() &&
                !existingCliente.getEmail().equals(requestDTO.email()) &&
                clienteRepository.existsByEmail(requestDTO.email())) {
            throw new ValidationException(
                    String.format("El email %s ya está registrado por otro cliente", requestDTO.email()));
        }
    }
}