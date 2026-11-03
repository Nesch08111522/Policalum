package com.policalum.erp.application.service.serviceImpl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class CmsPublicadorTest {

    @Mock CacheManager cacheManager;
    @InjectMocks CmsPublicador publicador;

    @AfterEach
    void limpiar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publicar_sinTransaccion_limpiaLaCacheAlMomento() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("cms-publico")).thenReturn(cache);

        publicador.publicar("ajustes");

        verify(cache).clear();
    }

    @Test
    void publicar_dentroDeTransaccion_esperaAlCommit() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("cms-publico")).thenReturn(cache);
        TransactionSynchronizationManager.initSynchronization();

        publicador.publicar("catalogo");

        verify(cache, never()).clear();
        for (TransactionSynchronization sincronizacion : TransactionSynchronizationManager.getSynchronizations()) {
            sincronizacion.afterCommit();
        }
        verify(cache).clear();
    }
}
