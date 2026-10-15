package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.TurnoDelDiaDTO;
import com.policalum.erp.application.service.TurnoTrabajoService;
import com.policalum.erp.application.dto.request.AsistenciaQrDTO;
import com.policalum.erp.application.dto.request.RrhhAsistenciaRequestDTO;
import com.policalum.erp.application.dto.response.AsistenciaEstadoDTO;
import com.policalum.erp.application.dto.response.DashboardAsistenciaDTO;
import com.policalum.erp.application.dto.response.RrhhAsistenciaResponseDTO;
import com.policalum.erp.application.mapper.RrhhAsistenciaMapper;
import com.policalum.erp.application.service.RrhhAsistenciaService;
import com.policalum.erp.application.service.TotpService;
import com.policalum.erp.domain.enums.EstadoAsistencia;
import com.policalum.erp.domain.enums.TipoMarcacion;
import com.policalum.erp.domain.model.RrhhAsistencia;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.RrhhAsistenciaRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RrhhAsistenciaImpl implements RrhhAsistenciaService {

    private final RrhhAsistenciaRepository asistenciaRepository;
    private final UserRepository userRepository;
    private final TotpService totpService;
    private final RrhhAsistenciaMapper asistenciaMapper;
    private final TurnoTrabajoService turnoTrabajoService; // Módulo 8: tardanza según turno

    // ✅ SECRETO PÚBLICO PARA QR
    @Value("${totp.secret-key-publico}")
    private String secretKeyPublico;

    private static final int TOLERANCIA_MINUTOS = 10;
    private static final int HORA_ENTRADA = 8;
    private static final int HORA_SALIDA = 17;

    // ============================================
    // ✅ MARCACIÓN POR QR
    // ============================================

    @Override
    @Transactional
    public RrhhAsistenciaResponseDTO procesarMarcacionQr(AsistenciaQrDTO qrRequest) {

        String payload = qrRequest.payload();

        // ✅ Verificar si es QR público
        if (payload != null && payload.startsWith("POLICALUM|PUBLICO|")) {
            String[] parts = payload.split("\\|");
            if (parts.length != 4) {
                throw new IllegalArgumentException("QR inválido");
            }

            UUID empleadoId = qrRequest.empleadoId();
            if (empleadoId == null) {
                throw new IllegalArgumentException("ID de empleado no proporcionado");
            }

            long window = Long.parseLong(parts[2]);
            String codigo = parts[3];

            // ✅ VALIDAR CON EL SECRETO PÚBLICO
            boolean isValid = totpService.verify(secretKeyPublico, window, codigo);
            if (!isValid) {
                throw new IllegalArgumentException("Código QR inválido o expirado");
            }

            // ✅ BUSCAR AL EMPLEADO
            User usuario = userRepository.findById(empleadoId)
                    .orElseThrow(() -> new ResourceNotFoundException("El usuario no está registrado en el sistema."));

            if (!usuario.getActivo()) {
                throw new IllegalStateException("El usuario se encuentra inactivo.");
            }

            // ✅ LÓGICA DE NEGOCIO
            LocalDateTime ahora = LocalDateTime.now();
            TipoMarcacion tipo = determinarTipoMarcacion(empleadoId);
            validarMarcacion(empleadoId, tipo, ahora);

            // ✅ CREAR ASISTENCIA
            RrhhAsistencia asistencia = crearAsistencia(usuario, tipo, ahora, "Escaneo QR Móvil");
            RrhhAsistencia guardada = asistenciaRepository.save(asistencia);

            log.info("✅ Asistencia QR registrada: {} - {}", usuario.getEmail(), tipo);
            return asistenciaMapper.toResponseDto(guardada);
        }

        // ✅ Si es token Base64 (formato antiguo)
        UUID userIdDecodificado = totpService.validarTokenYObtenerEmpleadoId(payload);
        if (userIdDecodificado == null) {
            throw new IllegalArgumentException("El código QR es inválido o ha expirado.");
        }

        if (!userIdDecodificado.equals(qrRequest.empleadoId())) {
            throw new SecurityException("Fraude detectado: El código QR no corresponde al usuario especificado.");
        }

        User usuario = userRepository.findById(userIdDecodificado)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no está registrado en el sistema."));

        if (!usuario.getActivo()) {
            throw new IllegalStateException("El usuario se encuentra inactivo.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        TipoMarcacion tipo = determinarTipoMarcacion(userIdDecodificado);
        validarMarcacion(userIdDecodificado, tipo, ahora);

        RrhhAsistencia asistencia = crearAsistencia(usuario, tipo, ahora, "Escaneo QR Móvil");
        RrhhAsistencia guardada = asistenciaRepository.save(asistencia);

        return asistenciaMapper.toResponseDto(guardada);
    }

    // ============================================
    // ✅ MARCACIÓN MANUAL (ADMIN)
    // ============================================

    @Override
    @Transactional
    public RrhhAsistenciaResponseDTO procesarMarcacionManual(RrhhAsistenciaRequestDTO manualRequest) {
        User usuario = userRepository.findById(manualRequest.empleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no está registrado."));

        if (!usuario.getActivo()) {
            throw new IllegalStateException("El usuario se encuentra inactivo.");
        }

        LocalDateTime fechaHora = manualRequest.fechaHora();
        TipoMarcacion tipo = manualRequest.tipoMarcacion();

        if (fechaHora.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se puede registrar una asistencia en el futuro.");
        }

        validarMarcacion(usuario.getId(), tipo, fechaHora);

        RrhhAsistencia asistencia = crearAsistencia(usuario, tipo, fechaHora,
                manualRequest.dispositivoInfo() != null ? manualRequest.dispositivoInfo() : "Registro Manual por Admin");

        RrhhAsistencia guardada = asistenciaRepository.save(asistencia);
        log.info("✅ Asistencia manual registrada: {} - {}", usuario.getEmail(), tipo);

        return asistenciaMapper.toResponseDto(guardada);
    }

    // ============================================
    // ✅ MÉTODOS PRIVADOS
    // ============================================

    private TipoMarcacion determinarTipoMarcacion(UUID empleadoId) {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);

        boolean yaEntrada = asistenciaRepository.yaMarcoEntradaHoy(empleadoId, inicioDia, finDia);
        boolean yaSalida = asistenciaRepository.yaMarcoSalidaHoy(empleadoId, inicioDia, finDia);

        if (!yaEntrada) {
            return TipoMarcacion.ENTRADA;
        } else if (!yaSalida) {
            return TipoMarcacion.SALIDA;
        } else {
            log.info("⚠️ Empleado {} ya marcó entrada y salida hoy. Nueva entrada.", empleadoId);
            return TipoMarcacion.ENTRADA;
        }
    }

    private void validarMarcacion(UUID empleadoId, TipoMarcacion tipo, LocalDateTime fechaHora) {
        LocalDateTime inicioDia = fechaHora.toLocalDate().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);

        if (tipo == TipoMarcacion.ENTRADA) {
            boolean yaEntrada = asistenciaRepository.yaMarcoEntradaHoy(empleadoId, inicioDia, finDia);
            if (yaEntrada) {
                throw new IllegalStateException("❌ El empleado ya marcó ENTRADA hoy.");
            }
        } else if (tipo == TipoMarcacion.SALIDA) {
            boolean yaSalida = asistenciaRepository.yaMarcoSalidaHoy(empleadoId, inicioDia, finDia);
            if (yaSalida) {
                throw new IllegalStateException("❌ El empleado ya marcó SALIDA hoy.");
            }
            boolean yaEntrada = asistenciaRepository.yaMarcoEntradaHoy(empleadoId, inicioDia, finDia);
            if (!yaEntrada) {
                throw new IllegalStateException("❌ No se puede marcar SALIDA sin haber marcado ENTRADA primero.");
            }
        }
    }

    /**
     * Módulo 8: si el empleado tiene turno ese día, la tardanza se mide contra su hora de
     * inicio + tolerancia del turno. Si no tiene turno, se mantiene la regla anterior (08:00 + 10 min).
     */
    private EstadoAsistencia calcularEstado(TipoMarcacion tipo, LocalDateTime fechaHora, TurnoDelDiaDTO turno) {
        if (tipo == TipoMarcacion.ENTRADA && turno != null) {
            return turno.esTardanza(fechaHora.toLocalTime()) ? EstadoAsistencia.TARDANZA : EstadoAsistencia.PRESENTE;
        }
        if (tipo == TipoMarcacion.ENTRADA) {
            LocalDateTime horaEntradaEsperada = fechaHora.withHour(HORA_ENTRADA).withMinute(0).withSecond(0).withNano(0);
            if (fechaHora.isAfter(horaEntradaEsperada.plusMinutes(TOLERANCIA_MINUTOS))) {
                return EstadoAsistencia.TARDANZA;
            }
            return EstadoAsistencia.PRESENTE;
        }
        return EstadoAsistencia.PRESENTE;
    }

    private RrhhAsistencia crearAsistencia(User usuario, TipoMarcacion tipo, LocalDateTime fechaHora, String dispositivo) {
        RrhhAsistencia asistencia = new RrhhAsistencia();
        asistencia.setEmpleado(usuario);
        asistencia.setFechaHora(fechaHora);
        asistencia.setTipoMarcacion(tipo);
        TurnoDelDiaDTO turno = tipo == TipoMarcacion.ENTRADA
                ? turnoTrabajoService.turnoDelDia(usuario.getId(), fechaHora).orElse(null) : null;
        if (turno != null) asistencia.setTurnoTrabajoId(turno.turnoId());
        asistencia.setEstado(calcularEstado(tipo, fechaHora, turno));
        asistencia.setDispositivoInfo(dispositivo);
        asistencia.setFecha(fechaHora.toLocalDate());

        if (tipo == TipoMarcacion.ENTRADA) {
            asistencia.setHoraEntrada(fechaHora.toLocalTime());
            asistencia.setHoraSalida(null);
            asistencia.setHorasTrabajadas(null);
        } else if (tipo == TipoMarcacion.SALIDA) {
            asistencia.setHoraSalida(fechaHora.toLocalTime());

            LocalDateTime inicioDia = fechaHora.toLocalDate().atStartOfDay();
            LocalDateTime finDia = inicioDia.plusDays(1);

            RrhhAsistencia entrada = asistenciaRepository.findUltimaEntradaDelDia(
                    usuario.getId(), inicioDia, finDia);

            if (entrada != null && entrada.getHoraEntrada() != null) {
                asistencia.setHoraEntrada(entrada.getHoraEntrada());
                long minutos = ChronoUnit.MINUTES.between(entrada.getHoraEntrada(), fechaHora.toLocalTime());
                BigDecimal horas = BigDecimal.valueOf(minutos / 60.0).setScale(2, java.math.RoundingMode.HALF_UP);
                asistencia.setHorasTrabajadas(horas);
            }
        }

        return asistencia;
    }

    // ============================================
    // ✅ CRUD Y LISTADOS
    // ============================================

    @Override
    public Page<RrhhAsistenciaResponseDTO> listarTodas(Pageable pageable) {
        return asistenciaRepository.findAll(pageable)
                .map(asistenciaMapper::toResponseDto);
    }

    @Override
    public Page<RrhhAsistenciaResponseDTO> buscarPorEmpleado(UUID empleadoId, Pageable pageable) {
        return asistenciaRepository.findByEmpleadoId(empleadoId, pageable)
                .map(asistenciaMapper::toResponseDto);
    }

    @Override
    public RrhhAsistenciaResponseDTO buscarPorId(UUID id) {
        RrhhAsistencia asistencia = asistenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro no encontrado con ID: " + id));
        return asistenciaMapper.toResponseDto(asistencia);
    }

    @Override
    @Transactional
    public RrhhAsistenciaResponseDTO actualizarMarcacionManual(UUID id, RrhhAsistenciaRequestDTO updateRequest) {
        RrhhAsistencia asistenciaExistente = asistenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro no encontrado con ID: " + id));

        asistenciaMapper.updateEntityFromDto(updateRequest, asistenciaExistente);
        asistenciaExistente.setDispositivoInfo("Modificado por Admin (Manual)");

        RrhhAsistencia actualizada = asistenciaRepository.save(asistenciaExistente);
        return asistenciaMapper.toResponseDto(actualizada);
    }

    @Override
    @Transactional
    public void eliminarMarcacion(UUID id) {
        if (!asistenciaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Registro no encontrado con ID: " + id);
        }
        asistenciaRepository.deleteById(id);
    }

    @Override
    public Page<RrhhAsistenciaResponseDTO> listarAsistencias(
            UUID empleadoId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable) {

        Page<RrhhAsistencia> page;

        if (empleadoId != null && fechaInicio != null && fechaFin != null) {
            page = asistenciaRepository.findByEmpleadoIdAndFechaHoraBetween(
                    empleadoId, fechaInicio, fechaFin, pageable);
        } else if (empleadoId != null) {
            page = asistenciaRepository.findByEmpleadoId(empleadoId, pageable);
        } else if (fechaInicio != null && fechaFin != null) {
            page = asistenciaRepository.findByFechaHoraBetween(fechaInicio, fechaFin, pageable);
        } else {
            page = asistenciaRepository.findAllConEmpleados(pageable);
        }

        return page.map(asistenciaMapper::toResponseDto);
    }

    @Override
    public Page<RrhhAsistenciaResponseDTO> misAsistencias(
            UUID empleadoId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable) {

        if (empleadoId == null) {
            throw new IllegalArgumentException("El ID del empleado es requerido");
        }

        userRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        Page<RrhhAsistencia> page;

        if (fechaInicio != null && fechaFin != null) {
            page = asistenciaRepository.findByEmpleadoIdAndFechaHoraBetween(
                    empleadoId, fechaInicio, fechaFin, pageable);
        } else {
            page = asistenciaRepository.findByEmpleadoId(empleadoId, pageable);
        }

        return page.map(asistenciaMapper::toResponseDto);
    }

    @Override
    public DashboardAsistenciaDTO obtenerDashboard(UUID empleadoId) {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);

        long totalEmpleados = asistenciaRepository.countEmpleadosActivos();
        long presentesHoy = asistenciaRepository.countPresentesHoy(inicioDia, finDia);
        long tardanzasHoy = asistenciaRepository.countTardanzasHoy(inicioDia, finDia);
        long ausentesHoy = totalEmpleados - presentesHoy;

        List<RrhhAsistencia> recientes;
        if (empleadoId != null) {
            Pageable top5 = Pageable.ofSize(5);
            Page<RrhhAsistencia> page = asistenciaRepository.findByEmpleadoId(empleadoId, top5);
            recientes = page.getContent();
        } else {
            recientes = asistenciaRepository.findUltimasAsistencias();
        }

        List<RrhhAsistenciaResponseDTO> recientesDTO = recientes.stream()
                .map(asistenciaMapper::toResponseDto)
                .toList();

        return new DashboardAsistenciaDTO(
                totalEmpleados,
                presentesHoy,
                ausentesHoy,
                tardanzasHoy,
                recientesDTO
        );
    }

    @Override
    public RrhhAsistenciaResponseDTO obtenerUltimaMarcacion(UUID empleadoId) {
        RrhhAsistencia ultima = asistenciaRepository.findUltimaMarcacion(empleadoId);
        if (ultima == null) {
            return null;
        }
        return asistenciaMapper.toResponseDto(ultima);
    }

    @Override
    public AsistenciaEstadoDTO obtenerEstadoDelDia(UUID empleadoId) {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);

        boolean yaEntrada = asistenciaRepository.yaMarcoEntradaHoy(empleadoId, inicioDia, finDia);
        boolean yaSalida = asistenciaRepository.yaMarcoSalidaHoy(empleadoId, inicioDia, finDia);

        return new AsistenciaEstadoDTO(
                yaEntrada,
                yaSalida,
                yaEntrada ? "✅ ENTRADA registrada hoy" : "❌ No ha marcado ENTRADA hoy",
                yaSalida ? "✅ SALIDA registrada hoy" : "❌ No ha marcado SALIDA hoy"
        );
    }
}