package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.domain.model.PushSuscripcion;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.domain.model.VapidClave;
import com.policalum.erp.infrastructure.repository.PushSuscripcionRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.VapidClaveRepository;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Envía la misma alerta a cada navegador donde el usuario activó las notificaciones. */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnvioPushService {

    private final VapidClaveRepository claves;
    private final PushSuscripcionRepository suscripciones;
    private final UserRepository usuarios;

    @Transactional
    public String clavePublica() {
        return asegurar().getPublica();
    }

    @Transactional
    public void guardar(String endpoint, String p256dh, String auth, String dispositivo) {
        User usuario = actual();
        PushSuscripcion fila = suscripciones.findByEndpoint(endpoint).orElseGet(PushSuscripcion::new);
        fila.setUsuarioId(usuario.getId());
        fila.setEndpoint(endpoint);
        fila.setP256dh(p256dh);
        fila.setAuth(auth);
        fila.setDispositivo(dispositivo == null ? null : dispositivo.substring(0, Math.min(80, dispositivo.length())));
        suscripciones.save(fila);
    }

    @Transactional
    public void quitar(String endpoint) {
        suscripciones.findByEndpoint(endpoint).ifPresent(suscripciones::delete);
    }

    public void enviar(String titulo, String cuerpo, String ruta) {
        try {
            VapidClave par = asegurar();
            PushService servicio = new PushService(par.getPublica(), par.getPrivada(), "mailto:soporte@policalum.local");
            String carga = "{\"title\":" + json(titulo) + ",\"body\":" + json(cuerpo) + ",\"url\":" + json(ruta) + "}";
            for (PushSuscripcion fila : suscripciones.findAll()) {
                enviarUna(servicio, fila, carga);
            }
        } catch (Exception ex) {
            log.warn("No se pudieron enviar las notificaciones del navegador: {}", ex.getMessage());
        }
    }

    private void enviarUna(PushService servicio, PushSuscripcion fila, String carga) {
        try {
            Subscription subscription = new Subscription(fila.getEndpoint(), new Subscription.Keys(fila.getP256dh(), fila.getAuth()));
            var respuesta = servicio.send(new Notification(subscription, carga));
            int codigo = respuesta.getStatusLine().getStatusCode();
            if (codigo == 404 || codigo == 410) suscripciones.delete(fila);
        } catch (Exception ex) {
            log.warn("Suscripción descartada {}: {}", fila.getId(), ex.getMessage());
        }
    }

    private VapidClave asegurar() {
        return claves.findById(VapidClave.ID).orElseGet(this::crear);
    }

    private VapidClave crear() {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            KeyPairGenerator generador = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
            generador.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair par = generador.generateKeyPair();
            VapidClave fila = new VapidClave();
            fila.setId(VapidClave.ID);
            fila.setPublica(Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.encode((ECPublicKey) par.getPublic())));
            fila.setPrivada(Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.encode((ECPrivateKey) par.getPrivate())));
            return claves.save(fila);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo preparar las notificaciones del navegador", ex);
        }
    }

    private User actual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarios.findByEmailAndActivoTrue(email).orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }

    private static String json(String valor) {
        String texto = valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
        return "\"" + texto + "\"";
    }
}
