package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.ChatClienteRequest;
import com.policalum.erp.application.dto.request.ChatTextoRequest;
import com.policalum.erp.application.dto.request.ClienteRequestDTO;
import com.policalum.erp.application.dto.request.DatosChatRequest;
import com.policalum.erp.application.dto.response.HiloEquipoDTO;
import com.policalum.erp.application.dto.response.HiloPublicoDTO;
import com.policalum.erp.application.dto.response.LineaChatDTO;
import com.policalum.erp.application.dto.response.ReconocimientoChatDTO;
import com.policalum.erp.application.dto.response.ResumenChatDTO;
import com.policalum.erp.domain.enums.AutorChat;
import com.policalum.erp.domain.model.ConversacionWeb;
import com.policalum.erp.domain.model.LineaChat;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.application.service.ClienteService;
import com.policalum.erp.infrastructure.repository.ConversacionWebRepository;
import com.policalum.erp.infrastructure.repository.LineaChatRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatWebService {

    private static final int PRESENCIA_SEGUNDOS = 5;

    private final ConversacionWebRepository conversaciones;
    private final LineaChatRepository lineas;
    private final CanalWebNotificador notificador;
    private final ClienteService clientes;

    @Transactional
    public HiloPublicoDTO enviarCliente(ChatClienteRequest request) {
        ConversacionWeb conversacion = localizar(request.clave()).orElseGet(() -> nueva(request));
        conversacion.setNombre(request.nombre().trim());
        conversacion.setCorreo(request.correo().trim().toLowerCase());
        conversacion.setTelefono(vacio(request.telefono()));
        conversacion.setEmpresa(vacio(request.empresa()));
        if (vacio(request.dispositivo()) != null) conversacion.setDispositivo(request.dispositivo().trim());
        if ("CERRADA".equals(conversacion.getEstado())) conversacion.setEstado("ABIERTA");
        LineaChat linea = agregar(conversacion, AutorChat.CLIENTE, request.nombre().trim(), request.texto());
        conversacion.setNoLeidosEquipo(conversacion.getNoLeidosEquipo() + 1);
        conversacion.setClienteEscribeHasta(null);
        conversaciones.save(conversacion);
        notificador.mensajeChat(linea.getId(), conversacion.getNombre(), linea.getTexto());
        return hiloPublico(conversacion);
    }

    @Transactional(readOnly = true)
    public HiloPublicoDTO leerCliente(String clave) {
        return hiloPublico(exigirClave(clave));
    }

    @Transactional
    public void marcarLeidoCliente(String clave) {
        ConversacionWeb conversacion = exigirClave(clave);
        if (conversacion.getNoLeidosCliente() == 0) return;
        conversacion.setNoLeidosCliente(0);
        conversaciones.save(conversacion);
    }

    @Transactional
    public void presenciaCliente(String clave, boolean activo) {
        ConversacionWeb conversacion = localizar(clave).orElse(null);
        if (conversacion == null) return;
        conversacion.setClienteEscribeHasta(activo ? LocalDateTime.now().plusSeconds(PRESENCIA_SEGUNDOS) : null);
        conversaciones.save(conversacion);
    }

    @Transactional(readOnly = true)
    public List<ResumenChatDTO> listar() {
        return conversaciones.findAllByOrderByUltimoMensajeAtDesc().stream().map(this::resumen).toList();
    }

    @Transactional(readOnly = true)
    public HiloEquipoDTO leerEquipo(UUID id) {
        return hiloEquipo(exigir(id));
    }

    @Transactional
    public HiloEquipoDTO responder(UUID id, ChatTextoRequest request) {
        ConversacionWeb conversacion = exigir(id);
        String nombre = request.autorNombre() == null || request.autorNombre().isBlank() ? "POLICALUM" : request.autorNombre().trim();
        agregar(conversacion, AutorChat.EQUIPO, nombre, request.texto());
        conversacion.setNoLeidosCliente(conversacion.getNoLeidosCliente() + 1);
        conversacion.setNoLeidosEquipo(0);
        if ("CERRADA".equals(conversacion.getEstado())) conversacion.setEstado("ABIERTA");
        conversacion.setEquipoEscribeHasta(null);
        conversaciones.save(conversacion);
        return hiloEquipo(conversacion);
    }

    @Transactional
    public void marcarLeidoEquipo(UUID id) {
        ConversacionWeb conversacion = exigir(id);
        if (conversacion.getNoLeidosEquipo() == 0) return;
        conversacion.setNoLeidosEquipo(0);
        conversaciones.save(conversacion);
    }

    @Transactional
    public void presenciaEquipo(UUID id, boolean activo) {
        ConversacionWeb conversacion = exigir(id);
        conversacion.setEquipoEscribeHasta(activo ? LocalDateTime.now().plusSeconds(PRESENCIA_SEGUNDOS) : null);
        conversaciones.save(conversacion);
    }

    private ConversacionWeb nueva(ChatClienteRequest request) {
        ConversacionWeb conversacion = ConversacionWeb.builder()
                .clave(UUID.randomUUID().toString())
                .nombre(request.nombre().trim())
                .correo(request.correo().trim().toLowerCase())
                .telefono(vacio(request.telefono()))
                .empresa(vacio(request.empresa()))
                .estado("ABIERTA")
                .dispositivo(vacio(request.dispositivo()))
                .noLeidosEquipo(0)
                .noLeidosCliente(0)
                .build();
        return conversaciones.save(conversacion);
    }

    private LineaChat agregar(ConversacionWeb conversacion, AutorChat autor, String autorNombre, String texto) {
        String limpio = texto.trim();
        LineaChat linea = lineas.save(LineaChat.builder()
                .conversacionId(conversacion.getId())
                .autor(autor)
                .autorNombre(autorNombre)
                .texto(limpio)
                .build());
        conversacion.setUltimoTexto(limpio.length() > 180 ? limpio.substring(0, 180) : limpio);
        conversacion.setUltimoMensajeAt(linea.getCreatedAt() == null ? LocalDateTime.now() : linea.getCreatedAt());
        return linea;
    }

    private java.util.Optional<ConversacionWeb> localizar(String clave) {
        if (clave == null || clave.isBlank()) return java.util.Optional.empty();
        try {
            UUID.fromString(clave.trim());
        } catch (IllegalArgumentException ex) {
            return java.util.Optional.empty();
        }
        return conversaciones.findByClave(clave.trim());
    }

    private ConversacionWeb exigirClave(String clave) {
        return localizar(clave).orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));
    }

    private ConversacionWeb exigir(UUID id) {
        return conversaciones.findById(id).orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));
    }

    private HiloPublicoDTO hiloPublico(ConversacionWeb conversacion) {
        return new HiloPublicoDTO(
                conversacion.getId(),
                conversacion.getClave(),
                conversacion.getNombre(),
                conversacion.getNoLeidosCliente(),
                vigente(conversacion.getEquipoEscribeHasta()),
                lineasDe(conversacion.getId()));
    }

    private HiloEquipoDTO hiloEquipo(ConversacionWeb conversacion) {
        return new HiloEquipoDTO(
                conversacion.getId(),
                conversacion.getNombre(),
                conversacion.getCorreo(),
                conversacion.getTelefono(),
                conversacion.getEmpresa(),
                conversacion.getNoLeidosEquipo(),
                vigente(conversacion.getClienteEscribeHasta()),
                lineasDe(conversacion.getId()),
                conversacion.getEstado() == null ? "ABIERTA" : conversacion.getEstado(),
                conversacion.getClienteId());
    }

    private ResumenChatDTO resumen(ConversacionWeb conversacion) {
        return new ResumenChatDTO(
                conversacion.getId(),
                conversacion.getNombre(),
                conversacion.getCorreo(),
                conversacion.getTelefono(),
                conversacion.getEmpresa(),
                conversacion.getUltimoTexto(),
                conversacion.getUltimoMensajeAt(),
                conversacion.getNoLeidosEquipo(),
                vigente(conversacion.getClienteEscribeHasta()),
                conversacion.getEstado() == null ? "ABIERTA" : conversacion.getEstado(),
                conversacion.getClienteId());
    }

    @Transactional(readOnly = true)
    public ReconocimientoChatDTO reconocer(String correo, String telefono, String dispositivo) {
        ConversacionWeb hallada = buscarParecida(correo, telefono, dispositivo);
        if (hallada == null) return new ReconocimientoChatDTO(false, false, null, null);
        boolean mismo = dispositivo != null && !dispositivo.isBlank() && dispositivo.trim().equals(hallada.getDispositivo());
        return new ReconocimientoChatDTO(true, mismo, hallada.getNombre(), hallada.getId());
    }

    @Transactional
    public String continuar(UUID referencia, String correo, String telefono, String dispositivo) {
        ConversacionWeb conversacion = exigir(referencia);
        String correoLimpio = correo == null ? "" : correo.trim().toLowerCase();
        String telefonoLimpio = digitos(telefono);
        boolean correoIgual = !correoLimpio.isBlank() && correoLimpio.equalsIgnoreCase(conversacion.getCorreo());
        boolean telefonoIgual = telefonoLimpio.length() >= 9 && telefonoLimpio.equals(digitos(conversacion.getTelefono()));
        if (!correoIgual && !telefonoIgual) {
            throw new IllegalArgumentException("Los datos no coinciden con la conversación anterior");
        }
        if (dispositivo != null && !dispositivo.isBlank()) conversacion.setDispositivo(dispositivo.trim());
        conversacion.setEstado("ABIERTA");
        conversaciones.save(conversacion);
        return conversacion.getClave();
    }

    @Transactional
    public HiloEquipoDTO actualizarDatos(UUID id, DatosChatRequest request) {
        ConversacionWeb conversacion = exigir(id);
        conversacion.setNombre(request.nombre().trim());
        conversacion.setCorreo(request.correo().trim().toLowerCase());
        conversacion.setTelefono(vacio(request.telefono()));
        conversaciones.save(conversacion);
        return hiloEquipo(conversacion);
    }

    @Transactional
    public void cambiarEstado(UUID id, String estado) {
        ConversacionWeb conversacion = exigir(id);
        conversacion.setEstado(estado);
        if ("CERRADA".equals(estado) || "EN_ESPERA".equals(estado)) conversacion.setNoLeidosEquipo(0);
        conversaciones.save(conversacion);
    }

    @Transactional
    public void eliminar(UUID id) {
        exigir(id);
        lineas.deleteByConversacionId(id);
        conversaciones.deleteById(id);
    }

    @Transactional
    public HiloEquipoDTO guardarCliente(UUID id, ClienteRequestDTO request) {
        ConversacionWeb conversacion = exigir(id);
        var creado = clientes.create(request);
        conversacion.setClienteId(creado.id());
        conversacion.setNombre(request.nombre().trim());
        if (request.email() != null && !request.email().isBlank()) conversacion.setCorreo(request.email().trim().toLowerCase());
        if (request.celular() != null && !request.celular().isBlank()) conversacion.setTelefono(request.celular().trim());
        conversaciones.save(conversacion);
        return hiloEquipo(conversacion);
    }

    private ConversacionWeb buscarParecida(String correo, String telefono, String dispositivo) {
        if (dispositivo != null && !dispositivo.isBlank()) {
            ConversacionWeb porEquipo = conversaciones.findFirstByDispositivoOrderByUltimoMensajeAtDesc(dispositivo.trim()).orElse(null);
            if (porEquipo != null && coincide(porEquipo, correo, telefono)) return porEquipo;
        }
        if (correo != null && !correo.isBlank()) {
            ConversacionWeb porCorreo = conversaciones.findFirstByCorreoIgnoreCaseOrderByUltimoMensajeAtDesc(correo.trim()).orElse(null);
            if (porCorreo != null) return porCorreo;
        }
        String soloDigitos = digitos(telefono);
        if (soloDigitos.length() >= 9) {
            return conversaciones.findAllByOrderByUltimoMensajeAtDesc().stream()
                    .filter(conversacion -> soloDigitos.equals(digitos(conversacion.getTelefono())))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private static boolean coincide(ConversacionWeb conversacion, String correo, String telefono) {
        String correoLimpio = correo == null ? "" : correo.trim();
        boolean correoIgual = !correoLimpio.isBlank() && correoLimpio.equalsIgnoreCase(conversacion.getCorreo());
        String telefonoLimpio = digitos(telefono);
        boolean telefonoIgual = telefonoLimpio.length() >= 9 && telefonoLimpio.equals(digitos(conversacion.getTelefono()));
        return correoIgual || telefonoIgual;
    }

    private static String digitos(String valor) {
        if (valor == null) return "";
        return valor.replaceAll("\\D", "");
    }

    private List<LineaChatDTO> lineasDe(UUID conversacionId) {
        return lineas.findByConversacionIdOrderByCreatedAtAsc(conversacionId).stream()
                .map(linea -> new LineaChatDTO(linea.getId(), linea.getAutor().name(), linea.getAutorNombre(), linea.getTexto(), linea.getCreatedAt()))
                .toList();
    }

    private static boolean vigente(LocalDateTime hasta) {
        return hasta != null && hasta.isAfter(LocalDateTime.now());
    }

    private static String vacio(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
