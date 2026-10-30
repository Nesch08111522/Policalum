package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import com.policalum.erp.application.dto.request.KardexMovimientoRequestDTO;
import com.policalum.erp.application.dto.request.ProductoImportacionFilaDTO;
import com.policalum.erp.application.dto.request.ProductoImportacionRequestDTO;
import com.policalum.erp.application.dto.response.ProductoImportacionResultadoDTO;
import com.policalum.erp.application.dto.response.ProductoResponseDTO;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.application.service.KardexService;
import com.policalum.erp.application.service.ProductoService;
import com.policalum.erp.domain.enums.TipoMovimiento;
import com.policalum.erp.domain.enums.TipoProducto;
import com.policalum.erp.domain.enums.TipoReferencia;
import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.exception.ImportacionRechazadaException;
import com.policalum.erp.infrastructure.repository.AlmacenRepository;
import com.policalum.erp.infrastructure.repository.CategoriaRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;

import jakarta.validation.ValidationException;

/** La importación es un solo lote: una fila mala no deja productos nuevos. */
@ExtendWith(MockitoExtension.class)
class ProductoImportacionServiceImplTest {

    @Mock ProductoService productoService;
    @Mock ProductoRepository productoRepository;
    @Mock CategoriaRepository categoriaRepository;
    @Mock AlmacenRepository almacenRepository;
    @Mock KardexService kardexService;
    @Mock FileStorageService fileStorageService;
    @InjectMocks ProductoImportacionServiceImpl service;

    @Test
    void importar_esTransaccionalParaRevertirElLote() throws Exception {
        Transactional tx = ProductoImportacionServiceImpl.class
                .getMethod("importar", ProductoImportacionRequestDTO.class)
                .getAnnotation(Transactional.class);
        assertNotNull(tx);
        assertFalse(tx.readOnly());
    }

    @Test
    void importar_segundaFilaInvalida_noEscribeNada() {
        when(productoRepository.findByCodigoSkuIgnoreCase(any())).thenReturn(Optional.empty());

        ImportacionRechazadaException ex = assertThrows(ImportacionRechazadaException.class, () -> service.importar(lote(
                fila("A-1", "Plancha", null, null),
                fila("A-2", "  ", null, null))));

        assertEquals(2, ex.getErrores().get(0).fila());
        assertEquals(1, ex.getErrores().size());
        verify(productoService, never()).create(any());
        verify(productoService, never()).update(any(), any());
        verify(kardexService, never()).registrarMovimiento(any());
    }

    @Test
    void importar_stockSinAlmacen_rechazaAntesDeCrear() {
        when(productoRepository.findByCodigoSkuIgnoreCase("A-1")).thenReturn(Optional.empty());
        when(almacenRepository.buscarPorNombre("Inexistente")).thenReturn(List.of());

        ImportacionRechazadaException ex = assertThrows(ImportacionRechazadaException.class,
                () -> service.importar(lote(fila("A-1", "Plancha", new BigDecimal("4"), "Inexistente"))));

        assertEquals(1, ex.getErrores().get(0).fila());
        verify(productoService, never()).create(any());
        verify(kardexService, never()).registrarMovimiento(any());
    }

    @Test
    void importar_skuNuevoSinStock_creaYNoMueveKardex() {
        when(productoRepository.findByCodigoSkuIgnoreCase("A-1")).thenReturn(Optional.empty());
        when(productoService.create(any())).thenReturn(respuesta(UUID.randomUUID()));

        ProductoImportacionResultadoDTO resultado = service.importar(lote(fila("A-1", "Plancha", null, null)));

        assertEquals(1, resultado.creados());
        assertEquals(0, resultado.actualizados());
        verify(kardexService, never()).registrarMovimiento(any());
    }

