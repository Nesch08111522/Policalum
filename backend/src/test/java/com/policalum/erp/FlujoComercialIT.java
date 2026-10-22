package com.policalum.erp;

import com.policalum.erp.application.dto.request.AperturaCajaRequestDTO;
import com.policalum.erp.application.dto.request.CompraRequestDTO;
import com.policalum.erp.application.dto.request.DetalleCompraCommandDTO;
import com.policalum.erp.application.dto.request.LineaVentaCommandDTO;
import com.policalum.erp.application.dto.request.PagoVentaCommandDTO;
import com.policalum.erp.application.dto.request.RecepcionCompraRequestDTO;
import com.policalum.erp.application.dto.request.RegistrarVentaRequestDTO;
import com.policalum.erp.application.service.CompraService;
import com.policalum.erp.application.service.TurnoCajaService;
import com.policalum.erp.application.service.VentaService;
import com.policalum.erp.domain.enums.EstadoCompra;
import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.Moneda;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.enums.TipoComprobante;
import com.policalum.erp.domain.enums.TipoProducto;
import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.domain.model.Proveedor;
import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.Stock;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.loader.DataLoader;
import com.policalum.erp.infrastructure.repository.AlmacenRepository;
import com.policalum.erp.infrastructure.repository.KardexRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;
import com.policalum.erp.infrastructure.repository.ProveedorRepository;
import com.policalum.erp.infrastructure.repository.RolRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.StockRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compra, recepción (kardex y stock), venta y caja sobre PostgreSQL efímero.
 * No se ejecuta si Docker no está disponible y no toca la base ERPpoli.
 * DataLoader exige una sede fija que no existe en una base vacía; el mock evita ese arranque.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class FlujoComercialIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    DataLoader dataLoader;

    @Autowired CompraService compraService;
    @Autowired VentaService ventaService;
    @Autowired TurnoCajaService turnoCajaService;
    @Autowired SedeRepository sedeRepository;
    @Autowired AlmacenRepository almacenRepository;
    @Autowired ProveedorRepository proveedorRepository;
    @Autowired ProductoRepository productoRepository;
    @Autowired RolRepository rolRepository;
    @Autowired UserRepository userRepository;
    @Autowired StockRepository stockRepository;
    @Autowired KardexRepository kardexRepository;

    private static final String EMAIL = "it-admin@policalum.test";

    private Sede sede;
    private Almacen almacen;
    private Producto producto;
    private UUID proveedorId;

    @BeforeEach
    void preparar() {
        Rol rol = rolRepository.save(Rol.builder().nombre(RolNombre.ROLE_ADMIN).permisosJson(Map.of()).build());
        sede = sedeRepository.save(Sede.builder().nombre("Sede IT").activa(true).build());
        User user = userRepository.save(User.builder()
                .rol(rol).sede(sede).nombreUsuario("it-admin").nombre("IT").apellido("Admin")
                .email(EMAIL).passwordHash("{noop}it").activo(true).build());
        almacen = almacenRepository.save(Almacen.builder().sede(sede).nombre("Principal").activo(true).build());
        Proveedor proveedor = proveedorRepository.save(Proveedor.builder()
                .nombreRazon("Proveedor IT").tipoDocumento("RUC").numeroDocumento("20123456789").activo(true).build());
        proveedorId = proveedor.getId();
        producto = productoRepository.save(Producto.builder()
                .proveedor(proveedor).nombre("Perfil IT").codigoSku("IT-1").tipo(TipoProducto.FISICO)
                .precioCompra(new BigDecimal("8.00")).precioVentaBase(new BigDecimal("10.00"))
                .unidad("UNID").esFraccionable(false).activo(true).incluyeIgv(true).build());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getEmail(), "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void compraRecepcionVentaYCaja() {
        var compra = compraService.crear(new CompraRequestDTO(
                proveedorId, sede.getId(), "F001-1", LocalDate.now(), Moneda.PEN, "IT",
                List.of(new DetalleCompraCommandDTO(producto.getId(), null, almacen.getId(),
                        new BigDecimal("10"), new BigDecimal("11.80")))));
        assertThat(compra.estadoCompra()).isEqualTo(EstadoCompra.BORRADOR);

        var recibida = compraService.recibir(compra.id(), new RecepcionCompraRequestDTO("F001-1", "Recepción IT"));
        assertThat(recibida.estadoCompra()).isEqualTo(EstadoCompra.RECIBIDA);
        assertThat(cantidad()).isEqualByComparingTo("10");
        assertThat(kardexRepository.count()).isEqualTo(1);

        var turno = turnoCajaService.abrir(new AperturaCajaRequestDTO(sede.getId(), new BigDecimal("100.00"), "Apertura IT"));
        var venta = ventaService.registrar(new RegistrarVentaRequestDTO(
                sede.getId(), almacen.getId(), null, turno.id(), TipoComprobante.BOLETA, null, null, null, null,
                List.of(new LineaVentaCommandDTO(producto.getId(), null, null, new BigDecimal("2"))),
                List.of(new PagoVentaCommandDTO(MetodoPago.EFECTIVO, new BigDecimal("20.00"), null))));

        assertThat(venta.total()).isEqualByComparingTo("20.00");
        assertThat(cantidad()).isEqualByComparingTo("8");
        assertThat(kardexRepository.count()).isEqualTo(2);
        assertThat(turnoCajaService.resumen(turno.id()).ventasEfectivo()).isEqualByComparingTo("20.00");
        assertThat(turnoCajaService.resumen(turno.id()).esperado()).isEqualByComparingTo("120.00");
    }

    private BigDecimal cantidad() {
        return stockRepository.findByProducto_Id(producto.getId()).stream()
                .map(Stock::getCantidad)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}
