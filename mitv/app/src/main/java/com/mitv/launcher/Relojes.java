package com.mitv.launcher;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/** Zonas horarias: hora local (automática o a mano) y relojes extra. */
final class Relojes {
    static final String[] ZONAS = {
            "Atlanta / Nueva York (Este)|America/New_York", "Chicago (Central)|America/Chicago",
            "Denver (Montaña)|America/Denver", "Phoenix|America/Phoenix",
            "Los Ángeles (Pacífico)|America/Los_Angeles", "Alaska|America/Anchorage", "Hawái|Pacific/Honolulu",
            "Honduras|America/Tegucigalpa", "Guatemala|America/Guatemala", "El Salvador|America/El_Salvador",
            "Nicaragua|America/Managua", "Costa Rica|America/Costa_Rica", "Panamá|America/Panama",
            "México|America/Mexico_City", "Cancún|America/Cancun", "Puerto Rico|America/Puerto_Rico",
            "Rep. Dominicana|America/Santo_Domingo", "Cuba|America/Havana", "Colombia|America/Bogota",
            "Venezuela|America/Caracas", "Ecuador|America/Guayaquil", "Perú|America/Lima",
            "Bolivia|America/La_Paz", "Chile|America/Santiago", "Argentina|America/Argentina/Buenos_Aires",
            "Brasil (São Paulo)|America/Sao_Paulo", "España|Europe/Madrid", "Reino Unido|Europe/London",
            "Alemania|Europe/Berlin", "Japón|Asia/Tokyo"
    };

    static String nombre(int i) {
        return ZONAS[i].split("\\|")[0];
    }

    static String id(int i) {
        return ZONAS[i].split("\\|")[1];
    }

    static String hora(TimeZone tz) {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.US);
        f.setTimeZone(tz);
        return f.format(new java.util.Date());
    }

    static int indice(String id) {
        for (int i = 0; i < ZONAS.length; i++) if (id(i).equals(id)) return i;
        return -1;
    }

    static TimeZone zonaLocal(Context c) {
        String z = Datos.p(c).getString("zona_local", "auto");
        return z.equals("auto") ? TimeZone.getDefault() : TimeZone.getTimeZone(z);
    }

    static String nombreLocal(Context c) {
        String z = Datos.p(c).getString("zona_local", "auto");
        if (z.equals("auto")) return "Automática";
        for (int i = 0; i < ZONAS.length; i++) if (id(i).equals(z)) return nombre(i);
        return z;
    }

    static List<String[]> extras(Context c) {
        String s = Datos.p(c).getString("extras", "Honduras|America/Tegucigalpa");
        List<String[]> l = new ArrayList<>();
        if (s.isEmpty()) return l;
        for (String it : s.split(";")) {
            String[] a = it.split("\\|");
            if (a.length == 2) l.add(a);
        }
        return l;
    }

    static void guardar(Context c, List<String[]> l) {
        StringBuilder b = new StringBuilder();
        for (String[] a : l) {
            if (b.length() > 0) b.append(';');
            b.append(a[0]).append('|').append(a[1]);
        }
        Datos.p(c).edit().putString("extras", b.toString()).apply();
    }
}
