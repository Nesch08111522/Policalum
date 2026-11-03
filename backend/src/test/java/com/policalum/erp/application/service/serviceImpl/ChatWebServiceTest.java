package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.policalum.erp.application.dto.request.ChatClienteRequest;
import com.policalum.erp.application.service.ClienteService;
import com.policalum.erp.application.dto.request.ChatTextoRequest;
import com.policalum.erp.application.dto.response.HiloEquipoDTO;
import com.policalum.erp.application.dto.response.HiloPublicoDTO;
import com.policalum.erp.domain.model.ConversacionWeb;
import com.policalum.erp.domain.model.LineaChat;
import com.policalum.erp.infrastructure.repository.ConversacionWebRepository;
import com.policalum.erp.infrastructure.repository.LineaChatRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatWebServiceTest {

    @Mock ConversacionWebRepository conversaciones;
    @Mock LineaChatRepository lineas;
    @Mock CanalWebNotificador notificador;
    @Mock ClienteService clientes;

    ChatWebService servicio;
    final List<ConversacionWeb> guardadas = new ArrayList<>();
    final List<LineaChat> textos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        servicio = new ChatWebService(conversaciones, lineas, notificador, clientes);
        when(conversaciones.save(any())).thenAnswer(invocacion -> {
            ConversacionWeb conversacion = invocacion.getArgument(0);
            if (conversacion.getId() == null) conversacion.setId(UUID.randomUUID());
            guardadas.removeIf(actual -> actual.getId().equals(conversacion.getId()));
            guardadas.add(conversacion);
            return conversacion;
        });
        lenient().when(conversaciones.findByClave(any())).thenAnswer(invocacion -> guardadas.stream()
                .filter(conversacion -> invocacion.getArgument(0).equals(conversacion.getClave()))
                .findFirst());
        lenient().when(conversaciones.findById(any())).thenAnswer(invocacion -> guardadas.stream()
                .filter(conversacion -> invocacion.getArgument(0).equals(conversacion.getId()))
                .findFirst());
        when(lineas.save(any())).thenAnswer(invocacion -> {
            LineaChat linea = invocacion.getArgument(0);
            if (linea.getId() == null) linea.setId(UUID.randomUUID());
            textos.add(linea);
            return linea;
        });
        when(lineas.findByConversacionIdOrderByCreatedAtAsc(any())).thenAnswer(invocacion -> textos.stream()
                .filter(linea -> invocacion.getArgument(0).equals(linea.getConversacionId()))
                .toList());
    }

    @Test
    void clienteYEquipoSeRespondenEnElMismoHilo() {
        HiloPublicoDTO publico = servicio.enviarCliente(new ChatClienteRequest(
                null, "Nancy", "nancy@correo.pe", "999888777", null, "Quiero una plancha", null));

        assertEquals(0, publico.noLeidos());
        assertEquals(1, textos.size());
        assertEquals(1, guardadas.get(0).getNoLeidosEquipo());
        assertEquals("Quiero una plancha", publico.lineas().get(0).texto());

        HiloEquipoDTO equipo = servicio.responder(publico.id(), new ChatTextoRequest("Te confirmo el stock", "Nancy"));

        assertEquals(0, equipo.noLeidos());
        assertEquals(1, guardadas.get(0).getNoLeidosCliente());
        assertEquals(2, equipo.lineas().size());
        assertEquals("EQUIPO", equipo.lineas().get(1).autor());

        servicio.presenciaCliente(publico.clave(), true);
        assertTrue(servicio.leerEquipo(publico.id()).clienteEscribiendo());

        servicio.marcarLeidoCliente(publico.clave());
        assertEquals(0, servicio.leerCliente(publico.clave()).noLeidos());
    }

    @Test
    void claveDesconocidaAbreUnHiloNuevo() {
        servicio.enviarCliente(new ChatClienteRequest("no-es-uuid", "Ana", "ana@correo.pe", "", null, "Hola", null));
        assertEquals(1, guardadas.size());
        Optional<ConversacionWeb> creada = Optional.of(guardadas.get(0));
        assertTrue(creada.get().getClave().length() == 36);
    }
}
