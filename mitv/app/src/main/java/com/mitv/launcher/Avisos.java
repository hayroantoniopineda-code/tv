package com.mitv.launcher;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;

/**
 * Con el permiso de "acceso a notificaciones" (se da una sola vez) el inicio puede ver
 * lo que se está reproduciendo o quedó en pausa, y los avisos nuevos de las apps.
 */
public class Avisos extends NotificationListenerService {
    static volatile Avisos yo;

    @Override
    public void onListenerConnected() {
        yo = this;
    }

    @Override
    public void onListenerDisconnected() {
        yo = null;
    }

    static boolean permitido(Context c) {
        String s = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(c.getPackageName());
    }

    static final class Item {
        String pkg, titulo, sub, etiqueta;
        Bitmap imagen;
        float progreso = -1;
        MediaController control;
        Notification aviso;
    }

    /** Lo que se reproduce o quedó en pausa (YouTube y apps que lo comparten). */
    static List<Item> medios(Context c) {
        List<Item> l = new ArrayList<>();
        if (!permitido(c)) return l;
        try {
            MediaSessionManager m = (MediaSessionManager) c.getSystemService(Context.MEDIA_SESSION_SERVICE);
            for (MediaController mc : m.getActiveSessions(new ComponentName(c, Avisos.class))) {
                MediaMetadata md = mc.getMetadata();
                if (md == null) continue;
                Item it = new Item();
                it.pkg = mc.getPackageName();
                it.control = mc;
                CharSequence t = md.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
                if (t == null) t = md.getText(MediaMetadata.METADATA_KEY_TITLE);
                it.titulo = t == null ? "" : t.toString();
                CharSequence s = md.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);
                if (s == null) s = md.getText(MediaMetadata.METADATA_KEY_ARTIST);
                it.sub = s == null ? "" : s.toString();
                it.imagen = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if (it.imagen == null) it.imagen = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (it.imagen == null) it.imagen = md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                long dur = md.getLong(MediaMetadata.METADATA_KEY_DURATION);
                PlaybackState ps = mc.getPlaybackState();
                if (ps != null) {
                    it.etiqueta = ps.getState() == PlaybackState.STATE_PLAYING ? "Reproduciendo" : "En pausa";
                    if (dur > 0) {
                        it.progreso = Math.min(1f, ps.getPosition() / (float) dur);
                        long rest = (dur - ps.getPosition()) / 60000L;
                        if (rest > 0) it.etiqueta += " · quedan " + rest + " min";
                    }
                } else it.etiqueta = "";
                if (!it.titulo.isEmpty()) l.add(it);
            }
        } catch (Exception ignored) {
        }
        return l;
    }

    /** Avisos recientes de las apps (videos nuevos, noticias, partidos en vivo…). */
    static List<Item> avisos(Context c) {
        List<Item> l = new ArrayList<>();
        Avisos a = yo;
        if (a == null) return l;
        try {
            for (StatusBarNotification sbn : a.getActiveNotifications()) {
                if (sbn.getPackageName().equals(c.getPackageName()) || !sbn.isClearable() && sbn.isOngoing()) continue;
                Bundle x = sbn.getNotification().extras;
                CharSequence t = x.getCharSequence(Notification.EXTRA_TITLE), s = x.getCharSequence(Notification.EXTRA_TEXT);
                if (t == null) continue;
                Item it = new Item();
                it.pkg = sbn.getPackageName();
                it.titulo = t.toString();
                it.sub = s == null ? "" : s.toString();
                it.aviso = sbn.getNotification();
                long min = (System.currentTimeMillis() - sbn.getPostTime()) / 60000L;
                it.etiqueta = min < 60 ? "NUEVO" : "";
                l.add(it);
            }
        } catch (Exception ignored) {
        }
        return l;
    }

    /** Pausa lo que se esté reproduciendo (para el temporizador). */
    static void pausarTodo(Context c) {
        for (Item it : medios(c)) {
            try {
                it.control.getTransportControls().pause();
            } catch (Exception ignored) {
            }
        }
    }
}
