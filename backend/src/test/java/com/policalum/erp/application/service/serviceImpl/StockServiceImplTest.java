package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.KardexMovimientoRequestDTO;
import com.policalum.erp.application.dto.request.StockRequestDTO;
import com.policalum.erp.application.mapper.StockMapper;
import com.policalum.erp.application.service.KardexService;
import com.policalum.erp.domain.enums.TipoMovimiento;
import com.policalum.erp.domain.enums.TipoReferencia;
import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.domain.model.Stock;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.StockRepository;
import jakarta.validation.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

/** Consulta y carga directa de stock. */
@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock StockRepository stockRepository;
    @Mock StockMapper stockMapper;
    @Mock KardexService kardexService;
    @InjectMocks StockServiceImpl service;

    Producto producto;
    Almacen almacen;

    @BeforeEach
    void setUp() {
        producto = Producto.builder().id(UUID.randomUUID()).build();
        almacen = Almacen.builder().id(UUID.randomUUID()).build();
        lenient().when(stockRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void saveOrUpdate_filaExistente_mueveLaDiferenciaPorKardex() {
        Stock s = Stock.builder().producto(producto).almacen(almacen).cantidad(new BigDecimal("3"))
                .stockMinimo(new BigDecimal("1")).updatedAt(LocalDateTime.now().minusDays(1)).build();
        when(stockRepository.findByAlmacen_IdAndProducto_Id(almacen.getId(), producto.getId())).thenReturn(Optional.of(s));
        doAnswer(invocacion -> {
            KardexMovimientoRequestDTO movimiento = invocacion.getArgument(0);
            s.setCantidad(s.getCantidad().add(movimiento.cantidadMovida()));
            return null;
        }).when(kardexService).registrarMovimiento(any());
        LocalDateTime antes = LocalDateTime.now();

        service.saveOrUpdate(new StockRequestDTO(producto, almacen, new BigDecimal("10"), new BigDecimal("2"), null));

        ArgumentCaptor<KardexMovimientoRequestDTO> movimiento = ArgumentCaptor.forClass(KardexMovimientoRequestDTO.class);
        verify(kardexService).registrarMovimiento(movimiento.capture());
        assertEquals(TipoMovimiento.ENTRADA, movimiento.getValue().tipoMovimiento());
        assertEquals(TipoReferencia.AJUSTE, movimiento.getValue().tipoReferencia());
        assertEquals(0, movimiento.getValue().cantidadMovida().compareTo(new BigDecimal("7")));
        assertEquals(new BigDecimal("10"), s.getCantidad());
        assertEquals(new BigDecimal("2"), s.getStockMinimo());
        assertFalse(s.getUpdatedAt().isBefore(antes));
        verify(stockRepository).save(s);
    }

    @Test
    void saveOrUpdate_sinMinimo_conservaElAnterior() {
        Stock s = Stock.builder().producto(producto).almacen(almacen).cantidad(BigDecimal.ONE).stockMinimo(new BigDecimal("5")).build();
        when(stockRepository.findByAlmacen_IdAndProducto_Id(any(), any())).thenReturn(Optional.of(s));
        doAnswer(invocacion -> {
            KardexMovimientoRequestDTO movimiento = invocacion.getArgument(0);
            s.setCantidad(movimiento.tipoMovimiento() == TipoMovimiento.SALIDA
                    ? s.getCantidad().subtract(movimiento.cantidadMovida())
                    : s.getCantidad().add(movimiento.cantidadMovida()));
            return null;
        }).when(kardexService).registrarMovimiento(any());

        service.saveOrUpdate(new StockRequestDTO(producto, almacen, new BigDecimal("7"), null, null));

        assertEquals(new BigDecimal("5"), s.getStockMinimo());
        assertEquals(0, s.getCantidad().compareTo(new BigDecimal("7")));
    }

    @Test
    void saveOrUpdate_mismaCantidad_noRegistraMovimiento() {
        Stock s = Stock.builder().producto(producto).almacen(almacen).cantidad(new BigDecimal("4")).stockMinimo(BigDecimal.ONE).build();
        when(stockRepository.findByAlmacen_IdAndProducto_Id(any(), any())).thenReturn(Optional.of(s));

        service.saveOrUpdate(new StockRequestDTO(producto, almacen, new BigDecimal("4.00"), new BigDecimal("3"), null));

        verify(kardexService, never()).registrarMovimiento(any());
        assertEquals(new BigDecimal("3"), s.getStockMinimo());
    }

    @Test
    void saveOrUpdate_cantidadNegativa_lanzaValidation() {
        assertThrows(ValidationException.class, () -> service.saveOrUpdate(
                new StockRequestDTO(producto, almacen, new BigDecimal("-1"), null, null)));
        verifyNoInteractions(kardexService);
    }

    @Test
    void saveOrUpdate_sinFila_creaLaEntradaPorKardex() {
        Stock creado = Stock.builder().producto(producto).almacen(almacen).cantidad(BigDecimal.ZERO).build();
        when(stockRepository.findByAlmacen_IdAndProducto_Id(any(), any())).thenReturn(Optional.empty(), Optional.of(creado));
        doAnswer(invocacion -> {
            creado.setCantidad(new BigDecimal("4"));
            return null;
        }).when(kardexService).registrarMovimiento(any());

        service.saveOrUpdate(new StockRequestDTO(producto, almacen, new BigDecimal("4"), null, null));

        ArgumentCaptor<KardexMovimientoRequestDTO> movimiento = ArgumentCaptor.forClass(KardexMovimientoRequestDTO.class);
        verify(kardexService).registrarMovimiento(movimiento.capture());
        assertEquals(TipoMovimiento.ENTRADA, movimiento.getValue().tipoMovimiento());
        assertEquals(0, movimiento.getValue().cantidadMovida().compareTo(new BigDecimal("4")));
        verify(stockRepository).save(creado);
        assertSame(producto, creado.getProducto());
        assertSame(almacen, creado.getAlmacen());
    }

    @Test
    void findByAlmacenAndProducto_sinFila_lanzaNotFound() {
        when(stockRepository.findByAlmacen_IdAndProducto_Id(almacen.getId(), producto.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByAlmacenAndProducto(almacen.getId(), producto.getId()));
    }

    @Test
    void searchByAlmacen_terminoEnBlancoSeEnviaNuloYSeRecorta() {
        Pageable pg = PageRequest.of(0, 10);
        when(stockRepository.searchByAlmacen(any(), any(), anyBoolean(), any())).thenReturn(Page.empty());

        service.searchByAlmacen(almacen.getId(), "  ", true, pg);
        service.searchByAlmacen(almacen.getId(), " poli ", false, pg);

        verify(stockRepository).searchByAlmacen(almacen.getId(), null, true, pg);
        verify(stockRepository).searchByAlmacen(almacen.getId(), "poli", false, pg);
    }

    @Test
    void findByProducto_mapeaCadaAlmacen() {
        when(stockRepository.findByProducto_Id(producto.getId())).thenReturn(List.of(new Stock(), new Stock()));

        assertEquals(2, service.findByProducto(producto.getId()).size());
    }
}