    @Test
    void importar_skuNuevoConAlmacen_registraEntradaDeKardex() {
        UUID id = UUID.randomUUID();
        Almacen almacen = Almacen.builder().id(UUID.randomUUID()).nombre("Principal").activo(true).build();
        when(productoRepository.findByCodigoSkuIgnoreCase("A-1")).thenReturn(Optional.empty());
        when(almacenRepository.buscarPorNombre("Principal")).thenReturn(List.of(almacen));
        when(productoService.create(any())).thenReturn(respuesta(id));
        when(productoRepository.findById(id)).thenReturn(Optional.of(Producto.builder().id(id).codigoSku("A-1").build()));

        service.importar(lote(fila("A-1", "Plancha", new BigDecimal("6"), "Principal")));

        ArgumentCaptor<KardexMovimientoRequestDTO> captor = ArgumentCaptor.forClass(KardexMovimientoRequestDTO.class);
        verify(kardexService).registrarMovimiento(captor.capture());
        assertEquals(TipoMovimiento.ENTRADA, captor.getValue().tipoMovimiento());
        assertEquals(TipoReferencia.AJUSTE, captor.getValue().tipoReferencia());
        assertEquals(almacen, captor.getValue().almacen());
        assertEquals(new BigDecimal("6"), captor.getValue().cantidadMovida());
    }

    @Test
    void importar_skuExistente_actualizaSinStock() {
        Producto existente = Producto.builder().id(UUID.randomUUID()).codigoSku("A-1").nombre("Viejo")
                .precioCompra(new BigDecimal("10")).precioVentaBase(new BigDecimal("20"))
                .tipo(TipoProducto.FISICO).activo(true).esFraccionable(false).build();
        when(productoRepository.findByCodigoSkuIgnoreCase("A-1")).thenReturn(Optional.of(existente));

        ProductoImportacionResultadoDTO resultado = service.importar(lote(fila("A-1", "Nuevo nombre", null, null)));

        assertEquals(0, resultado.creados());
        assertEquals(1, resultado.actualizados());
        verify(productoService).update(any(), any());
        verify(productoService, never()).create(any());
        verify(kardexService, never()).registrarMovimiento(any());
    }

    @Test
    void importar_skuExistenteConStockInicial_rechazaSinActualizar() {
        Producto existente = Producto.builder().id(UUID.randomUUID()).codigoSku("A-1").nombre("Viejo")
                .precioCompra(BigDecimal.ONE).precioVentaBase(BigDecimal.TEN).tipo(TipoProducto.FISICO).build();
        when(productoRepository.findByCodigoSkuIgnoreCase("A-1")).thenReturn(Optional.of(existente));

        assertThrows(ImportacionRechazadaException.class,
                () -> service.importar(lote(fila("A-1", "Viejo", new BigDecimal("2"), "Principal"))));

        verify(productoService, never()).update(any(), any());
        verify(kardexService, never()).registrarMovimiento(any());
    }

    @Test
    void importar_fallaAlPersistirLaSegunda_informaLaFilaYElMotivo() {
        when(productoRepository.findByCodigoSkuIgnoreCase(any())).thenReturn(Optional.empty());
        when(productoService.create(any()))
                .thenReturn(respuesta(UUID.randomUUID()))
                .thenThrow(new ValidationException("precio de venta inválido"));

        ImportacionRechazadaException ex = assertThrows(ImportacionRechazadaException.class, () -> service.importar(lote(
                fila("A-1", "Uno", null, null),
                fila("A-2", "Dos", null, null))));

        assertEquals(2, ex.getErrores().get(0).fila());
        assertEquals("precio de venta inválido", ex.getErrores().get(0).motivo());
        verify(kardexService, never()).registrarMovimiento(any());
    }

    private ProductoImportacionRequestDTO lote(ProductoImportacionFilaDTO... filas) {
        return new ProductoImportacionRequestDTO(List.of(filas));
    }

    private ProductoImportacionFilaDTO fila(String sku, String nombre, BigDecimal stock, String almacen) {
        return new ProductoImportacionFilaDTO(sku, nombre, null, null, "FISICO",
                new BigDecimal("10.00"), new BigDecimal("20.00"), "UNID", false, true, true, "10", null, stock, almacen);
    }

    private ProductoResponseDTO respuesta(UUID id) {
        return new ProductoResponseDTO(id, null, null, "A-1", "Uno", null, TipoProducto.FISICO,
                BigDecimal.ONE, BigDecimal.TEN, "UNID", false, true, null, true, "10", List.of(), null, false);
    }
}
