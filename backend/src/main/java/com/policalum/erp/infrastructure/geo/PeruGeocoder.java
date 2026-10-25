package com.policalum.erp.infrastructure.geo;

import java.util.Locale;
import java.util.Map;

/**
 * Gazetter local de ciudades del Perú. No llama a internet.
 * La Libertad está detallada porque las sedes operan en esa región:
 * cada distrito tiene su propio punto y no se apilan en Trujillo.
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
            Map.entry("puerto maldonado", new double[] {-12.5930, -69.1890}),
            Map.entry("el porvenir", new double[] {-8.0839, -79.0031}),
            Map.entry("florencia de mora", new double[] {-8.0828, -79.0242}),
            Map.entry("la esperanza", new double[] {-8.0706, -79.0464}),
            Map.entry("huanchaco", new double[] {-8.0814, -79.1214}),
            Map.entry("victor larco herrera", new double[] {-8.1292, -79.0472}),
            Map.entry("victor larco", new double[] {-8.1292, -79.0472}),
            Map.entry("moche", new double[] {-8.1714, -79.0092}),
            Map.entry("laredo", new double[] {-8.0897, -78.9603}),
            Map.entry("salaverry", new double[] {-8.2214, -78.9764}),
            Map.entry("poroto", new double[] {-8.0111, -78.7669}),
            Map.entry("simbal", new double[] {-7.9736, -78.8131}),
            Map.entry("el milagro", new double[] {-8.0558, -79.0675}),
            Map.entry("alto trujillo", new double[] {-8.0694, -78.9906}),
            Map.entry("ascope", new double[] {-7.7144, -79.1078}),
            Map.entry("chocope", new double[] {-7.7914, -79.2217}),
            Map.entry("casa grande", new double[] {-7.7411, -79.1883}),
            Map.entry("paijan", new double[] {-7.7336, -79.3028}),
            Map.entry("chicama", new double[] {-7.8406, -79.1464}),
            Map.entry("santiago de cao", new double[] {-7.9583, -79.2394}),
            Map.entry("magdalena de cao", new double[] {-7.8806, -79.2833}),
            Map.entry("razuri", new double[] {-7.7014, -79.4375}),
            Map.entry("cartavio", new double[] {-7.8903, -79.2211}),
            Map.entry("chepen", new double[] {-7.2275, -79.4286}),
            Map.entry("pacanga", new double[] {-7.1664, -79.4831}),
            Map.entry("pueblo nuevo", new double[] {-7.1881, -79.5169}),
            Map.entry("pacasmayo", new double[] {-7.4006, -79.5714}),
            Map.entry("san pedro de lloc", new double[] {-7.4286, -79.5036}),
            Map.entry("guadalupe", new double[] {-7.2431, -79.4708}),
            Map.entry("jequetepeque", new double[] {-7.3372, -79.5631}),
            Map.entry("viru", new double[] {-8.4153, -78.7522}),
            Map.entry("chao", new double[] {-8.5372, -78.6772}),
            Map.entry("guadalupito", new double[] {-8.4319, -78.6403}),
            Map.entry("otuzco", new double[] {-7.9022, -78.5656}),
            Map.entry("huamachuco", new double[] {-7.8122, -78.0489}),
            Map.entry("santiago de chuco", new double[] {-8.1458, -78.1733}),
            Map.entry("quiruvilca", new double[] {-8.0014, -78.3092}),
            Map.entry("cascas", new double[] {-7.4781, -78.8194}),
            Map.entry("julcan", new double[] {-8.0428, -78.4861}),
            Map.entry("bolivar", new double[] {-7.1531, -77.7008}),
            Map.entry("tayabamba", new double[] {-8.2753, -77.2967}),
            Map.entry("parcoy", new double[] {-8.0333, -77.4833})
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
