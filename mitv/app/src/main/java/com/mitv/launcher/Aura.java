package com.mitv.launcher;

import android.os.Build;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Brillo suave al seleccionar: la vista crece un poco y proyecta un resplandor de su misma forma
 * (sombra de color). No agrega vistas ni cambia el tamaño de nada en la pantalla.
 */
final class Aura {
    private static final DecelerateInterpolator SUAVE = new DecelerateInterpolator(2f);
    static final int R_ESCALA = 0x7f0f0002, R_Z = 0x7f0f0003;

    static View envolver(View v, float u, int w, int h, float escala) {
        v.setTag(R_ESCALA, escala);
        v.setTag(R_Z, 22 * u);
        if (Build.VERSION.SDK_INT >= 28) {
            v.setOutlineSpotShadowColor(0xFFC9B8FF);
            v.setOutlineAmbientShadowColor(0xFFC9B8FF);
        }
        return v;
    }

    /** Llamar desde el listener de foco. */
    static void foco(View v, boolean f) {
        Object e = v.getTag(R_ESCALA), z = v.getTag(R_Z);
        float s = e instanceof Float ? (Float) e : 1.06f;
        float zz = z instanceof Float ? (Float) z : 0f;
        v.animate().scaleX(f ? s : 1f).scaleY(f ? s : 1f).translationZ(f ? zz : 0f)
                .setInterpolator(SUAVE).setDuration(240).start();
    }
}
