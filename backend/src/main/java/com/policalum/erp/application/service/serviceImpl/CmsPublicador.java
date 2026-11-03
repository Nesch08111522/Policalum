package com.policalum.erp.application.service.serviceImpl;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Invalida la caché del CMS y avisa al sitio abierto para que vuelva a leer. */
@Service
@RequiredArgsConstructor
public class CmsPublicador {

    private static final long TIMEOUT_MS = 15 * 60 * 1000L;
    private final CacheManager cacheManager;
    private final CopyOnWriteArrayList<SseEmitter> emisores = new CopyOnWriteArrayList<>();

    public SseEmitter suscribir() {
        SseEmitter emisor = new SseEmitter(TIMEOUT_MS);
        emisores.add(emisor);
        emisor.onCompletion(() -> emisores.remove(emisor));
        emisor.onTimeout(() -> emisores.remove(emisor));
        emisor.onError(error -> emisores.remove(emisor));
        try {
            emisor.send(SseEmitter.event().name("listo").data("ok"));
        } catch (IOException ex) {
            emisores.remove(emisor);
        }
        return emisor;
    }

    public void publicar(String tipo) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emitir(tipo);
                }
            });
            return;
        }
        emitir(tipo);
    }

    private void emitir(String tipo) {
        Cache cache = cacheManager.getCache("cms-publico");
        if (cache != null) cache.clear();
        String carga = "{\"tipo\":\"" + tipo + "\"}";
        for (SseEmitter emisor : List.copyOf(emisores)) {
            try {
                emisor.send(SseEmitter.event().name("cms").data(carga));
            } catch (Exception ex) {
                emisores.remove(emisor);
            }
        }
    }
}
