package com.mitv.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

/** Páginas En vivo / Películas / Series: lo que dejaste en pausa y lo que pasa en tus apps. */
final class Contenido extends FrameLayout {
    static final List<String> STREAMING = Arrays.asList("com.netflix.ninja", "com.disney.disneyplus",
            "com.amazon.amazonvideo.livingroom", "com.wbd.stream", "com.hbo.hbonow", "com.apple.atve.androidtv.appletv",
            "com.cbs.ott", "com.peacocktv.peacockandroid", "com.hulu.livingroomplus", "com.plexapp.android", "com.tubitv",
            "tv.pluto.android", "com.univision.prendetv", "com.crunchyroll.crunchyroid", "com.google.android.youtube.tv",
            "com.google.android.youtube.tvunplugged", "com.google.android.videos");

    private final MainActivity a;
    private final float u;
    private final LinearLayout col;
    private final ScrollView scroll;

    Contenido(MainActivity a, float u) {
        super(a);
        this.a = a;
        this.u = u;
        setBackgroundColor(0xE0080A1C);
        scroll = new ScrollView(a);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);
        col.setPadding((int) (76 * u), (int) (130 * u), (int) (76 * u), (int) (60 * u));
        scroll.addView(col);
        addView(scroll, new LayoutParams(-1, -1));
        setVisibility(GONE);
    }

    boolean abierto() {
        return getVisibility() == VISIBLE;
    }

    void cerrar() {
        if (!abierto()) return;
        animate().alpha(0f).setDuration(220).withEndAction(() -> setVisibility(GONE)).start();
    }

    private TextView txt(float s, int c, boolean b, String t) {
        TextView v = Pantalla.txt(getContext(), u, s, c, b);
        v.setText(t);
        return v;
    }

    private void cab(String t) {
        TextView v = txt(24, 0xFFFFFFFF, true, t);
        v.setPadding(0, (int) (26 * u), 0, (int) (16 * u));
        col.addView(v);
    }

    private LinearLayout fila() {
        HorizontalScrollView h = new HorizontalScrollView(getContext());
        h.setHorizontalScrollBarEnabled(false);
        h.setClipChildren(false);
        h.setClipToPadding(false);
        h.setPadding(0, (int) (10 * u), 0, (int) (10 * u));
        LinearLayout l = new LinearLayout(getContext());
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setClipChildren(false);
        h.addView(l);
        col.addView(h);
        return l;
    }

    private String nombreApp(String pkg) {
        PackageManager pm = getContext().getPackageManager();
        try {
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (Exception e) {
            return pkg;
        }
    }

    private GradientDrawable redondo(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(r * u);
        g.setColor(color);
        return g;
    }

    private void agregar(LinearLayout fila, View tarjeta, int w, int h, Runnable click) {
        tarjeta.setFocusable(true);
        final GradientDrawable borde = new GradientDrawable();
        borde.setCornerRadius(20 * u);
        borde.setStroke((int) Math.max(2, 3 * u), 0xFFFFFFFF);
        tarjeta.setOnFocusChangeListener((v, f) -> {
            Aura.foco(v, f);
            v.setForeground(f ? borde : null);
        });
        tarjeta.setOnClickListener(v -> click.run());
        FrameLayout c = Aura.envolver(tarjeta, u, w, h, 1.06f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, -2);
        lp.rightMargin = (int) (30 * u);
        fila.addView(c, lp);
    }

    private void tarjetaMedio(LinearLayout fila, final Avisos.Item it) {
        Context c = getContext();
        int w = (int) (352 * u), h = (int) (198 * u);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        FrameLayout img = new FrameLayout(c);
        img.setBackground(redondo(0x33FFFFFF, 20));
        img.setClipToOutline(true);
        if (it.imagen != null) {
            ImageView iv = new ImageView(c);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(it.imagen);
            img.addView(iv, new LayoutParams(-1, -1));
        }
        TextView chip = txt(13, 0xFFFFFFFF, true, "●  " + nombreApp(it.pkg));
        chip.setBackground(redondo(0xC00A0C1E, 13));
        chip.setPadding((int) (12 * u), (int) (6 * u), (int) (12 * u), (int) (6 * u));
        LayoutParams cl = new LayoutParams(-2, -2, Gravity.TOP | Gravity.LEFT);
        cl.leftMargin = cl.topMargin = (int) (12 * u);
        img.addView(chip, cl);
        if (it.progreso >= 0) {
            View base = new View(c), barra = new View(c);
            base.setBackground(redondo(0x55FFFFFF, 3));
            barra.setBackground(redondo(0xFFFF3C3C, 3));
            LayoutParams bl = new LayoutParams(-1, (int) (5 * u), Gravity.BOTTOM);
            bl.leftMargin = bl.rightMargin = (int) (14 * u);
            bl.bottomMargin = (int) (12 * u);
            img.addView(base, bl);
            LayoutParams pl = new LayoutParams((int) ((w - 28 * u) * it.progreso), (int) (5 * u), Gravity.BOTTOM | Gravity.LEFT);
            pl.leftMargin = (int) (14 * u);
            pl.bottomMargin = (int) (12 * u);
            img.addView(barra, pl);
        }
        box.addView(img, new LinearLayout.LayoutParams(w, h));
        TextView t = txt(19, 0xFFFFFFFF, true, it.titulo);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setPadding((int) (2 * u), (int) (12 * u), 0, 0);
        box.addView(t, new LinearLayout.LayoutParams(w, -2));
        box.addView(txt(15, 0xB4FFFFFF, false, it.etiqueta), new LinearLayout.LayoutParams(w, -2));
        // El foco y el aura van en la imagen; el texto queda debajo.
        agregar(fila, box, w, -2, () -> {
            Intent i = Vpn.lanzar(c, it.pkg);
            if (i != null) a.abrirIntent(i);
            try {
                it.control.getTransportControls().play();
            } catch (Exception ignored) {
            }
        });
    }

    private void tarjetaAviso(LinearLayout fila, final Avisos.Item it) {
        Context c = getContext();
        int w = (int) (352 * u), h = (int) (150 * u);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(redondo(0x24FFFFFF, 20));
        box.setPadding((int) (20 * u), (int) (18 * u), (int) (20 * u), (int) (16 * u));
        LinearLayout top = new LinearLayout(c);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView app = txt(15, 0xB4FFFFFF, true, "●  " + nombreApp(it.pkg));
        top.addView(app, new LinearLayout.LayoutParams(0, -2, 1f));
        if (!it.etiqueta.isEmpty()) {
            TextView tag = txt(12, 0xFFFFFFFF, true, it.etiqueta);
            tag.setBackground(redondo(0xE65A8CFF, 12));
            tag.setPadding((int) (10 * u), (int) (4 * u), (int) (10 * u), (int) (4 * u));
            top.addView(tag);
        }
        box.addView(top);
        TextView t = txt(19, 0xFFFFFFFF, true, it.titulo);
        t.setMaxLines(1);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setPadding(0, (int) (16 * u), 0, (int) (6 * u));
        box.addView(t);
        TextView s = txt(15, 0xB4FFFFFF, false, it.sub);
        s.setMaxLines(2);
        s.setEllipsize(TextUtils.TruncateAt.END);
        box.addView(s);
        agregar(fila, box, w, h, () -> {
            try {
                if (it.aviso.contentIntent != null) it.aviso.contentIntent.send();
                else a.abrirIntent(Vpn.lanzar(c, it.pkg));
            } catch (Exception e) {
                Toast.makeText(c, "No se pudo abrir", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void tarjetaTexto(LinearLayout fila, String t, String s, Runnable click) {
        Avisos.Item it = new Avisos.Item();
        it.pkg = getContext().getPackageName();
        it.titulo = t;
        it.sub = s;
        it.etiqueta = "";
        int w = (int) (720 * u), h = (int) (150 * u);
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(redondo(0x24FFFFFF, 20));
        box.setPadding((int) (24 * u), (int) (22 * u), (int) (24 * u), (int) (18 * u));
        box.addView(txt(20, 0xFFFFFFFF, true, t));
        TextView sv = txt(15, 0xB4FFFFFF, false, s);
        sv.setPadding(0, (int) (10 * u), 0, 0);
        box.addView(sv);
        agregar(fila, box, w, h, click);
    }

    /** tipo: "En vivo", "Películas" o "Series". */
    void mostrar(String tipo) {
        col.removeAllViews();
        Context c = getContext();
        boolean todo = tipo.equals("En vivo");
        col.addView(txt(40, 0xFFFFFFFF, true, tipo));
        col.addView(txt(18, 0xB4FFFFFF, false, todo ? "Lo que dejaste a medias y lo que pasa ahora en tus apps"
                : tipo.equals("Series") ? "Sigue con el próximo episodio en tus apps" : "Las películas que dejaste en pausa"));
        if (!Avisos.permitido(c)) {
            cab("Activa este apartado");
            tarjetaTexto(fila(), "Permitir \"acceso a notificaciones\" para Mi TV",
                    "Se hace una sola vez. Así Mi TV puede ver lo que dejaste en pausa y los avisos de tus apps.", () -> {
                        try {
                            a.abrirIntent(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                        } catch (Exception e) {
                            Toast.makeText(c, "Tu tele esconde ese ajuste. Se activa desde la computadora con: adb shell cmd notification allow_listener com.mitv.launcher/.Avisos", Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            cab("Continuar viendo");
            LinearLayout f1 = fila();
            for (Avisos.Item it : Avisos.medios(c))
                if (todo || STREAMING.contains(it.pkg)) tarjetaMedio(f1, it);
            if (f1.getChildCount() == 0)
                tarjetaTexto(f1, "Nada en pausa por ahora", "Aquí aparece lo que dejes a medias en YouTube y otras apps.", () -> {
                });
            cab("Ahora en tus apps");
            LinearLayout f2 = fila();
            for (Avisos.Item it : Avisos.avisos(c))
                if (todo || STREAMING.contains(it.pkg)) tarjetaAviso(f2, it);
            if (f2.getChildCount() == 0)
                tarjetaTexto(f2, "Sin novedades", "Los avisos nuevos de tus apps (videos, partidos, noticias) salen aquí.", () -> {
                });
        }
        cab(todo ? "Apps para ver en vivo" : "Tus apps de " + tipo.toLowerCase());
        LinearLayout f3 = fila();
        for (String pkg : STREAMING) {
            final Intent i = Vpn.lanzar(c, pkg);
            if (i == null) continue;
            TextView t = txt(19, 0xFFFFFFFF, true, nombreApp(pkg));
            t.setGravity(Gravity.CENTER);
            t.setBackground(redondo(0x30FFFFFF, 20));
            int w = (int) (220 * u), h = (int) (90 * u);
            agregar(f3, t, w, h, () -> a.abrirIntent(i));
        }
        setAlpha(0f);
        setVisibility(VISIBLE);
        animate().alpha(1f).setDuration(250).start();
        scroll.scrollTo(0, 0);
        post(col::requestFocus);
    }
}
