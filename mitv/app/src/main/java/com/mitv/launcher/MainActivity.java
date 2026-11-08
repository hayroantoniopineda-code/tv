package com.mitv.launcher;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private Pantalla pan;
    private float u;
    private LinearLayout nav, apps, appsCont;
    private View dot;
    private ObjectAnimator pulso;
    private boolean enReposo;
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
        String nombre;
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
        setContentView(pan);
    }

    // ---------- barra superior ----------
    private GradientDrawable pildora(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(30 * u);
        g.setColor(color);
        return g;
    }

    private void enfoque(View v, final TextView t, final boolean seleccionado) {
        v.setFocusable(true);
        v.setBackground(pildora(seleccionado ? 0x33FFFFFF : 0));
        v.setOnFocusChangeListener((x, f) -> {
            x.setBackground(pildora(f ? 0xF2FFFFFF : seleccionado ? 0x33FFFFFF : 0));
            t.setTextColor(f ? 0xFF14162A : 0xFFFFFFFF);
            t.setShadowLayer(f ? 0 : 4 * u, 0, f ? 0 : 2 * u, 0x88000000);
        });
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
            TextView t = Pantalla.txt(this, u, 24, 0xFFFFFFFF, true);
            t.setText(it);
            v.addView(t);
            if (it.equals("VPN")) {
                dot = new View(this);
                GradientDrawable g = new GradientDrawable();
                g.setShape(GradientDrawable.OVAL);
                g.setColor(0xFF46D778);
                dot.setBackground(g);
                LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams((int) (10 * u), (int) (10 * u));
                dl.leftMargin = (int) (8 * u);
                dl.bottomMargin = (int) (14 * u);
                v.addView(dot, dl);
                pulso = ObjectAnimator.ofPropertyValuesHolder(dot,
                        PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.3f),
                        PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.4f),
                        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.4f));
                pulso.setDuration(1400);
                pulso.setRepeatCount(ValueAnimator.INFINITE);
                pulso.setRepeatMode(ValueAnimator.REVERSE);
            }
            enfoque(v, t, it.equals("Inicio"));
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
                if (apps.getChildCount() > 0) apps.getChildAt(0).requestFocus();
                break;
            case "Apps":
            case "Películas":
            case "Series":
                todas(it.equals("Apps") ? "Todas las apps" : it);
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
                Vpn.panel(this);
                break;
            case "Ajustes":
                ajustes();
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

    // ---------- fila de apps ----------
    private void construirApps() {
        appsCont = new LinearLayout(this);
        appsCont.setOrientation(LinearLayout.VERTICAL);
        appsCont.setClipChildren(false);
        appsCont.setClipToPadding(false);
        TextView tit = Pantalla.txt(this, u, 25, 0xFFFFFFFF, true);
        tit.setText("Tus apps");
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.leftMargin = (int) (76 * u);
        appsCont.addView(tit, tl);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipChildren(false);
        hs.setClipToPadding(false);
        hs.setPadding((int) (76 * u), (int) (18 * u), (int) (76 * u), (int) (18 * u));
        apps = new LinearLayout(this);
        apps.setOrientation(LinearLayout.HORIZONTAL);
        apps.setClipChildren(false);
        hs.addView(apps);
        appsCont.addView(hs);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        lp.bottomMargin = (int) (70 * u);
        pan.addView(appsCont, lp);
    }

    private List<App> listar(boolean todas) {
        PackageManager pm = getPackageManager();
        List<App> r = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        String[] cats = todas ? new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}
                : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER};
        for (String cat : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || !vistos.add(pkg)) continue;
                App a = new App();
                a.nombre = String.valueOf(ri.loadLabel(pm));
                a.intent = new Intent(Intent.ACTION_MAIN).addCategory(cat)
                        .setClassName(pkg, ri.activityInfo.name);
                if (cat.equals(Intent.CATEGORY_LEANBACK_LAUNCHER)) {
                    a.banner = ri.activityInfo.loadBanner(pm);
                    if (a.banner == null) a.banner = ri.activityInfo.applicationInfo.loadBanner(pm);
                }
                if (a.banner == null) a.icono = ri.loadIcon(pm);
                r.add(a);
            }
        }
        if (r.isEmpty() && !todas) return listar(true);
        Collections.sort(r, (x, y) -> x.nombre.compareToIgnoreCase(y.nombre));
        return r;
    }

    private void cargarApps() {
        Pantalla.BG.execute(() -> {
            final List<App> l = listar(false);
            h.post(() -> {
                View foco = getCurrentFocus();
                boolean teniaFoco = foco != null && foco.getParent() == apps;
                apps.removeAllViews();
                for (App a : l) apps.addView(tile(a), tileLp());
                App mas = new App();
                mas.nombre = "▼  Todas";
                apps.addView(tile(mas), tileLp());
                if (teniaFoco || getCurrentFocus() == null) apps.getChildAt(0).requestFocus();
            });
        });
    }

    private LinearLayout.LayoutParams tileLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (174 * u), (int) (98 * u));
        lp.rightMargin = (int) (22 * u);
        return lp;
    }

    private View tile(final App a) {
        FrameLayout f = new FrameLayout(this);
        f.setFocusable(true);
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
                c.addView(iv, new LinearLayout.LayoutParams((int) (44 * u), (int) (44 * u)));
            }
            TextView t = Pantalla.txt(this, u, 17, 0xFFFFFFFF, true);
            t.setText(a.nombre);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            t.setGravity(Gravity.CENTER);
            c.addView(t);
            f.addView(c, new FrameLayout.LayoutParams(-1, -1));
        }
        final GradientDrawable borde = new GradientDrawable();
        borde.setCornerRadius(12 * u);
        borde.setStroke((int) Math.max(2, 3 * u), 0xFFFFFFFF);
        f.setOnFocusChangeListener((v, foc) -> {
            v.animate().scaleX(foc ? 1.1f : 1f).scaleY(foc ? 1.1f : 1f).translationZ(foc ? 12 * u : 0).setDuration(150).start();
            v.setForeground(foc ? borde : null);
        });
        f.setOnClickListener(v -> {
            if (a.intent == null) todas("Todas las apps");
            else abrir(a.intent);
        });
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

    private void todas(String titulo) {
        final List<App> l = listar(true);
        GridView g = new GridView(this);
        g.setNumColumns(5);
        g.setVerticalSpacing((int) (14 * u));
        g.setHorizontalSpacing((int) (14 * u));
        g.setPadding((int) (20 * u), (int) (20 * u), (int) (20 * u), (int) (20 * u));
        g.setAdapter(new BaseAdapter() {
            public int getCount() {
                return l.size();
            }

            public Object getItem(int i) {
                return l.get(i);
            }

            public long getItemId(int i) {
                return i;
            }

            public View getView(int i, View v, ViewGroup parent) {
                App a = l.get(i);
                LinearLayout c = new LinearLayout(MainActivity.this);
                c.setOrientation(LinearLayout.VERTICAL);
                c.setGravity(Gravity.CENTER_HORIZONTAL);
                c.setPadding(0, (int) (10 * u), 0, (int) (10 * u));
                ImageView iv = new ImageView(MainActivity.this);
                iv.setImageDrawable(a.banner != null ? a.banner : a.icono);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                c.addView(iv, new LinearLayout.LayoutParams((int) (130 * u), (int) (74 * u)));
                TextView t = Pantalla.txt(MainActivity.this, u, 16, 0xFFFFFFFF, false);
                t.setText(a.nombre);
                t.setSingleLine(true);
                t.setEllipsize(TextUtils.TruncateAt.END);
                t.setGravity(Gravity.CENTER);
                c.addView(t, new LinearLayout.LayoutParams((int) (160 * u), -2));
                return c;
            }
        });
        final AlertDialog d = new AlertDialog.Builder(this).setTitle(titulo).setView(g).create();
        g.setOnItemClickListener((p, v, i, id) -> {
            d.dismiss();
            abrir(l.get(i).intent);
        });
        d.show();
    }

    // ---------- ajustes ----------
    private void ajustes() {
        final SharedPreferences p = Datos.p(this);
        int iv = p.getInt("intervalo", 0);
        String zf = p.getString("zona_fondos", "auto");
        String un = p.getString("unidad", "auto");
        int rm = p.getInt("reposo_min", 5);
        String[] items = {
                "Cambio de fondo: " + (iv == 0 ? "automático (" + Fondos.intervalo(this) + " s)" : iv + " s"),
                "Hora local: " + Relojes.nombreLocal(this),
                "Relojes extra (" + Relojes.extras(this).size() + ")",
                "Paisajes: " + (zf.equals("auto") ? "mi zona" : zf.equals("pais") ? "todo mi país" : "todo el mundo"),
                "Temperatura: " + (un.equals("auto") ? "automática" : un.equals("c") ? "°C" : "°F"),
                "Salvapantallas: " + (rm == 0 ? "desactivado" : rm + " min"),
                "App de VPN",
                "Wi‑Fi",
                "Ajustes de la tele"
        };
        new AlertDialog.Builder(this).setTitle("Ajustes").setItems(items, (d, w) -> {
            switch (w) {
                case 0:
                    elegir("Cambio de fondo", new String[]{"Automático (lista central)", "30 segundos", "1 minuto", "2 minutos", "5 minutos"},
                            i -> p.edit().putInt("intervalo", new int[]{0, 30, 60, 120, 300}[i]).apply());
                    break;
                case 1:
                    String[] z = new String[Relojes.ZONAS.length + 1];
                    z[0] = "Automática (red)";
                    for (int i = 0; i < Relojes.ZONAS.length; i++) z[i + 1] = Relojes.nombre(i);
                    elegir("Hora local", z, i -> p.edit().putString("zona_local", i == 0 ? "auto" : Relojes.id(i - 1)).apply());
                    break;
                case 2:
                    relojesExtra();
                    break;
                case 3:
                    elegir("Paisajes de", new String[]{"Mi zona (automático)", "Todo mi país", "Todo el mundo"},
                            i -> p.edit().putString("zona_fondos", new String[]{"auto", "pais", "mundo"}[i]).apply());
                    break;
                case 4:
                    elegir("Temperatura", new String[]{"Automática", "°C", "°F"},
                            i -> p.edit().putString("unidad", new String[]{"auto", "c", "f"}[i]).putLong("clima_t", 0).apply());
                    break;
                case 5:
                    elegir("Salvapantallas después de", new String[]{"2 minutos", "5 minutos", "10 minutos", "15 minutos", "Desactivado"},
                            i -> p.edit().putInt("reposo_min", new int[]{2, 5, 10, 15, 0}[i]).apply());
                    break;
                case 6:
                    final List<String[]> l = Vpn.instaladas(this);
                    if (l.isEmpty()) {
                        Toast.makeText(this, "No encontré apps de VPN instaladas", Toast.LENGTH_LONG).show();
                        break;
                    }
                    String[] n = new String[l.size()];
                    for (int i = 0; i < n.length; i++) n[i] = l.get(i)[0];
                    elegir("App de VPN", n, i -> p.edit().putString("vpn_app", l.get(i)[1]).apply());
                    break;
                case 7:
                    abrir(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    break;
                case 8:
                    abrir(new Intent(Settings.ACTION_SETTINGS));
                    break;
            }
        }).show();
    }

    interface Eleccion {
        void elegido(int i);
    }

    private void elegir(String titulo, String[] op, final Eleccion e) {
        new AlertDialog.Builder(this).setTitle(titulo).setItems(op, (d, i) -> {
            e.elegido(i);
            refrescar();
        }).show();
    }

    private void relojesExtra() {
        final List<String[]> l = Relojes.extras(this);
        List<String> items = new ArrayList<>();
        for (String[] a : l) items.add("✕  Eliminar " + a[0]);
        if (l.size() < 2) items.add("+  Agregar zona horaria");
        new AlertDialog.Builder(this).setTitle("Relojes extra").setItems(items.toArray(new String[0]), (d, w) -> {
            if (w < l.size()) {
                l.remove(w);
                Relojes.guardar(this, l);
                refrescar();
            } else {
                String[] z = new String[Relojes.ZONAS.length];
                for (int i = 0; i < z.length; i++) z[i] = Relojes.nombre(i);
                elegir("Agregar reloj", z, i -> {
                    l.add(new String[]{Relojes.nombre(i), Relojes.id(i)});
                    Relojes.guardar(this, l);
                });
            }
        }).show();
    }

    private void refrescar() {
        pan.construirRelojes();
        pan.forzarRed();
        pan.clima.actualizar();
    }

    // ---------- reposo (salvapantallas dentro del inicio) ----------
    private void programarReposo() {
        h.removeCallbacks(aReposo);
        int m = Datos.p(this).getInt("reposo_min", 5);
        if (m > 0) h.postDelayed(aReposo, m * 60000L);
    }

    private void entrarReposo() {
        enReposo = true;
        nav.setVisibility(View.GONE);
        appsCont.setVisibility(View.GONE);
        pan.setReposo(true);
    }

    private void salirReposo() {
        enReposo = false;
        nav.setVisibility(View.VISIBLE);
        appsCont.setVisibility(View.VISIBLE);
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
        return super.dispatchKeyEvent(e);
    }

    @Override
    public void onBackPressed() {
        // El inicio no se cierra con "Atrás".
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
