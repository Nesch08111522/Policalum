package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.DiaAtencionDTO;
import com.policalum.erp.application.dto.response.AtencionPublicaDTO;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Traduce la semana configurada al estado de hoy, en hora de Lima. */
public final class AtencionHorario {

    private AtencionHorario() {
    }

    public static AtencionPublicaDTO evaluar(List<DiaAtencionDTO> dias, LocalDateTime ahora) {
        if (dias == null || dias.isEmpty()) {
            return new AtencionPublicaDTO(false, false, false, null, null, null, "El horario de atención todavía no está publicado.");
        }
        String clave = clave(ahora.getDayOfWeek());
        DiaAtencionDTO hoy = dias.stream().filter(dia -> clave.equals(dia.dia())).findFirst().orElse(null);
        if (hoy == null || !Boolean.TRUE.equals(hoy.abierto()) || enBlanco(hoy.desde()) || enBlanco(hoy.hasta())) {
            return new AtencionPublicaDTO(true, false, false, clave, null, null, "Hoy, " + nombre(clave) + ", no atendemos.");
        }
        LocalTime desde = LocalTime.parse(hoy.desde());
        LocalTime hasta = LocalTime.parse(hoy.hasta());
        String rango = "de " + hora(desde) + " a " + hora(hasta);
        LocalTime actual = ahora.toLocalTime().withSecond(0).withNano(0);
        if (actual.isBefore(desde)) {
            return new AtencionPublicaDTO(true, true, false, clave, hoy.desde(), hoy.hasta(),
                    frase(nombre(clave), "atendemos", rango, "Aún no abrimos."));
        }
        if (!actual.isBefore(hasta)) {
            return new AtencionPublicaDTO(true, true, false, clave, hoy.desde(), hoy.hasta(),
                    frase(nombre(clave), "atendimos", rango, "Ya cerramos."));
        }
        return new AtencionPublicaDTO(true, true, true, clave, hoy.desde(), hoy.hasta(),
                frase(nombre(clave), "atendemos", rango, "Estamos abiertos."));
    }

    public static String clave(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }

    private static boolean enBlanco(String valor) {
        return valor == null || valor.isBlank();
    }

    private static String frase(String dia, String verbo, String rango, String cierre) {
        return ("Hoy, " + dia + ", " + verbo + " " + rango + ". " + cierre).replace("p. m..", "p. m.").replace("a. m..", "a. m.");
    }

    static String hora(LocalTime tiempo) {
        int hora = tiempo.getHour();
        String sufijo = hora < 12 ? "a. m." : "p. m.";
        int doce = hora % 12;
        if (doce == 0) doce = 12;
        return doce + ":" + String.format("%02d", tiempo.getMinute()) + " " + sufijo;
    }

    private static String nombre(String clave) {
        return switch (clave) {
            case "LUNES" -> "lunes";
            case "MARTES" -> "martes";
            case "MIERCOLES" -> "miércoles";
            case "JUEVES" -> "jueves";
            case "VIERNES" -> "viernes";
            case "SABADO" -> "sábado";
            case "DOMINGO" -> "domingo";
            default -> "hoy";
        };
    }
}
