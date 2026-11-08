package com.mitv.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/** Ubicación aproximada (por red) y clima de Open-Meteo. Sin claves. */
final class Datos {
    static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("mitv", Context.MODE_PRIVATE);
    }

    /** Ciudad, estado y país una vez cada 6 horas. Con VPN activa no se cambia. */
    static void zona(Context c) {
        SharedPreferences p = p(c);
        long now = System.currentTimeMillis();
        if (p.contains("cc") && (now - p.getLong("zona_t", 0) < 6 * 3600000L || Vpn.activa(c))) return;
        try {
            JSONObject j = new JSONObject(Net.text("https://ipwho.is/"));
            if (!j.optBoolean("success")) return;
            p.edit().putString("cc", j.optString("country_code"))
                    .putString("rc", j.optString("region_code"))
                    .putString("ciudad", j.optString("city"))
                    .putFloat("lat", (float) j.optDouble("latitude", 0))
                    .putFloat("lon", (float) j.optDouble("longitude", 0))
                    .putLong("zona_t", now).apply();
        } catch (Exception ignored) {
        }
    }

    static boolean fahrenheit(Context c) {
        String u = p(c).getString("unidad", "auto");
        if (u.equals("f")) return true;
        if (u.equals("c")) return false;
        return "US".equals(p(c).getString("cc", ""));
    }

    /** Clima actual y de 7 días, cada 30 minutos. */
    static void clima(Context c) {
        SharedPreferences p = p(c);
        long now = System.currentTimeMillis();
        if (!p.contains("lat") || now - p.getLong("clima_t", 0) < 30 * 60000L) return;
        boolean f = fahrenheit(c);
        String u = "https://api.open-meteo.com/v1/forecast?latitude=" + p.getFloat("lat", 0)
                + "&longitude=" + p.getFloat("lon", 0)
                + "&current=temperature_2m,weather_code,is_day"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                + "&timezone=auto&forecast_days=7" + (f ? "&temperature_unit=fahrenheit" : "");
        try {
            String s = Net.text(u);
            new JSONObject(s);
            p.edit().putString("clima", s).putLong("clima_t", now).apply();
        } catch (Exception ignored) {
        }
    }
}
