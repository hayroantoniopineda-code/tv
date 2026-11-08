package com.mitv.launcher;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

/** Brillo suave detrás de lo que tiene el foco: entra y sale con animación, sin cortes. */
final class Aura {
    private static final DecelerateInterpolator SUAVE = new DecelerateInterpolator(2f);

    /** Envuelve v (de tamaño w×h; -2 = su tamaño) con un aura. Devuelve el contenedor. */
    static FrameLayout envolver(View v, float u, int w, int h, float escala) {
        FrameLayout c = new FrameLayout(v.getContext());
        c.setClipChildren(false);
        c.setClipToPadding(false);
        View aura = new View(v.getContext());
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setColors(new int[]{0x88CDBFFF, 0x33A99BFF, 0x00A99BFF});
        g.setGradientRadius(Math.max(w > 0 ? w : 140 * u, h > 0 ? h : 60 * u) * 0.78f);
        aura.setBackground(g);
        aura.setAlpha(0f);
        int pad = (int) (46 * u);
        FrameLayout.LayoutParams al = new FrameLayout.LayoutParams(w > 0 ? w + pad * 2 : -1, h > 0 ? h + pad * 2 : -1, Gravity.CENTER);
        if (w <= 0) al.leftMargin = al.rightMargin = -pad;
        if (h <= 0) al.topMargin = al.bottomMargin = -pad;
        c.addView(aura, al);
        c.addView(v, new FrameLayout.LayoutParams(w > 0 ? w : -2, h > 0 ? h : -2, Gravity.CENTER));
        v.setTag(R_AURA, aura);
        v.setTag(R_ESCALA, escala);
        return c;
    }

    static final int R_AURA = 0x7f0f0001, R_ESCALA = 0x7f0f0002;

    /** Llamar desde el listener de foco. */
    static void foco(View v, boolean f) {
        Object a = v.getTag(R_AURA), e = v.getTag(R_ESCALA);
        float s = e instanceof Float ? (Float) e : 1.08f;
        v.animate().scaleX(f ? s : 1f).scaleY(f ? s : 1f).setInterpolator(SUAVE).setDuration(240).start();
        if (a instanceof View) {
            View aura = (View) a;
            aura.animate().alpha(f ? 1f : 0f).scaleX(f ? s : 0.9f).scaleY(f ? s : 0.9f)
                    .setInterpolator(SUAVE).setDuration(f ? 260 : 200).start();
        }
    }
}
