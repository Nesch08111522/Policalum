package com.policalum.erp.util;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Utilidades de horarios de trabajo (días en español y turnos que cruzan medianoche). */
public final class HorarioUtil {

    public static final int MINUTOS_DIA = 1440;
    public static final int MINUTOS_SEMANA = 7 * MINUTOS_DIA;

    private static final Map<String, DayOfWeek> DIAS = Map.of(
            "LUNES", DayOfWeek.MONDAY, "MARTES", DayOfWeek.TUESDAY, "MIERCOLES", DayOfWeek.WEDNESDAY,
            "JUEVES", DayOfWeek.THURSDAY, "VIERNES", DayOfWeek.FRIDAY, "SABADO", DayOfWeek.SATURDAY,
            "DOMINGO", DayOfWeek.SUNDAY);

    private static final Map<DayOfWeek, String> NOMBRES = Map.of(
            DayOfWeek.MONDAY, "LUNES", DayOfWeek.TUESDAY, "MARTES", DayOfWeek.WEDNESDAY, "MIERCOLES",
            DayOfWeek.THURSDAY, "JUEVES", DayOfWeek.FRIDAY, "VIERNES", DayOfWeek.SATURDAY, "SABADO",
            DayOfWeek.SUNDAY, "DOMINGO");

    private HorarioUtil() { }

    /** Minutos que dura el turno; si fin <= inicio cruza la medianoche. */
    public static int duracionMinutos(LocalTime inicio, LocalTime fin) {
        int d = fin.toSecondOfDay() / 60 - inicio.toSecondOfDay() / 60;
        return d > 0 ? d : d + MINUTOS_DIA;
    }

    /** "miércoles", "MIERCOLES", "Miercoles" → DayOfWeek.WEDNESDAY. */
    public static DayOfWeek aDayOfWeek(String dia) {
        if (dia == null) throw new IllegalArgumentException("Día de la semana vacío");
        String limpio = Normalizer.normalize(dia.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        DayOfWeek d = DIAS.get(limpio);
        if (d == null) {
            throw new IllegalArgumentException("Día inválido: '" + dia + "'. Use LUNES a DOMINGO");
        }
        return d;
    }

    /** Como aDayOfWeek pero devuelve null si el texto no es un día válido (datos antiguos). */
    public static DayOfWeek aDayOfWeekONull(String dia) {
        try {
            return aDayOfWeek(dia);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** true si la lista contiene el día, tolerando tildes/mayúsculas y entradas inválidas. */
    public static boolean incluyeDia(Collection<String> dias, DayOfWeek dia) {
        if (dias == null) return false;
        for (String d : dias) if (aDayOfWeekONull(d) == dia) return true;
        return false;
    }

    public static String nombre(DayOfWeek dia) {
        return NOMBRES.get(dia);
    }

    /** Normaliza, quita duplicados y ordena de LUNES a DOMINGO. */
    public static List<String> normalizarDias(Collection<String> dias) {
        if (dias == null || dias.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un día de la semana");
        }
        Set<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
        for (String d : dias) set.add(aDayOfWeek(d));
        List<String> out = new ArrayList<>();
        for (DayOfWeek d : set) out.add(nombre(d)); // EnumSet itera en orden MONDAY..SUNDAY
        return out;
    }

    /**
     * true si dos turnos se cruzan en algún momento de la semana. Cada día se convierte
     * en un intervalo [inicio, inicio + duración) en minutos de la semana; el desplazamiento
     * de ±1 semana cubre el turno nocturno del domingo que termina el lunes.
     */
    public static boolean seCruzan(List<String> diasA, LocalTime iniA, LocalTime finA,
                                   List<String> diasB, LocalTime iniB, LocalTime finB) {
        int durA = duracionMinutos(iniA, finA);
        int durB = duracionMinutos(iniB, finB);
        for (String da : diasA) {
            DayOfWeek dA = aDayOfWeekONull(da);
            if (dA == null) continue;
            int a0 = (dA.getValue() - 1) * MINUTOS_DIA + iniA.toSecondOfDay() / 60;
            int a1 = a0 + durA;
            for (String db : diasB) {
                DayOfWeek dB = aDayOfWeekONull(db);
                if (dB == null) continue;
                int b0 = (dB.getValue() - 1) * MINUTOS_DIA + iniB.toSecondOfDay() / 60;
                for (int shift : new int[]{-MINUTOS_SEMANA, 0, MINUTOS_SEMANA}) {
                    int s0 = b0 + shift;
                    int s1 = s0 + durB;
                    if (a0 < s1 && s0 < a1) return true;
                }
            }
        }
        return false;
    }
}
