package com.mitv.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Panel lateral elegante (ajustes, relojes, VPN). Se maneja con las flechas y "Atrás". */
final class Panel extends FrameLayout {
    interface Pagina {
        void llenar(Panel p);
    }

    private final float u;
    private final LinearLayout caja, lista;
    private final TextView titulo, sub;
    private final ScrollView scroll;
    private final List<Pagina> pila = new ArrayList<>();
    private Runnable alCerrar;

    Panel(Context c, float u) {
        super(c);
        this.u = u;
        setBackgroundColor(0x66000000);
        setClipChildren(false);
        setClickable(true);
        caja = new LinearLayout(c);
        caja.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF51A1E3D, 0xF50E1126});
        g.setCornerRadii(new float[]{28 * u, 28 * u, 0, 0, 0, 0, 28 * u, 28 * u});
        caja.setBackground(g);
        caja.setPadding((int) (40 * u), (int) (46 * u), (int) (40 * u), (int) (30 * u));
        titulo = Pantalla.txt(c, u, 34, 0xFFFFFFFF, true);
        sub = Pantalla.txt(c, u, 16, 0x99FFFFFF, false);
        sub.setPadding(0, (int) (8 * u), 0, (int) (22 * u));
        caja.addView(titulo);
        caja.addView(sub);
        scroll = new ScrollView(c);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        lista = new LinearLayout(c);
        lista.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(lista);
        caja.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        addView(caja, new LayoutParams((int) (600 * u), -1, Gravity.RIGHT));
        setVisibility(GONE);
    }

    boolean abierto() {
        return getVisibility() == VISIBLE;
    }

    void abrir(Pagina p, Runnable alCerrar) {
        this.alCerrar = alCerrar;
        pila.clear();
        pila.add(p);
        setVisibility(VISIBLE);
        caja.setTranslationX(80 * u);
        caja.setAlpha(0f);
        caja.animate().translationX(0).alpha(1f).setDuration(220).start();
        mostrar();
    }

    void ir(Pagina p) {
        pila.add(p);
        mostrar();
    }

    /** Vuelve a dibujar la página actual (después de un cambio). */
    void mostrar() {
        lista.removeAllViews();
        titulo.setText("");
        sub.setText("");
        sub.setVisibility(GONE);
        pila.get(pila.size() - 1).llenar(this);
        scroll.scrollTo(0, 0);
        lista.setAlpha(0f);
        lista.setTranslationX(24 * u);
        lista.animate().alpha(1f).translationX(0).setDuration(180).start();
        for (int i = 0; i < lista.getChildCount(); i++) {
            View v = lista.getChildAt(i);
            if (v.isFocusable()) {
                v.requestFocus();
                break;
            }
        }
    }

    void atras() {
        if (pila.size() > 1) {
            pila.remove(pila.size() - 1);
            mostrar();
        } else cerrar();
    }

    void cerrar() {
        if (!abierto()) return;
        pila.clear();
        caja.animate().translationX(80 * u).alpha(0f).setDuration(200).withEndAction(() -> {
            if (pila.isEmpty()) setVisibility(GONE);
        }).start();
        if (alCerrar != null) alCerrar.run();
    }

    // ---------- piezas ----------
    void titulo(String t, String s) {
        titulo.setText(t);
        if (s != null && !s.isEmpty()) {
            sub.setText(s);
            sub.setVisibility(VISIBLE);
        }
    }

    void seccion(String t) {
        TextView v = Pantalla.txt(getContext(), u, 14, 0x80FFFFFF, true);
        v.setText(t.toUpperCase());
        v.setLetterSpacing(0.08f);
        v.setPadding((int) (18 * u), (int) (20 * u), 0, (int) (8 * u));
        lista.addView(v);
    }

    void vista(View v) {
        lista.addView(v);
    }

    void nota(String t) {
        TextView v = Pantalla.txt(getContext(), u, 15, 0x99FFFFFF, false);
        v.setText(t);
        v.setPadding((int) (18 * u), (int) (10 * u), (int) (18 * u), (int) (10 * u));
        lista.addView(v);
    }

    /** Una fila: ícono, título, valor debajo y un indicador a la derecha (›, ✓ o nada). */
    View fila(String icono, String t, String valor, String derecha, final Runnable accion) {
        Context c = getContext();
        final LinearLayout f = new LinearLayout(c);
        f.setOrientation(LinearLayout.HORIZONTAL);
        f.setGravity(Gravity.CENTER_VERTICAL);
        f.setPadding((int) (18 * u), (int) (14 * u), (int) (20 * u), (int) (14 * u));
        f.setFocusable(true);
        final TextView ic = Pantalla.txt(c, u, 26, 0xFFFFFFFF, false);
        ic.setText(icono == null ? "" : icono);
        ic.setGravity(Gravity.CENTER);
        f.addView(ic, new LinearLayout.LayoutParams((int) (46 * u), -2));
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding((int) (14 * u), 0, (int) (10 * u), 0);
        final TextView tt = Pantalla.txt(c, u, 21, 0xFFFFFFFF, true);
        tt.setText(t);
        tt.setSingleLine(true);
        tt.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(tt);
        final TextView vv = Pantalla.txt(c, u, 15, 0xA6FFFFFF, false);
        if (valor != null && !valor.isEmpty()) {
            vv.setText(valor);
            vv.setPadding(0, (int) (4 * u), 0, 0);
            col.addView(vv);
        }
        f.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView der = Pantalla.txt(c, u, derecha != null && derecha.equals("✓") ? 24 : 30, 0x99FFFFFF, false);
        der.setText(derecha == null ? "" : derecha);
        if ("✓".equals(derecha)) der.setTextColor(0xFF46D778);
        f.addView(der);
        final int colorDer = der.getCurrentTextColor();
        f.setBackground(fondo(false));
        f.setOnFocusChangeListener((v, foc) -> {
            v.animate().scaleX(foc ? 1.03f : 1f).scaleY(foc ? 1.03f : 1f).setDuration(180).start();
            v.setElevation(foc ? 14 * u : 0);
            v.setBackground(fondo(foc));
            int oscuro = 0xFF14162A;
            tt.setTextColor(foc ? oscuro : 0xFFFFFFFF);
            vv.setTextColor(foc ? 0xCC14162A : 0xA6FFFFFF);
            ic.setTextColor(foc ? oscuro : 0xFFFFFFFF);
            der.setTextColor(foc && !"✓".equals(derecha) ? oscuro : colorDer);
            for (TextView x : new TextView[]{tt, vv, ic, der}) x.setShadowLayer(foc ? 0 : 4 * u, 0, foc ? 0 : 2 * u, 0x88000000);
        });
        f.setOnClickListener(v -> {
            if (accion != null) accion.run();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = (int) (6 * u);
        lista.addView(f, lp);
        return f;
    }

    private GradientDrawable fondo(boolean foco) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(18 * u);
        g.setColor(foco ? 0xF2FFFFFF : 0x14FFFFFF);
        return g;
    }

    /** Lista para elegir una opción, con ✓ en la actual. */
    void opciones(final String t, final String s, final String[] op, final int actual, final Elegir e) {
        ir(p -> {
            p.titulo(t, s);
            for (int i = 0; i < op.length; i++) {
                final int k = i;
                p.fila(null, op[i], null, i == actual ? "✓" : "", () -> {
                    e.elegido(k);
                    atras();
                });
            }
        });
    }

    interface Elegir {
        void elegido(int i);
    }

    static float px(Context c, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    static Typeface medio() {
        return Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }
}
