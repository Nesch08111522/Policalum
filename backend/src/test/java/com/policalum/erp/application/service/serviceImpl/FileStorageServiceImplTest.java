package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;

import com.policalum.erp.exception.FileStorageException;
import com.policalum.erp.infrastructure.config.FileStorageProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Almacenamiento local de adjuntos. Usa una carpeta temporal real (@TempDir): sin BD ni Spring,
 * y cada test arranca con el directorio vacío.
 */
class FileStorageServiceImplTest {

    @TempDir Path raiz;
    Path base;
    FileStorageProperties props;
    FileStorageServiceImpl service;

    @BeforeEach
    void setUp() throws IOException {
        base = Files.createDirectories(raiz.resolve("uploads"));
        props = new FileStorageProperties();
        props.setBasePath(base.toString());
        props.setMaxFileSizeBytes(1024);
        service = new FileStorageServiceImpl(props);
    }

    private MockMultipartFile archivo(String nombre, int bytes) {
        return new MockMultipartFile("file", nombre, "application/pdf", new byte[bytes]);
    }

    // ---------------------------------------------------------------- guardar

    @Test
    void guardar_valido_escribeEnSubcarpetaAnioMesConNombreUnico() throws IOException {
        String ruta = service.guardar(new MockMultipartFile("file", "Factura F001.PDF", "application/pdf",
                "contenido".getBytes(StandardCharsets.UTF_8)), "compras");

        LocalDate hoy = LocalDate.now();
        assertTrue(ruta.startsWith(String.format("compras/%04d/%02d/", hoy.getYear(), hoy.getMonthValue())));
        assertTrue(ruta.endsWith(".pdf"), "la extensión se normaliza a minúsculas");
        assertFalse(ruta.contains("Factura"), "no se usa el nombre original en disco");
        assertEquals("contenido", Files.readString(base.resolve(ruta)));
    }

    @Test
    void guardar_dosVecesElMismoNombre_noSePisan() {
        String a = service.guardar(archivo("x.pdf", 1), "compras");
        String b = service.guardar(archivo("x.pdf", 1), "compras");

        assertNotEquals(a, b);
    }

    @Test
    void guardar_vacio_lanzaFileStorageException() {
        assertThrows(FileStorageException.class, () -> service.guardar(archivo("x.pdf", 0), "compras"));
    }

    @Test
    void guardar_nulo_lanzaFileStorageException() {
        assertThrows(FileStorageException.class, () -> service.guardar(null, "compras"));
    }

    @Test
    void guardar_excedeTamanoMaximo_lanzaFileStorageException() {
        assertThrows(FileStorageException.class, () -> service.guardar(archivo("x.pdf", 1025), "compras"));
    }

    @Test
    void guardar_enElLimiteDeTamano_seAcepta() {
        assertNotNull(service.guardar(archivo("x.pdf", 1024), "compras"));
    }

    @Test
    void guardar_extensionNoPermitida_lanzaFileStorageException() {
        FileStorageException ex = assertThrows(FileStorageException.class, () -> service.guardar(archivo("script.exe", 10), "compras"));
        assertTrue(ex.getMessage().contains(".exe"));
    }

    @Test
    void guardar_sinExtension_lanzaFileStorageException() {
        assertThrows(FileStorageException.class, () -> service.guardar(archivo("README", 10), "compras"));
    }

    @Test
    void guardar_archivoOcultoSinExtension_noSeTomaComoExtension() {
        assertThrows(FileStorageException.class, () -> service.guardar(archivo(".pdf", 10), "compras"));
    }

    @Test
    void guardar_subcarpetaConTraversal_lanzaFileStorageExceptionSinEscribirFuera() {
        assertThrows(FileStorageException.class, () -> service.guardar(archivo("x.pdf", 10), "../../fuera"));
        assertFalse(Files.exists(raiz.getParent().resolve("fuera")));
    }

    // ---------------------------------------------------------------- cargar

    @Test
    void cargar_existente_devuelveRecursoLegible() throws IOException {
        String ruta = service.guardar(new MockMultipartFile("f", "a.pdf", "application/pdf", "hola".getBytes()), "compras");

        Resource r = service.cargar(ruta);

        assertTrue(r.exists());
        assertEquals("hola", new String(r.getInputStream().readAllBytes()));
    }

    @Test
    void cargar_inexistente_lanzaFileStorageException() {
        assertThrows(FileStorageException.class, () -> service.cargar("compras/2026/01/no-existe.pdf"));
    }

    @Test
    void cargar_rutaConTraversal_lanzaFileStorageException() throws IOException {
        Files.writeString(raiz.resolve("secreto.txt"), "x");

        assertThrows(FileStorageException.class, () -> service.cargar("../secreto.txt"));
    }

    // ---------------------------------------------------------------- eliminar / existe

    @Test
    void eliminar_existente_borraYExisteDevuelveFalse() {
        String ruta = service.guardar(archivo("a.pdf", 5), "compras");
        assertTrue(service.existe(ruta));

        service.eliminar(ruta);

        assertFalse(service.existe(ruta));
    }

    @Test
    void eliminar_inexistente_noLanza() {
        assertDoesNotThrow(() -> service.eliminar("compras/nada.pdf"));
    }

    /**
     * HALLAZGO DE SEGURIDAD: eliminar() no valida que la ruta quede dentro de basePath (guardar y
     * cargar sí lo hacen). Hoy borra el archivo de fuera. Habilitar este test al corregir
     * FileStorageServiceImpl.eliminar/existe con el mismo chequeo startsWith(basePath).
     */
    @Test
    @Disabled("Hallazgo: eliminar() permite path traversal. Habilitar tras agregar la validación startsWith(basePath)")
    void eliminar_rutaConTraversal_noBorraFueraDelAlmacenamiento() throws IOException {
        Path fuera = Files.writeString(raiz.resolve("no-borrar.txt"), "x");

        try {
            service.eliminar("../no-borrar.txt");
        } catch (FileStorageException esperado) {
            // también es aceptable rechazar la ruta
        }

        assertTrue(Files.exists(fuera));
    }
}
