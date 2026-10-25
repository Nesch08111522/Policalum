package com.policalum.erp.infrastructure.geo;

import java.util.Locale;
import java.util.Map;

/**
 * Gazetter local de ciudades del Perú. No llama a internet.
 * Photon o Nominatim en Docker quedan para cuando haya servidor propio.
 */
public final class PeruGeocoder {

    private static final Map<String, double[]> CIUDADES = Map.ofEntries(
            Map.entry("lima", new double[] {-12.0464, -77.0428}),
            Map.entry("callao", new double[] {-12.0566, -77.1181}),
            Map.entry("arequipa", new double[] {-16.4090, -71.5375}),
            Map.entry("trujillo", new double[] {-8.1116, -79.0288}),
            Map.entry("cusco", new double[] {-13.5319, -71.9675}),
            Map.entry("cuzco", new double[] {-13.5319, -71.9675}),
            Map.entry("piura", new double[] {-5.1945, -80.6328}),
            Map.entry("chiclayo", new double[] {-6.7714, -79.8409}),
            Map.entry("iquitos", new double[] {-3.7437, -73.2516}),
            Map.entry("huancayo", new double[] {-12.0651, -75.2045}),
            Map.entry("tacna", new double[] {-18.0066, -70.2463}),
            Map.entry("chimbote", new double[] {-9.0745, -78.5936}),
            Map.entry("pucallpa", new double[] {-8.3791, -74.5539}),
            Map.entry("juliaca", new double[] {-15.4997, -70.1297}),
            Map.entry("ica", new double[] {-14.0755, -75.7342}),
            Map.entry("cajamarca", new double[] {-7.1617, -78.5128}),
            Map.entry("puno", new double[] {-15.8402, -70.0219}),
            Map.entry("ayacucho", new double[] {-13.1631, -74.2246}),
            Map.entry("huanuco", new double[] {-9.9306, -76.2422}),
            Map.entry("miraflores", new double[] {-12.1211, -77.0297}),
            Map.entry("san isidro", new double[] {-12.0969, -77.0365}),
            Map.entry("barranco", new double[] {-12.1439, -77.0203}),
            Map.entry("santiago de surco", new double[] {-12.1460, -76.9910}),
            Map.entry("surco", new double[] {-12.1460, -76.9910}),
            Map.entry("san borja", new double[] {-12.0960, -76.9956}),
            Map.entry("la molina", new double[] {-12.0796, -76.9163}),
            Map.entry("surquillo", new double[] {-12.1136, -77.0120}),
            Map.entry("san miguel", new double[] {-12.0778, -77.0936}),
            Map.entry("los olivos", new double[] {-11.9590, -77.0708}),
            Map.entry("san juan de lurigancho", new double[] {-11.9910, -76.9990}),
            Map.entry("san juan de miraflores", new double[] {-12.1600, -76.9700}),
            Map.entry("villa el salvador", new double[] {-12.2130, -76.9370}),
            Map.entry("chorrillos", new double[] {-12.1760, -77.0170}),
            Map.entry("ate", new double[] {-12.0260, -76.9170}),
            Map.entry("ventanilla", new double[] {-11.8770, -77.1260}),
            Map.entry("huaraz", new double[] {-9.5278, -77.5278}),
            Map.entry("tarapoto", new double[] {-6.4825, -76.3730}),
            Map.entry("tumbes", new double[] {-3.5669, -80.4515}),
            Map.entry("huacho", new double[] {-11.1070, -77.6050}),
            Map.entry("chincha", new double[] {-13.4170, -76.1320}),
            Map.entry("pisco", new double[] {-13.7100, -76.2030}),
            Map.entry("moquegua", new double[] {-17.1930, -70.9350}),
            Map.entry("puerto maldonado", new double[] {-12.5930, -69.1890})
    );

    private PeruGeocoder() {}

    /** Latitud y longitud, o null si ninguna ciudad del gazetter aparece en los textos. */
    public static double[] buscar(String ciudad, String direccion, String nombre) {
        String texto = normalizar((ciudad == null ? "" : ciudad) + " " + (direccion == null ? "" : direccion) + " " + (nombre == null ? "" : nombre));
        if (texto.isBlank()) return null;
        double[] mejor = null;
        int largo = -1;
        for (var entrada : CIUDADES.entrySet()) {
            String clave = normalizar(entrada.getKey());
            if (contienePalabra(texto, clave) && clave.length() > largo) {
                mejor = entrada.getValue();
                largo = clave.length();
            }
        }
        return mejor;
    }

    private static boolean contienePalabra(String texto, String clave) {
        int desde = 0;
        while (desde <= texto.length() - clave.length()) {
            int indice = texto.indexOf(clave, desde);
            if (indice < 0) return false;
            boolean izquierda = indice == 0 || !Character.isLetterOrDigit(texto.charAt(indice - 1));
            int fin = indice + clave.length();
            boolean derecha = fin == texto.length() || !Character.isLetterOrDigit(texto.charAt(fin));
            if (izquierda && derecha) return true;
            desde = indice + 1;
        }
        return false;
    }

    private static String normalizar(String valor) {
        return valor.toLowerCase(Locale.ROOT)
                .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u');
    }
}
