package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.domain.model.Servicio;
import com.policalum.erp.infrastructure.export.ExcelEstilos;
import com.policalum.erp.infrastructure.export.ReporteSolicitud;
import com.policalum.erp.infrastructure.repository.ServicioRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ServicioImportacionServiceImpl {

    private final ServicioRepository repository;

    public byte[] plantilla() {
        ReporteSolicitud solicitud = new ReporteSolicitud(
                "Plantilla de servicios", "Servicios", "sistema",
                List.of("Columnas: nombre, descripcion, precio, activo (true/false)."),
                List.of(ReporteSolicitud.Columna.texto("nombre"), ReporteSolicitud.Columna.texto("descripcion"),
                        ReporteSolicitud.Columna.moneda("precio"), ReporteSolicitud.Columna.texto("activo")),
                List.of(List.of("Instalación de plancha", "Incluye mano de obra", new BigDecimal("80.00"), "true")));
        return ExcelEstilos.generar(solicitud);
    }

    @Transactional
    public Map<String, Integer> importar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw new IllegalArgumentException("Adjunta un archivo CSV o Excel");
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        List<String[]> filas = nombre.endsWith(".xlsx") || nombre.endsWith(".xls") ? leerExcel(archivo) : leerCsv(archivo);
        int creados = 0;
        int actualizados = 0;
        for (String[] fila : filas) {
            if (fila.length < 3 || fila[0].isBlank() || fila[0].equalsIgnoreCase("nombre")) continue;
            String titulo = fila[0].trim();
            String descripcion = fila.length > 1 ? fila[1].trim() : "";
            BigDecimal precio = new BigDecimal(fila[2].trim().replace(",", ""));
            boolean activo = fila.length < 4 || fila[3].isBlank() || Boolean.parseBoolean(fila[3].trim());
            Servicio servicio = repository.findByNombreIgnoreCase(titulo).orElseGet(Servicio::new);
            boolean nuevo = servicio.getId() == null;
            servicio.setNombre(titulo);
            servicio.setDescripcion(descripcion);
            servicio.setPrecio(precio);
            servicio.setActivo(activo);
            repository.save(servicio);
            if (nuevo) creados++; else actualizados++;
        }
        return Map.of("creados", creados, "actualizados", actualizados);
    }

    private List<String[]> leerCsv(MultipartFile archivo) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> filas = new ArrayList<>();
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (!linea.isBlank()) filas.add(linea.split(";", -1).length > 1 ? linea.split(";", -1) : linea.split(",", -1));
            }
            return filas;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el CSV");
        }
    }

    private List<String[]> leerExcel(MultipartFile archivo) {
        try (var libro = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet hoja = libro.getSheet("Datos") != null ? libro.getSheet("Datos") : libro.getSheetAt(0);
            DataFormatter formato = new DataFormatter();
            List<String[]> filas = new ArrayList<>();
            for (Row row : hoja) {
                String[] valores = new String[4];
                for (int i = 0; i < 4; i++) valores[i] = formato.formatCellValue(row.getCell(i)).trim();
                if (!valores[0].isBlank()) filas.add(valores);
            }
            return filas;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el Excel");
        }
    }
}
