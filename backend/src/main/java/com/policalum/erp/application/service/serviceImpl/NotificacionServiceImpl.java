package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.NotificacionResponseDTO;
import com.policalum.erp.domain.model.Notificacion;
import com.policalum.erp.domain.model.Stock;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.NotificacionRepository;
import com.policalum.erp.infrastructure.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl {

    private static final int DIAS_CONSERVACION = 30;

    private final NotificacionRepository notificacionRepository;
    private final StockRepository stockRepository;

    @Transactional
    public List<NotificacionResponseDTO> listar() {
        sincronizarStock();
        return notificacionRepository.findAllByOrderByCreatedAtDesc().stream().map(this::dto).toList();
    }

    @Transactional
    public NotificacionResponseDTO marcarLeida(UUID id) {
        Notificacion aviso = buscar(id);
        aviso.setLeida(true);
        aviso.setLeidaAt(LocalDateTime.now());
        return dto(aviso);
    }

    @Transactional
    public int marcarTodasLeidas() {
        int marcadas = 0;
        for (Notificacion aviso : notificacionRepository.findAllByOrderByCreatedAtDesc()) {
            if (!aviso.isLeida() && !aviso.isEliminada()) {
                aviso.setLeida(true);
                aviso.setLeidaAt(LocalDateTime.now());
                marcadas++;
            }
        }
        return marcadas;
    }

    /** La campana deja de mostrarla. La vista de notificaciones la conserva como pasada. */
    @Transactional
    public NotificacionResponseDTO archivar(UUID id) {
        Notificacion aviso = buscar(id);
        aviso.setEliminada(true);
        aviso.setLeida(true);
        aviso.setLeidaAt(aviso.getLeidaAt() == null ? LocalDateTime.now() : aviso.getLeidaAt());
        return dto(aviso);
    }

    @Transactional
    @Scheduled(cron = "0 20 3 * * *")
    public int purgarVencidas() {
        return (int) notificacionRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(DIAS_CONSERVACION));
    }

    @Transactional
    public int sincronizarStock() {
        int creadas = 0;
        for (Stock stock : stockRepository.alertasDeStock()) {
            boolean critico = stock.getCantidad() == null || stock.getCantidad().compareTo(BigDecimal.ZERO) <= 0;
            String nivel = critico ? "CRITICO" : "MINIMO";
            String clave = "STOCK:" + stock.getProducto().getId() + ":" + stock.getAlmacen().getId() + ":" + nivel;
            if (notificacionRepository.findByClave(clave).isPresent()) continue;
            String nombre = stock.getProducto().getNombre();
            String almacen = stock.getAlmacen().getNombre();
            Notificacion aviso = Notificacion.builder()
                    .clave(clave)
                    .tipo(critico ? "STOCK_CRITICO" : "STOCK_MINIMO")
                    .titulo(critico ? "Stock crítico" : "Stock bajo el mínimo")
                    .detalle(nombre + " en " + almacen + ": " + stock.getCantidad() + " (mínimo " + stock.getStockMinimo() + ")")
                    .ruta("/modules/stock?tab=bajo")
                    .leida(false)
                    .eliminada(false)
                    .build();
            notificacionRepository.save(aviso);
            creadas++;
        }
        return creadas;
    }

    private Notificacion buscar(UUID id) {
        return notificacionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
    }

    private NotificacionResponseDTO dto(Notificacion aviso) {
        return new NotificacionResponseDTO(aviso.getId(), aviso.getTipo(), aviso.getTitulo(), aviso.getDetalle(),
                aviso.getRuta(), aviso.isLeida(), aviso.isEliminada(), aviso.getCreatedAt());
    }
}
