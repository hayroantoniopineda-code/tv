package com.mitv.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Fondo de paisajes + relojes + clima. La usan el inicio y el salvapantallas. */
final class Pantalla extends FrameLayout {
    static final ExecutorService BG = Executors.newSingleThreadExecutor();
    final Handler h = new Handler(Looper.getMainLooper());
    final float u;
    private final ImageView[] fondo = new ImageView[2];
    private final Bitmap[] bmp = new Bitmap[2];
    private int actual = 0;
    private String urlMostrada;
    private boolean cargando, activo, reposo;
    private long ultimaRed;
    private final View sombraArriba, sombraAbajo;
    private final LinearLayout bloque, relojes;
    private final TextView fecha, nota, pie;
    final WeatherView clima;
    private final List<Object[]> horas = new ArrayList<>();
    private Fondos.Foto fotoActual;

    static TextView txt(Context c, float u, float size, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * u);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setShadowLayer(4 * u, 0, 2 * u, 0x88000000);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    Pantalla(Context c, boolean reposo) {
        super(c);
        int w = getResources().getDisplayMetrics().widthPixels, hh = getResources().getDisplayMetrics().heightPixels;
        u = Math.max(w, hh) / 1652f;
        setBackgroundColor(0xFF0B0E22);
        for (int i = 0; i < 2; i++) {
            ImageView iv = new ImageView(c);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setAlpha(0f);
            fondo[i] = iv;
            addView(iv, new LayoutParams(-1, -1));
        }
        sombraArriba = new View(c);
        sombraArriba.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xB0060816, 0x00060816}));
        addView(sombraArriba, new LayoutParams(-1, (int) (330 * u), Gravity.TOP));
        sombraAbajo = new View(c);
        sombraAbajo.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xD8060816, 0x00060816}));
        addView(sombraAbajo, new LayoutParams(-1, (int) (480 * u), Gravity.BOTTOM));

        bloque = new LinearLayout(c);
        bloque.setOrientation(LinearLayout.VERTICAL);
        relojes = new LinearLayout(c);
        relojes.setOrientation(LinearLayout.HORIZONTAL);
        relojes.setGravity(Gravity.BOTTOM);
        fecha = txt(c, u, 14, 0xB4FFFFFF, false);
        nota = txt(c, u, 13, 0x99FFFFFF, false);
        bloque.addView(relojes);
        bloque.addView(fecha);
        bloque.addView(nota);
        addView(bloque);
        clima = new WeatherView(c, u);
        addView(clima);
        pie = txt(c, u, 15, 0xB4FFFFFF, false);
        pie.setMaxLines(2);
        addView(pie);
        setReposo(reposo);
    }

    boolean enReposo() {
        return reposo;
    }

    void setReposo(boolean r) {
        reposo = r;
        sombraArriba.setAlpha(r ? 0.6f : 1f);
        LayoutParams lp = new LayoutParams(-2, -2, r ? Gravity.BOTTOM | Gravity.LEFT : Gravity.TOP | Gravity.RIGHT);
        lp.leftMargin = (int) (72 * u);
        lp.rightMargin = (int) (62 * u);
        lp.topMargin = (int) (34 * u);
        lp.bottomMargin = (int) (110 * u);
        bloque.setLayoutParams(lp);
        int g = r ? Gravity.LEFT : Gravity.RIGHT;
        bloque.setGravity(g);
        fecha.setGravity(g);
        nota.setGravity(g);
        fecha.setTextSize(TypedValue.COMPLEX_UNIT_PX, (r ? 24 : 14) * u);
        fecha.setPadding(0, (int) ((r ? 10 : 5) * u), 0, 0);
        LayoutParams cl = new LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT);
        cl.rightMargin = (int) (62 * u);
        cl.topMargin = (int) ((r ? 48 : 128) * u);
        clima.setLayoutParams(cl);
        LayoutParams pl = new LayoutParams(-2, -2, Gravity.BOTTOM | (r ? Gravity.RIGHT : Gravity.LEFT));
        pl.leftMargin = (int) (76 * u);
        pl.rightMargin = (int) (62 * u);
        pl.bottomMargin = (int) ((r ? 80 : 300) * u);
        pie.setGravity(r ? Gravity.RIGHT : Gravity.LEFT);
        pie.setLayoutParams(pl);
        construirRelojes();
        mostrarPie();
    }

    private LinearLayout reloj(Context c, TimeZone tz, String etiqueta, boolean local) {
        LinearLayout b = new LinearLayout(c);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(reposo ? Gravity.LEFT : Gravity.RIGHT);
        boolean grande = reposo && local;
        TextView hora = txt(c, u, grande ? 132 : reposo ? 46 : 29, 0xFFFFFFFF, !reposo);
        if (reposo) hora.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        TextView lab = txt(c, u, reposo ? 15 : 12.5f, 0xB4FFFFFF, true);
        lab.setText(etiqueta);
        lab.setLetterSpacing(0.04f);
        lab.setPadding(0, (int) (3 * u), 0, 0);
        if (grande) {
            b.addView(lab);
            b.addView(hora);
        } else {
            b.addView(hora);
            b.addView(lab);
        }
        horas.add(new Object[]{hora, tz, grande});
        return b;
    }

    void construirRelojes() {
        Context c = getContext();
        relojes.removeAllViews();
        horas.clear();
        TimeZone local = Relojes.zonaLocal(c);
        long now = System.currentTimeMillis();
        boolean auto = Datos.p(c).getString("zona_local", "auto").equals("auto");
        relojes.addView(reloj(c, local, auto ? "HORA LOCAL · RED" : "HORA LOCAL", true));
        String notaTxt = "";
        for (String[] e : Relojes.extras(c)) {
            TimeZone tz = TimeZone.getTimeZone(e[1]);
            if (tz.getID().equals(local.getID())) continue;
            if (tz.getOffset(now) == local.getOffset(now)) {
                notaTxt = "Misma hora en " + e[0];
                continue;
            }
            View sep = new View(c);
            sep.setBackgroundColor(0x50FFFFFF);
            LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(Math.max(1, (int) u), (int) ((reposo ? 90 : 46) * u));
            sl.leftMargin = sl.rightMargin = (int) ((reposo ? 36 : 20) * u);
            sl.gravity = Gravity.CENTER_VERTICAL;
            relojes.addView(sep, sl);
            relojes.addView(reloj(c, tz, e[0].toUpperCase(Locale.getDefault()), false));
        }
        nota.setText(notaTxt);
        nota.setVisibility(notaTxt.isEmpty() ? GONE : VISIBLE);
        tic();
    }

    private void tic() {
        Date d = new Date();
        for (Object[] o : horas) {
            TimeZone tz = (TimeZone) o[1];
            SimpleDateFormat fh = new SimpleDateFormat("h:mm", Locale.US), fa = new SimpleDateFormat(" a", Locale.US);
            fh.setTimeZone(tz);
            fa.setTimeZone(tz);
            String h1 = fh.format(d), a = fa.format(d);
            SpannableString s = new SpannableString(h1 + a);
            if ((Boolean) o[2]) s.setSpan(new RelativeSizeSpan(0.3f), h1.length(), s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ((TextView) o[0]).setText(s);
        }
        SimpleDateFormat ff = new SimpleDateFormat(reposo ? "EEEE, d 'de' MMMM" : "EEE, d MMM yyyy", new Locale("es"));
        ff.setTimeZone(Relojes.zonaLocal(getContext()));
        String f = ff.format(d).replace(".", "");
        if (!f.isEmpty()) f = Character.toUpperCase(f.charAt(0)) + f.substring(1);
        fecha.setText(f);
    }

    private void mostrarPie() {
        Fondos.Foto f = fotoActual;
        if (f == null) {
            pie.setText("");
            return;
        }
        StringBuilder b = new StringBuilder();
        if (reposo) b.append(Fondos.nombrePeriodo(getContext())).append(" · ");
        if (!f.l.isEmpty()) b.append(f.l).append(" · ");
        b.append("Foto: ").append(f.a.isEmpty() ? "Wikimedia Commons" : f.a + " / Wikimedia Commons");
        pie.setText(b.toString());
    }

    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            if (!activo) return;
            tic();
            revisarFondo();
            long n = System.currentTimeMillis();
            if (n - ultimaRed > 30 * 60000L) {
                ultimaRed = n;
                red();
            }
            h.postDelayed(this, 5000);
        }
    };

    void forzarRed() {
        ultimaRed = 0;
    }

    private void red() {
        final Context c = getContext().getApplicationContext();
        BG.execute(() -> {
            Datos.zona(c);
            Datos.clima(c);
            Fondos.refrescar(c);
            h.post(clima::actualizar);
        });
    }

    void iniciar() {
        activo = true;
        construirRelojes();
        clima.actualizar();
        h.removeCallbacks(loop);
        h.post(loop);
    }

    void detener() {
        activo = false;
        h.removeCallbacks(loop);
    }

    private void revisarFondo() {
        if (cargando) return;
        final Fondos.Foto f = Fondos.actual(getContext());
        if (f == null || f.u.equals(urlMostrada)) return;
        cargando = true;
        final int w = Math.min(1920, getResources().getDisplayMetrics().widthPixels);
        BG.execute(() -> {
            Bitmap b = null;
            try {
                b = Net.image(f.u, w);
            } catch (Throwable ignored) {
            }
            final Bitmap fb = b;
            h.post(() -> {
                cargando = false;
                if (fb == null) {
                    urlMostrada = f.u;
                    return;
                }
                if (!activo) {
                    fb.recycle();
                    return;
                }
                mostrar(fb, f);
            });
        });
    }

    private void mostrar(Bitmap b, Fondos.Foto f) {
        final int sig = 1 - actual, viejo = actual;
        fondo[sig].animate().cancel();
        fondo[sig].setImageBitmap(b);
        if (bmp[sig] != null && bmp[sig] != b) bmp[sig].recycle();
        bmp[sig] = b;
        fondo[sig].setAlpha(0f);
        fondo[sig].animate().alpha(1f).setDuration(1500).start();
        fondo[viejo].animate().alpha(0f).setDuration(1500).withEndAction(() -> {
            fondo[viejo].setImageDrawable(null);
            if (bmp[viejo] != null) {
                bmp[viejo].recycle();
                bmp[viejo] = null;
            }
        }).start();
        actual = sig;
        urlMostrada = f.u;
        fotoActual = f;
        mostrarPie();
    }
}
