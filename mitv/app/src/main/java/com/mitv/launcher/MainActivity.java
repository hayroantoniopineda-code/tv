package com.mitv.launcher;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class MainActivity extends Activity {
    static final int COLS = 8;
    static final long MANTENER_MS = 2000;
    private Pantalla pan;
    private Panel panel;
    private float u;
    private LinearLayout nav, grid;
    private ScrollView appsScroll;
    private TextView titApps;
    private View dot, velo, vpnBoton, ajustesBoton;
    private ObjectAnimator pulso;
    private boolean enReposo, expandido;
    private int totalApps, moverIdx = -1;
    private List<App> lista = new ArrayList<>();
    private long tPulsado;
    private boolean menuSalio;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable aReposo = this::entrarReposo;
    private final Runnable vigilarVpn = new Runnable() {
        @Override
        public void run() {
            actualizarVpn();
            h.postDelayed(this, 5000);
        }
    };

    static final class App {
        String nombre, pkg;
        Drawable banner, icono;
        Intent intent;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        pan = new Pantalla(this, false);
        u = pan.u;
        construirNav();
        construirApps();
        panel = new Panel(this, u);
        pan.addView(panel, new FrameLayout.LayoutParams(-1, -1));
        setContentView(pan);
    }

    // ================= barra superior =================
    private GradientDrawable pildora(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(30 * u);
        g.setColor(color);
        return g;
    }

    private void construirNav() {
        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = Pantalla.txt(this, u, 34, 0xFFFFFFFF, true);
        logo.setText("Mi TV");
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-2, -2);
        ll.rightMargin = (int) (50 * u);
        nav.addView(logo, ll);
        String[] items = {"Inicio", "Apps", "En vivo", "Películas", "Series", "VPN", "Ajustes"};
        for (final String it : items) {
            LinearLayout v = new LinearLayout(this);
            v.setOrientation(LinearLayout.HORIZONTAL);
            v.setGravity(Gravity.CENTER_VERTICAL);
            v.setPadding((int) (22 * u), (int) (11 * u), (int) (22 * u), (int) (11 * u));
            final TextView t = Pantalla.txt(this, u, 24, 0xFFFFFFFF, true);
            t.setText(it);
            v.addView(t);
            if (it.equals("VPN")) {
                vpnBoton = v;
                dot = new View(this);
                GradientDrawable g = new GradientDrawable();
                g.setShape(GradientDrawable.OVAL);
                g.setColor(0x66FFFFFF);
                dot.setBackground(g);
                LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams((int) (10 * u), (int) (10 * u));
                dl.leftMargin = (int) (8 * u);
                dl.bottomMargin = (int) (14 * u);
                v.addView(dot, dl);
                pulso = ObjectAnimator.ofPropertyValuesHolder(dot,
                        PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.3f),
                        PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.45f),
                        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.45f));
                pulso.setDuration(1500);
                pulso.setRepeatCount(ValueAnimator.INFINITE);
                pulso.setRepeatMode(ValueAnimator.REVERSE);
            }
            if (it.equals("Ajustes")) ajustesBoton = v;
            final boolean sel = it.equals("Inicio");
            v.setFocusable(true);
            v.setBackground(pildora(sel ? 0x33FFFFFF : 0));
            v.setOnFocusChangeListener((x, f) -> {
                x.setBackground(pildora(f ? 0xF2FFFFFF : sel ? 0x33FFFFFF : 0));
                t.setTextColor(f ? 0xFF14162A : 0xFFFFFFFF);
                t.setShadowLayer(f ? 0 : 4 * u, 0, f ? 0 : 2 * u, 0x88000000);
            });
            v.setOnClickListener(x -> accion(it));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = (int) (6 * u);
            nav.addView(v, lp);
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.LEFT);
        lp.leftMargin = (int) (76 * u);
        lp.topMargin = (int) (38 * u);
        pan.addView(nav, lp);
    }

    private void accion(String it) {
        switch (it) {
            case "Inicio":
                colapsar();
                enfocarTile(0);
                break;
            case "Apps":
            case "Películas":
            case "Series":
                expandir();
                enfocarTile(totalApps > COLS ? COLS : 0);
                break;
            case "En vivo":
                String[] vivos = {"com.google.android.tv", "com.google.android.youtube.tvunplugged", "tv.pluto.android", "com.tubitv"};
                for (String p : vivos) {
                    Intent i = Vpn.lanzar(this, p);
                    if (i != null) {
                        abrir(i);
                        return;
                    }
                }
                Toast.makeText(this, "Instala una app de TV en vivo (Pluto TV, Tubi, YouTube TV…)", Toast.LENGTH_LONG).show();
                break;
            case "VPN":
                panel.abrir(this::paginaVpn, () -> vpnBoton.requestFocus());
                break;
            case "Ajustes":
                panel.abrir(this::paginaAjustes, () -> ajustesBoton.requestFocus());
                break;
        }
    }

    private void actualizarVpn() {
        boolean on = Vpn.activa(this);
        ((GradientDrawable) dot.getBackground()).setColor(on ? 0xFF46D778 : 0x66FFFFFF);
        if (on && !pulso.isStarted()) pulso.start();
        if (!on && pulso.isStarted()) {
            pulso.cancel();
            dot.setAlpha(1f);
            dot.setScaleX(1f);
            dot.setScaleY(1f);
        }
    }

    // ================= apps: 8 visibles, ▼ para ver todas =================
    private int altoColapsado() {
        return (int) (205 * u);
    }

    private void construirApps() {
        velo = new View(this);
        velo.setBackgroundColor(0xE6080A1C);
        velo.setAlpha(0f);
        velo.setVisibility(View.GONE);
        pan.addView(velo, new FrameLayout.LayoutParams(-1, -1));

        appsScroll = new ScrollView(this);
        appsScroll.setVerticalScrollBarEnabled(false);
        appsScroll.setClipToPadding(false);
        appsScroll.setClipChildren(false);
        appsScroll.setSmoothScrollingEnabled(true);
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        grid.setPadding((int) (76 * u), 0, (int) (76 * u), (int) (20 * u));
        appsScroll.addView(grid);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, altoColapsado(), Gravity.BOTTOM);
        lp.bottomMargin = (int) (40 * u);
        pan.addView(appsScroll, lp);
    }

    private List<App> listar() {
        PackageManager pm = getPackageManager();
        List<App> tv = new ArrayList<>(), otras = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || !vistos.add(pkg)) continue;
                App a = new App();
                if (ocultas().contains(pkg)) continue;
                a.pkg = pkg;
                a.nombre = String.valueOf(ri.loadLabel(pm));
                a.intent = new Intent(Intent.ACTION_MAIN).addCategory(cat).setClassName(pkg, ri.activityInfo.name);
                a.banner = ri.activityInfo.loadBanner(pm);
                if (a.banner == null) a.banner = ri.activityInfo.applicationInfo.loadBanner(pm);
                if (a.banner == null) a.icono = ri.loadIcon(pm);
                (cat.equals(Intent.CATEGORY_LEANBACK_LAUNCHER) ? tv : otras).add(a);
            }
        }
        Collections.sort(tv, (x, y) -> x.nombre.compareToIgnoreCase(y.nombre));
        Collections.sort(otras, (x, y) -> x.nombre.compareToIgnoreCase(y.nombre));
        tv.addAll(otras);
        final List<String> orden = orden();
        List<App> r = new ArrayList<>();
        for (String pk : orden)
            for (App a : tv)
                if (a.pkg.equals(pk)) r.add(a);
        for (App a : tv) if (!orden.contains(a.pkg)) r.add(a);
        return r;
    }

    private void cargarApps() {
        Pantalla.BG.execute(() -> {
            final List<App> l = listar();
            h.post(() -> {
                lista = l;
                llenarGrid(l);
            });
        });
    }

    private void llenarGrid(List<App> l) {
        View foco = getCurrentFocus();
        boolean teniaFoco = foco != null && foco.getTag() instanceof Integer;
        grid.removeAllViews();
        totalApps = l.size();
        LinearLayout cab = new LinearLayout(this);
        cab.setOrientation(LinearLayout.HORIZONTAL);
        cab.setGravity(Gravity.BOTTOM);
        titApps = Pantalla.txt(this, u, 25, 0xFFFFFFFF, true);
        cab.addView(titApps);
        TextView pista = Pantalla.txt(this, u, 15, 0x99FFFFFF, false);
        pista.setText(totalApps > COLS ? "      ▼  Baja para ver todas (" + totalApps + ")" : "");
        cab.addView(pista);
        grid.addView(cab);
        ponerTitulo();
        LinearLayout fila = null;
        for (int i = 0; i < l.size(); i++) {
            if (i % COLS == 0) {
                fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                fila.setClipChildren(false);
                LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(-1, -2);
                fl.topMargin = (int) (18 * u);
                grid.addView(fila, fl);
            }
            View t = tile(l.get(i), i);
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams((int) (166 * u), (int) (93 * u));
            tl.rightMargin = (int) (17 * u);
            fila.addView(t, tl);
        }
        if (teniaFoco || getCurrentFocus() == null) enfocarTile(0);
    }

    private void ponerTitulo() {
        if (titApps != null) titApps.setText(expandido ? "Todas tus apps" : "Tus apps");
    }

    private void enfocarTile(int i) {
        View v = grid.findViewWithTag(i);
        if (v != null) v.requestFocus();
    }

    private void expandir() {
        if (expandido || totalApps <= COLS) return;
        expandido = true;
        ponerTitulo();
        velo.setVisibility(View.VISIBLE);
        velo.animate().alpha(1f).setDuration(250).start();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) appsScroll.getLayoutParams();
        lp.height = pan.getHeight() - (int) (150 * u);
        appsScroll.setLayoutParams(lp);
    }

    private void colapsar() {
        if (!expandido) return;
        expandido = false;
        ponerTitulo();
        velo.animate().alpha(0f).setDuration(200).withEndAction(() -> velo.setVisibility(View.GONE)).start();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) appsScroll.getLayoutParams();
        lp.height = altoColapsado();
        appsScroll.setLayoutParams(lp);
        appsScroll.scrollTo(0, 0);
    }

    private View tile(final App a, final int idx) {
        FrameLayout f = new FrameLayout(this);
        f.setFocusable(true);
        f.setTag(idx);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12 * u);
        bg.setColor(0x40FFFFFF);
        f.setBackground(bg);
        f.setClipToOutline(true);
        if (a.banner != null) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
            iv.setImageDrawable(a.banner);
            f.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setGravity(Gravity.CENTER);
            if (a.icono != null) {
                ImageView iv = new ImageView(this);
                iv.setImageDrawable(a.icono);
                c.addView(iv, new LinearLayout.LayoutParams((int) (42 * u), (int) (42 * u)));
            }
            TextView t = Pantalla.txt(this, u, 15, 0xFFFFFFFF, true);
            t.setText(a.nombre);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            t.setGravity(Gravity.CENTER);
            t.setPadding((int) (8 * u), (int) (6 * u), (int) (8 * u), 0);
            c.addView(t, new LinearLayout.LayoutParams(-1, -2));
            f.addView(c, new FrameLayout.LayoutParams(-1, -1));
        }
        final GradientDrawable borde = new GradientDrawable();
        borde.setCornerRadius(12 * u);
        borde.setStroke((int) Math.max(2, 3 * u), moverIdx >= 0 ? 0xFFFFC94D : 0xFFFFFFFF);
        f.setOnFocusChangeListener((v, foc) -> {
            v.animate().scaleX(foc ? 1.1f : 1f).scaleY(foc ? 1.1f : 1f).translationZ(foc ? 12 * u : 0).setDuration(150).start();
            v.setForeground(foc ? borde : null);
            if (foc) {
                if (idx < COLS) colapsar();
                else expandir();
            }
        });
        f.setOnClickListener(v -> abrir(a.intent));
        return f;
    }

    private void abrir(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir", Toast.LENGTH_SHORT).show();
        }
    }

    // ================= mantener presionado: menú de la app =================
    private List<String> orden() {
        List<String> l = new ArrayList<>();
        for (String x : Datos.p(this).getString("orden", "").split(",")) if (!x.isEmpty()) l.add(x);
        return l;
    }

    private Set<String> ocultas() {
        return new HashSet<>(Datos.p(this).getStringSet("ocultas", new HashSet<>()));
    }

    private void guardarOrden() {
        StringBuilder b = new StringBuilder();
        for (App a : lista) b.append(a.pkg).append(',');
        Datos.p(this).edit().putString("orden", b.toString()).apply();
    }

    private void menuApp(final int idx) {
        if (idx < 0 || idx >= lista.size()) return;
        final App a = lista.get(idx);
        panel.abrir(p -> {
            p.titulo(a.nombre, "Opciones de la app");
            p.fila("▶", "Abrir", null, "", () -> {
                p.cerrar();
                abrir(a.intent);
            });
            p.fila("↔", "Mover", "Usa las flechas y presiona OK para soltar", "", () -> {
                p.cerrar();
                moverIdx = idx;
                llenarGrid(lista);
                if (idx >= COLS) expandir();
                enfocarTile(idx);
                Toast.makeText(this, "Mueve " + a.nombre + " con las flechas · OK para soltar", Toast.LENGTH_LONG).show();
            });
            p.fila("⭐", "Poner primero", "Al inicio de tus apps", "", () -> {
                lista.remove(idx);
                lista.add(0, a);
                guardarOrden();
                p.cerrar();
                llenarGrid(lista);
                enfocarTile(0);
            });
            p.fila("🙈", "Quitar de la fila", "Se oculta; la recuperas en Ajustes", "", () -> {
                Set<String> o = ocultas();
                o.add(a.pkg);
                Datos.p(this).edit().putStringSet("ocultas", o).apply();
                lista.remove(idx);
                p.cerrar();
                llenarGrid(lista);
                enfocarTile(Math.max(0, Math.min(idx, lista.size() - 1)));
            });
            p.fila("ℹ", "Información de la app", "Permisos, almacenamiento, forzar detención", "›", () ->
                    abrir(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + a.pkg))));
            p.fila("🗑", "Desinstalar", null, "›", () ->
                    abrir(new Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:" + a.pkg))));
        }, () -> enfocarTile(idx));
    }

    private void paginaOcultas(Panel p) {
        p.titulo("Apps ocultas", "Elige una para volver a mostrarla");
        final PackageManager pm = getPackageManager();
        Set<String> o = ocultas();
        if (o.isEmpty()) p.nota("No tienes apps ocultas.");
        for (final String pk : o) {
            String n = pk;
            try {
                n = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pk, 0)));
            } catch (Exception ignored) {
            }
            p.fila("👁", n, "Volver a mostrar", "", () -> {
                Set<String> x = ocultas();
                x.remove(pk);
                Datos.p(this).edit().putStringSet("ocultas", x).apply();
                cargarApps();
                p.mostrar();
            });
        }
    }

    /** Modo mover: flechas cambian la posición, OK o Atrás sueltan. */
    private boolean teclaMover(KeyEvent e) {
        int k = e.getKeyCode();
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;
        int d = k == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : k == KeyEvent.KEYCODE_DPAD_RIGHT ? 1
                : k == KeyEvent.KEYCODE_DPAD_UP ? -COLS : k == KeyEvent.KEYCODE_DPAD_DOWN ? COLS : 0;
        if (d != 0) {
            int n = moverIdx + d;
            if (n < 0 || n >= lista.size()) return true;
            App a = lista.remove(moverIdx);
            lista.add(n, a);
            moverIdx = n;
            llenarGrid(lista);
            if (n >= COLS) expandir();
            else colapsar();
            enfocarTile(n);
        } else if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_BACK) {
            int fin = moverIdx;
            moverIdx = -1;
            guardarOrden();
            llenarGrid(lista);
            enfocarTile(fin);
            Toast.makeText(this, "Listo", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    // ================= ajustes =================
    private String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private void cambio() {
        pan.construirRelojes();
        pan.forzarRed();
        pan.clima.actualizar();
    }

    private String nombreZonaFondos(String m) {
        return m.equals("pais") ? "Todo mi país" : m.equals("mundo") ? "Todo el mundo" : "Mi zona";
    }

    private void paginaAjustes(Panel p) {
        SharedPreferences s = Datos.p(this);
        p.titulo("Ajustes", "Mi TV · versión " + version());
        p.seccion("Pantalla de inicio");
        p.fila("🖼", "Fondos de pantalla", nombreZonaFondos(s.getString("zona_fondos", "auto"))
                + " · cambia cada " + segundos(Fondos.intervalo(this)), "›", () -> p.ir(this::paginaFondos));
        int extras = Relojes.extras(this).size();
        p.fila("🕒", "Relojes", Relojes.nombreLocal(this) + (extras > 0 ? " + " + extras + " extra" : ""), "›",
                () -> p.ir(this::paginaRelojes));
        String un = s.getString("unidad", "auto");
        p.fila("🌤", "Clima", un.equals("auto") ? "Automático (" + (Datos.fahrenheit(this) ? "°F" : "°C") + ")"
                : un.equals("c") ? "Celsius (°C)" : "Fahrenheit (°F)", "›", () -> p.opciones("Clima",
                "Unidad de temperatura", new String[]{"Automático según tu país", "Celsius (°C)", "Fahrenheit (°F)"},
                un.equals("c") ? 1 : un.equals("f") ? 2 : 0, i -> {
                    s.edit().putString("unidad", new String[]{"auto", "c", "f"}[i]).putLong("clima_t", 0).apply();
                    cambio();
                }));
        int rm = s.getInt("reposo_min", 5);
        final int[] mins = {2, 5, 10, 15, 30, 0};
        int sel = 1;
        for (int i = 0; i < mins.length; i++) if (mins[i] == rm) sel = i;
        final int fsel = sel;
        p.fila("🌙", "Salvapantallas", rm == 0 ? "Desactivado" : "Después de " + rm + " min sin usar el control", "›",
                () -> p.opciones("Salvapantallas", "Paisajes, relojes y clima a pantalla completa",
                        new String[]{"2 minutos", "5 minutos", "10 minutos", "15 minutos", "30 minutos", "Desactivado"},
                        fsel, i -> s.edit().putInt("reposo_min", mins[i]).apply()));
        p.fila("🙈", "Apps ocultas", ocultas().size() + " oculta(s)", "›", () -> p.ir(this::paginaOcultas));
        p.seccion("Conexión");
        p.fila("🛡", "VPN", Vpn.activa(this) ? "Conectada" : "Desconectada", "›", () -> p.ir(this::paginaVpn));
        p.fila("📶", "Wi‑Fi", "Redes e internet", "›", () -> abrir(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        p.seccion("Sistema");
        p.fila("⚙", "Ajustes de la tele", "Sonido, pantalla, cuentas y más", "›", () -> abrir(new Intent(Settings.ACTION_SETTINGS)));
        p.fila("ℹ", "Acerca de Mi TV", "Fotos: Wikimedia Commons · Clima: Open-Meteo", "", null);
    }

    private String segundos(int s) {
        return s < 60 ? s + " s" : (s / 60) + " min";
    }

    private void paginaFondos(Panel p) {
        SharedPreferences s = Datos.p(this);
        String ciudad = s.getString("ciudad", ""), cc = s.getString("cc", "");
        p.titulo("Fondos de pantalla", "Fotos reales de naturaleza según la hora y la temporada");
        String m = s.getString("zona_fondos", "auto");
        p.fila("📍", "Paisajes de", nombreZonaFondos(m) + (ciudad.isEmpty() ? "" : " · detectado: " + ciudad + ", " + cc), "›",
                () -> p.opciones("Paisajes de", "De dónde salen las fotos",
                        new String[]{"Mi zona (estado o región, luego el país)", "Todo mi país", "Todo el mundo"},
                        m.equals("pais") ? 1 : m.equals("mundo") ? 2 : 0, i -> {
                            s.edit().putString("zona_fondos", new String[]{"auto", "pais", "mundo"}[i]).apply();
                            cambio();
                        }));
        final int[] seg = {0, 30, 60, 120, 300, 600};
        int iv = s.getInt("intervalo", 0), sel = 0;
        for (int i = 0; i < seg.length; i++) if (seg[i] == iv) sel = i;
        final int fs = sel;
        p.fila("⏱", "Cambiar cada", iv == 0 ? "Automático (" + segundos(Fondos.intervalo(this)) + ")" : segundos(iv), "›",
                () -> p.opciones("Cambiar cada", "Con \"Automático\" todas las teles cambian al mismo tiempo",
                        new String[]{"Automático", "30 segundos", "1 minuto", "2 minutos", "5 minutos", "10 minutos"},
                        fs, i -> s.edit().putInt("intervalo", seg[i]).apply()));
        p.nota("Ahora: " + Fondos.nombrePeriodo(this) + ". Las fotos vienen de Wikimedia Commons y no se guardan en la tele.");
    }

    private void paginaRelojes(Panel p) {
        final TimeZone local = Relojes.zonaLocal(this);
        p.titulo("Relojes", "Tu hora local y relojes de otros países");
        p.seccion("Hora local");
        String z = Datos.p(this).getString("zona_local", "auto");
        p.fila("📍", Relojes.nombreLocal(this), "Ahora " + Relojes.hora(local) + (z.equals("auto") ? " · según la red de la tele" : " · elegida a mano"), "›", () -> {
            String[] op = new String[Relojes.ZONAS.length + 1];
            op[0] = "Automática (según la red)";
            for (int i = 0; i < Relojes.ZONAS.length; i++) op[i + 1] = Relojes.nombre(i) + "   " + Relojes.hora(TimeZone.getTimeZone(Relojes.id(i)));
            p.opciones("Hora local", "Puedes cambiarla cuando quieras", op, z.equals("auto") ? 0 : Relojes.indice(z) + 1, i -> {
                Datos.p(this).edit().putString("zona_local", i == 0 ? "auto" : Relojes.id(i - 1)).apply();
                cambio();
            });
        });
        p.seccion("Relojes extra");
        final List<String[]> l = Relojes.extras(this);
        long now = System.currentTimeMillis();
        for (int k = 0; k < l.size(); k++) {
            final int idx = k;
            String[] e = l.get(k);
            TimeZone tz = TimeZone.getTimeZone(e[1]);
            boolean oculto = tz.getID().equals(local.getID()) || tz.getOffset(now) == local.getOffset(now);
            p.fila("🕒", e[0], Relojes.hora(tz) + (oculto ? " · oculto: misma hora que tu hora local" : ""), "›",
                    () -> p.ir(q -> paginaRelojExtra(q, idx)));
        }
        if (l.size() < 2) p.fila("➕", "Agregar reloj", "Máximo 2 relojes extra", "›", () -> elegirZona(p, -1));
        if (l.isEmpty()) p.nota("No tienes relojes extra. Solo se muestra tu hora local.");
    }

    private void paginaRelojExtra(Panel p, final int idx) {
        final List<String[]> l = Relojes.extras(this);
        if (idx >= l.size()) {
            p.atras();
            return;
        }
        String[] e = l.get(idx);
        p.titulo(e[0], "Ahora " + Relojes.hora(TimeZone.getTimeZone(e[1])));
        p.fila("🔄", "Cambiar país o ciudad", e[0], "›", () -> elegirZona(p, idx));
        p.fila("🗑", "Eliminar este reloj", "Queda solo tu hora local", "", () -> {
            l.remove(idx);
            Relojes.guardar(this, l);
            cambio();
            Toast.makeText(this, "Reloj eliminado", Toast.LENGTH_SHORT).show();
            p.atras();
        });
    }

    private void elegirZona(Panel p, final int idx) {
        String[] op = new String[Relojes.ZONAS.length];
        for (int i = 0; i < op.length; i++) op[i] = Relojes.nombre(i) + "   " + Relojes.hora(TimeZone.getTimeZone(Relojes.id(i)));
        p.opciones(idx < 0 ? "Agregar reloj" : "Cambiar reloj", "Elige el país o la ciudad", op, -1, i -> {
            List<String[]> l = Relojes.extras(this);
            String[] nuevo = {Relojes.nombre(i), Relojes.id(i)};
            if (idx < 0) l.add(nuevo);
            else if (idx < l.size()) l.set(idx, nuevo);
            Relojes.guardar(this, l);
            cambio();
        });
    }

    private void paginaVpn(Panel p) {
        final SharedPreferences s = Datos.p(this);
        boolean on = Vpn.activa(this);
        p.titulo("VPN", on ? "●  Conectada" : "○  Desconectada");
        String ult = s.getString("vpn_ultimo", "");
        if (!ult.isEmpty()) {
            p.seccion("Último país");
            for (String[] pa : Vpn.PAISES)
                if (pa[0].equals(ult)) p.fila(Vpn.bandera(pa[1]), pa[0], on ? "Conectado" : "Volver a conectar", "›", () -> conectar(pa[0]));
        }
        p.seccion("Países");
        for (final String[] pa : Vpn.PAISES)
            if (!pa[0].equals(ult)) p.fila(Vpn.bandera(pa[1]), pa[0], null, "›", () -> conectar(pa[0]));
        p.seccion("App de VPN");
        final List<String[]> inst = Vpn.instaladas(this);
        String app = s.getString("vpn_app", "");
        String nombre = inst.isEmpty() ? "No hay apps de VPN instaladas" : inst.get(0)[0];
        int sel = 0;
        for (int i = 0; i < inst.size(); i++)
            if (inst.get(i)[1].equals(app)) {
                nombre = inst.get(i)[0];
                sel = i;
            }
        final int fs = sel;
        p.fila("🛡", nombre, inst.isEmpty() ? "Instala Proton VPN, NordVPN, Surfshark…" : "Se abre para conectar o desconectar", "›", () -> {
            if (inst.isEmpty()) return;
            String[] n = new String[inst.size()];
            for (int i = 0; i < n.length; i++) n[i] = inst.get(i)[0];
            p.opciones("App de VPN", "La que se abre al elegir un país", n, fs, i -> s.edit().putString("vpn_app", inst.get(i)[1]).apply());
        });
        if (on) p.fila("⏻", "Desconectar", "Abre tu app de VPN", "", () -> Vpn.abrirApp(this));
    }

    private void conectar(String pais) {
        Datos.p(this).edit().putString("vpn_ultimo", pais).apply();
        Toast.makeText(this, "Elige " + pais + " en tu app de VPN", Toast.LENGTH_LONG).show();
        panel.cerrar();
        Vpn.abrirApp(this);
    }

    // ================= reposo =================
    private void programarReposo() {
        h.removeCallbacks(aReposo);
        int m = Datos.p(this).getInt("reposo_min", 5);
        if (m > 0) h.postDelayed(aReposo, m * 60000L);
    }

    private void entrarReposo() {
        if (panel.abierto()) panel.cerrar();
        colapsar();
        enReposo = true;
        nav.setVisibility(View.GONE);
        appsScroll.setVisibility(View.GONE);
        pan.setReposo(true);
    }

    private void salirReposo() {
        enReposo = false;
        nav.setVisibility(View.VISIBLE);
        appsScroll.setVisibility(View.VISIBLE);
        pan.setReposo(false);
        programarReposo();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (enReposo) {
            if (e.getAction() == KeyEvent.ACTION_UP) salirReposo();
            return true;
        }
        programarReposo();
        int k = e.getKeyCode();
        if (moverIdx >= 0) return teclaMover(e);
        View foco = getCurrentFocus();
        if ((k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) && !panel.abierto()
                && foco != null && foco.getTag() instanceof Integer) {
            if (e.getAction() == KeyEvent.ACTION_DOWN) {
                if (e.getRepeatCount() == 0) {
                    tPulsado = e.getEventTime();
                    menuSalio = false;
                } else if (!menuSalio && e.getEventTime() - tPulsado >= MANTENER_MS) {
                    menuSalio = true;
                    menuApp((Integer) foco.getTag());
                }
            } else if (e.getAction() == KeyEvent.ACTION_UP && !menuSalio) {
                foco.performClick();
            }
            return true;
        }
        if (k == KeyEvent.KEYCODE_BACK || k == KeyEvent.KEYCODE_ESCAPE) {
            if (e.getAction() == KeyEvent.ACTION_UP) {
                if (panel.abierto()) panel.atras();
                else if (expandido) {
                    colapsar();
                    enfocarTile(0);
                }
            }
            return true;
        }
        if (k == KeyEvent.KEYCODE_DPAD_DOWN && e.getAction() == KeyEvent.ACTION_DOWN && !panel.abierto() && !expandido) {
            View f = getCurrentFocus();
            if (f != null && f.getTag() instanceof Integer) {
                int i = (Integer) f.getTag();
                if (i < COLS && totalApps > COLS) {
                    expandir();
                    final int dest = Math.min(i + COLS, totalApps - 1);
                    h.post(() -> enfocarTile(dest));
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if (enReposo) salirReposo();
        pan.iniciar();
        cargarApps();
        programarReposo();
        h.post(vigilarVpn);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pan.detener();
        h.removeCallbacksAndMessages(null);
        if (pulso != null) pulso.cancel();
    }
}
