package com.mitv.launcher;

import android.service.dreams.DreamService;

/** Salvapantallas del sistema: mismos paisajes, relojes y clima. */
public class Salvapantallas extends DreamService {
    private Pantalla pan;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        pan = new Pantalla(this, true);
        setContentView(pan);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        pan.iniciar();
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        pan.detener();
    }
}
