package com.mitv.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Lista central de paisajes (generada en GitHub, ver mitv/fondos/generar.py).
 * Solo se guarda la lista de texto; las fotos nunca se guardan en el almacenamiento.
 * Todas las teles de la misma zona calculan la misma foto con la hora: quedan sincronizadas.
 */
final class Fondos {
    static final String BASE = "https://raw.githubusercontent.com/hayroantoniopineda-code/tv/main/fondos/";

    static final class Foto {
        String u, a, l;
    }

    private static String clave;
    private static List<Foto> pool = new ArrayList<>();

    static List<String> archivos(Context c) {
        SharedPreferences p = Datos.p(c);
        String modo = p.getString("zona_fondos", "auto");
        String cc = p.getString("cc", "");
        String rc = p.getString("rc", "");
        List<String> l = new ArrayList<>();
        if (!modo.equals("mundo") && !cc.isEmpty()) {
            if (modo.equals("auto") && !rc.isEmpty()) l.add(cc + "-" + rc);
            l.add(cc);
        }
        l.add("MUNDO");
        return l;
    }

    private static File archivo(Context c, String a) {
        return new File(c.getFilesDir(), "f_" + a + ".json");
    }

    /** En segundo plano: baja las listas cada 12 horas (unos KB). */
    static void refrescar(Context c) {
        SharedPreferences p = Datos.p(c);
        long now = System.currentTimeMillis();
        if (now - p.getLong("cfg_t", 0) > 12 * 3600000L) {
            try {
                JSONObject cfg = new JSONObject(Net.text(BASE + "config.json"));
                p.edit().putInt("intervalo_central", cfg.optInt("intervalo", 120)).putLong("cfg_t", now).apply();
            } catch (Exception ignored) {
            }
        }
        for (String a : archivos(c)) {
            File f = archivo(c, a);
            if (f.exists() && now - f.lastModified() < 12 * 3600000L) continue;
            String s;
            try {
                s = Net.text(BASE + a + ".json");
                new JSONArray(s);
            } catch (FileNotFoundException e) {
                s = "[]";
            } catch (Exception e) {
                continue;
            }
            try (FileOutputStream o = new FileOutputStream(f)) {
                o.write(s.getBytes("UTF-8"));
            } catch (Exception ignored) {
            }
        }
    }

    static String periodo(Context c) {
        Calendar k = Calendar.getInstance(Relojes.zonaLocal(c));
        int h = k.get(Calendar.HOUR_OF_DAY);
        if (h >= 5 && h < 12) return "m";
        if (h >= 12 && h < 18) return "t";
        return "n";
    }

    static String nombrePeriodo(Context c) {
        String p = periodo(c);
        return p.equals("m") ? "Mañana" : p.equals("t") ? "Tarde" : "Noche";
    }

    /** o=otoño, i=invierno, p=primavera, v=verano, x=trópico. */
    static char estacion(Context c) {
        float lat = Datos.p(c).getFloat("lat", 30f);
        if (Math.abs(lat) < 23.5f) return 'x';
        int m = Calendar.getInstance().get(Calendar.MONTH);
        if (lat < 0) m = (m + 6) % 12;
        if (m >= 2 && m <= 4) return 'p';
        if (m >= 5 && m <= 7) return 'v';
        if (m >= 8 && m <= 10) return 'o';
        return 'i';
    }

    static int intervalo(Context c) {
        SharedPreferences p = Datos.p(c);
        int v = p.getInt("intervalo", 0);
        if (v <= 0) v = p.getInt("intervalo_central", 120);
        return Math.max(15, v);
    }

    private static JSONArray leer(Context c, String a) {
        File f = archivo(c, a);
        if (!f.exists()) return new JSONArray();
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] b = new byte[(int) f.length()];
            int off = 0, n;
            while (off < b.length && (n = in.read(b, off, b.length - off)) > 0) off += n;
            return new JSONArray(new String(b, 0, off, "UTF-8"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static boolean okPer(String per, String t) {
        if (per.equals("n")) return t.equals("n");
        return t.equals(per) || t.equals("d");
    }

    private static boolean okEst(char est, String s) {
        if (s.isEmpty()) return true;
        if (est == 'x') return s.equals("p") || s.equals("v");
        return s.charAt(0) == est;
    }

    private static List<Foto> construir(Context c, List<String> arch, String per, char est) {
        for (int nivel = 0; nivel < 3; nivel++) {
            List<List<Foto>> ls = new ArrayList<>();
            int total = 0;
            for (String a : arch) {
                JSONArray j = leer(c, a);
                List<Foto> l = new ArrayList<>();
                for (int i = 0; i < j.length(); i++) {
                    JSONObject o = j.optJSONObject(i);
                    if (o == null) continue;
                    if (nivel < 2 && !okPer(per, o.optString("t", "d"))) continue;
                    if (nivel < 1 && !okEst(est, o.optString("s", ""))) continue;
                    Foto f = new Foto();
                    f.u = o.optString("u");
                    f.a = o.optString("a");
                    f.l = o.optString("l");
                    if (!f.u.isEmpty()) l.add(f);
                }
                ls.add(l);
                total += l.size();
            }
            if (total >= 8 || nivel == 2) {
                List<Foto> r = new ArrayList<>(total);
                for (int i = 0; ; i++) {
                    boolean hay = false;
                    for (List<Foto> l : ls) {
                        if (i < l.size()) {
                            r.add(l.get(i));
                            hay = true;
                        }
                    }
                    if (!hay) break;
                }
                return r;
            }
        }
        return new ArrayList<>();
    }

    /** La foto que toca ahora (igual en todas las teles de la zona). */
    static synchronized Foto actual(Context c) {
        String per = periodo(c);
        char est = estacion(c);
        List<String> arch = archivos(c);
        long ver = 0;
        for (String a : arch) ver += archivo(c, a).lastModified();
        String k = per + est + arch + ver;
        if (!k.equals(clave)) {
            pool = construir(c, arch, per, est);
            clave = k;
        }
        if (pool.isEmpty()) return null;
        long slot = System.currentTimeMillis() / 1000L / intervalo(c);
        return pool.get((int) (slot % pool.size()));
    }
}
