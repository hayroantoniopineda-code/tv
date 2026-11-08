package com.mitv.launcher;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Red mínima: texto e imágenes, sin librerías. */
final class Net {
    static final String UA = "MiTV-launcher/1.0 (https://github.com/hayroantoniopineda-code/tv)";

    private static byte[] bytes(String u) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(25000);
        c.setRequestProperty("User-Agent", UA);
        try (InputStream in = new BufferedInputStream(c.getInputStream())) {
            ByteArrayOutputStream b = new ByteArrayOutputStream(64 * 1024);
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            return b.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    static String text(String u) throws IOException {
        return new String(bytes(u), "UTF-8");
    }

    /** Decodifica en RGB_565 y reducida al ancho de la pantalla: poca RAM. */
    static Bitmap image(String u, int maxW) throws IOException {
        byte[] d = bytes(u);
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(d, 0, d.length, o);
        int s = 1;
        while (o.outWidth / (s * 2) >= maxW) s *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = s;
        o2.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeByteArray(d, 0, d.length, o2);
    }
}
