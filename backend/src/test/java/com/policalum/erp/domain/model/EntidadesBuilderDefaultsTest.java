package com.policalum.erp.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Guardián del bug "@Builder ignora los inicializadores" (M5, M10, M13, M11).
 *
 * Para cada @Entity con builder(): si un campo tiene valor por defecto al usar new X(), debe
 * seguir teniéndolo con X.builder().build() + @PrePersist (que es lo que pasa cuando un
 * servicio o un mapper de MapStruct crea la entidad). Si alguien agrega un campo con
 * inicializador y olvida el @PrePersist, este test falla.
 * Las colecciones se excluyen: Hibernate las inicializa al cargar.
 */
class EntidadesBuilderDefaultsTest {

    @Test
    void builderMasPrePersist_conservaLosValoresPorDefecto() throws Exception {
        List<String> faltantes = new ArrayList<>();
        int revisadas = 0;
        for (Class<?> clase : entidades()) {
            Method builder;
            try {
                builder = clase.getMethod("builder");
            } catch (NoSuchMethodException e) {
                continue;
            }
            Object conNew = clase.getDeclaredConstructor().newInstance();
            Object b = builder.invoke(null);
            Object conBuilder = b.getClass().getMethod("build").invoke(b);
            for (Method m : clase.getDeclaredMethods()) {
                if (m.isAnnotationPresent(PrePersist.class)) {
                    m.setAccessible(true);
                    m.invoke(conBuilder);
                }
            }
            revisadas++;
            for (Field f : clase.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()
                        || Collection.class.isAssignableFrom(f.getType()) || Map.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                if (f.get(conNew) != null && f.get(conBuilder) == null) {
                    faltantes.add(clase.getSimpleName() + "." + f.getName());
                }
            }
        }
        assertTrue(revisadas > 20, "No se encontraron las entidades (" + revisadas + ")");
        assertTrue(faltantes.isEmpty(), "Campos con valor por defecto que quedan NULL al crear con builder: " + faltantes
                + ". Agregue 'if (this.campo == null) this.campo = ...;' en el @PrePersist de la entidad.");
    }

    private static List<Class<?>> entidades() throws Exception {
        String paquete = EntidadesBuilderDefaultsTest.class.getPackageName();
        URL url = EntidadesBuilderDefaultsTest.class.getClassLoader().getResource(paquete.replace('.', '/'));
        List<Class<?>> clases = new ArrayList<>();
        // En Maven, test-classes y classes son carpetas distintas: se toma la de las entidades
        URL urlEntidad = Venta.class.getProtectionDomain().getCodeSource().getLocation();
        File dir = new File(new File(urlEntidad.toURI()), paquete.replace('.', '/'));
        File[] archivos = dir.listFiles((d, n) -> n.endsWith(".class") && !n.contains("$"));
        assertNotNull(archivos, "No se pudo listar " + dir + " (url de prueba: " + url + ")");
        for (File a : archivos) {
            Class<?> c = Class.forName(paquete + "." + a.getName().replace(".class", ""));
            if (c.isAnnotationPresent(Entity.class)) clases.add(c);
        }
        return clases;
    }
}
