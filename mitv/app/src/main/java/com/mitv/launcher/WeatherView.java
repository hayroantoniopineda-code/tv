package com.mitv.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Clima de 7 días, dibujado a mano: sin imágenes, casi sin RAM. */
final class WeatherView extends View {
    private static final String[] DIAS = {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
    private final float u;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();
    private String[] dias = new String[0];
    private int[] cod, max, min;
    private int ahoraCod, ahoraT;
    private boolean noche, fahr;

    WeatherView(Context c, float u) {
        super(c);
        this.u = u;
    }

    void actualizar() {
        try {
            JSONObject j = new JSONObject(Datos.p(getContext()).getString("clima", ""));
            JSONObject cur = j.getJSONObject("current");
            JSONObject d = j.getJSONObject("daily");
            JSONArray t = d.getJSONArray("time"), wc = d.getJSONArray("weather_code"),
                    mx = d.getJSONArray("temperature_2m_max"), mn = d.getJSONArray("temperature_2m_min");
            int n = Math.min(7, t.length());
            dias = new String[n];
            cod = new int[n];
            max = new int[n];
            min = new int[n];
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar k = Calendar.getInstance();
            for (int i = 0; i < n; i++) {
                Date dt = f.parse(t.getString(i));
                k.setTime(dt);
                dias[i] = i == 0 ? "Hoy" : DIAS[k.get(Calendar.DAY_OF_WEEK) - 1];
                cod[i] = wc.optInt(i);
                max[i] = (int) Math.round(mx.optDouble(i));
                min[i] = (int) Math.round(mn.optDouble(i));
            }
            ahoraCod = cur.optInt("weather_code");
            ahoraT = (int) Math.round(cur.optDouble("temperature_2m"));
            noche = cur.optInt("is_day", 1) == 0;
            fahr = j.optJSONObject("current_units") != null
                    && j.getJSONObject("current_units").optString("temperature_2m").contains("F");
        } catch (Exception e) {
            dias = new String[0];
        }
        requestLayout();
        invalidate();
    }

    static String desc(int c) {
        if (c == 0) return "Despejado";
        if (c <= 2) return "Parcialmente nublado";
        if (c == 3) return "Nublado";
        if (c == 45 || c == 48) return "Niebla";
        if (c >= 51 && c <= 57) return "Llovizna";
        if ((c >= 61 && c <= 67) || (c >= 80 && c <= 82)) return "Lluvia";
        if ((c >= 71 && c <= 77) || c == 85 || c == 86) return "Nieve";
        if (c >= 95) return "Tormenta";
        return "";
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension((int) (56 * 7 * u), dias.length == 0 ? 0 : (int) (152 * u));
    }

    private void texto(Canvas c, String s, float x, float y, float size, int color, boolean bold, Paint.Align al) {
        p.setStyle(Paint.Style.FILL);
        p.setTextSize(size * u);
        p.setColor(color);
        p.setTypeface(bold ? Typeface.create("sans-serif-medium", Typeface.NORMAL) : Typeface.SANS_SERIF);
        p.setTextAlign(al);
        p.setShadowLayer(3 * u, 0, u, 0x88000000);
        c.drawText(s, x, y, p);
        p.clearShadowLayer();
    }

    private int colorTemp(float t) {
        if (fahr) t = (t - 32f) * 5f / 9f;
        float[][] st = {{0, 110, 170, 255}, {8, 120, 210, 230}, {14, 255, 210, 110}, {22, 255, 140, 80}};
        if (t <= st[0][0]) return 0xFF000000 | (110 << 16) | (170 << 8) | 255;
        for (int i = 0; i < st.length - 1; i++) {
            if (t <= st[i + 1][0]) {
                float k = (t - st[i][0]) / (st[i + 1][0] - st[i][0]);
                int rr = (int) (st[i][1] + (st[i + 1][1] - st[i][1]) * k);
                int gg = (int) (st[i][2] + (st[i + 1][2] - st[i][2]) * k);
                int bb = (int) (st[i][3] + (st[i + 1][3] - st[i][3]) * k);
                return 0xFF000000 | (rr << 16) | (gg << 8) | bb;
            }
        }
        return 0xFFFF8C50;
    }

    private void sol(Canvas c, float x, float y, float rr) {
        p.setColor(0xFFFFC94D);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.2f * u);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            c.drawLine(x + (float) Math.cos(a) * rr * 1.35f, y + (float) Math.sin(a) * rr * 1.35f,
                    x + (float) Math.cos(a) * rr * 1.8f, y + (float) Math.sin(a) * rr * 1.8f, p);
        }
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(x, y, rr, p);
    }

    private void luna(Canvas c, float x, float y, float rr) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFEDEFFA);
        path.reset();
        path.addCircle(x, y, rr, Path.Direction.CW);
        Path cut = new Path();
        cut.addCircle(x + rr * 0.55f, y - rr * 0.35f, rr * 0.85f, Path.Direction.CW);
        path.op(cut, Path.Op.DIFFERENCE);
        c.drawPath(path, p);
    }

    private void nube(Canvas c, float x, float y, float w, int color) {
        float h = w * 0.36f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        r.set(x - w / 2, y, x + w / 2, y + h);
        c.drawRoundRect(r, h / 2, h / 2, p);
        c.drawCircle(x - w * 0.15f, y + h * 0.1f, h * 0.65f, p);
        c.drawCircle(x + w * 0.13f, y - h * 0.1f, h * 0.85f, p);
    }

    private void icono(Canvas c, int code, float x, float y, boolean deNoche) {
        String d = desc(code);
        if (d.equals("Despejado")) {
            if (deNoche) luna(c, x, y, 8 * u);
            else sol(c, x, y, 7.5f * u);
        } else if (d.equals("Parcialmente nublado")) {
            if (deNoche) luna(c, x + 5 * u, y - 5 * u, 7 * u);
            else sol(c, x + 5 * u, y - 5 * u, 6.5f * u);
            nube(c, x - 2 * u, y + u, 24 * u, 0xFFEEF2FA);
        } else if (d.equals("Nublado") || d.equals("Niebla")) {
            nube(c, x + 4 * u, y - 4 * u, 18 * u, 0xFFAAB6CD);
            nube(c, x - 2 * u, y + u, 24 * u, 0xFFEEF2FA);
        } else {
            nube(c, x, y - 4 * u, 24 * u, 0xFFEEF2FA);
            p.setStrokeWidth(2.2f * u);
            if (d.equals("Nieve")) {
                p.setColor(0xFFFFFFFF);
                for (int k = -1; k <= 1; k++) c.drawCircle(x + k * 7 * u, y + 10 * u, 1.8f * u, p);
            } else if (d.equals("Tormenta")) {
                p.setColor(0xFFFFD34D);
                path.reset();
                path.moveTo(x + u, y + 5 * u);
                path.lineTo(x - 4 * u, y + 11 * u);
                path.lineTo(x, y + 11 * u);
                path.lineTo(x - 3 * u, y + 17 * u);
                path.lineTo(x + 4 * u, y + 9 * u);
                path.lineTo(x, y + 9 * u);
                path.close();
                c.drawPath(path, p);
            } else {
                p.setColor(0xFF6EAFFF);
                p.setStyle(Paint.Style.STROKE);
                for (int k = -1; k <= 1; k++)
                    c.drawLine(x + k * 7 * u, y + 7 * u, x + k * 7 * u - 3 * u, y + 13 * u, p);
            }
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        if (dias.length == 0) return;
        float x0 = 0, cw = 56 * u;
        icono(c, ahoraCod, x0 + 14 * u, 14 * u, noche);
        String a = "Ahora " + ahoraT + "°";
        texto(c, a, x0 + 34 * u, 24 * u, 22, 0xFFFFFFFF, true, Paint.Align.LEFT);
        p.setTextSize(22 * u);
        float wa = p.measureText(a);
        texto(c, "· " + desc(ahoraCod), x0 + 40 * u + wa, 24 * u, 14, 0xB4FFFFFF, false, Paint.Align.LEFT);
        int lo = 999, hi = -999;
        for (int i = 0; i < dias.length; i++) {
            lo = Math.min(lo, min[i]);
            hi = Math.max(hi, max[i]);
        }
        if (hi == lo) hi = lo + 1;
        float top = 40 * u;
        for (int i = 0; i < dias.length; i++) {
            float cx = x0 + cw * i + cw / 2;
            texto(c, dias[i], cx, top + 18 * u, 13.5f, i == 0 ? 0xFFFFFFFF : 0xB4FFFFFF, true, Paint.Align.CENTER);
            icono(c, cod[i], cx, top + 42 * u, false);
            texto(c, max[i] + "°", cx, top + 78 * u, 15.5f, 0xFFFFFFFF, true, Paint.Align.CENTER);
            float bx = cx - 17 * u, bw = 34 * u, by = top + 86 * u;
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x2DFFFFFF);
            r.set(bx, by, bx + bw, by + 4 * u);
            c.drawRoundRect(r, 2 * u, 2 * u, p);
            float a0 = bx + (min[i] - lo) / (float) (hi - lo) * bw, a1 = bx + (max[i] - lo) / (float) (hi - lo) * bw;
            p.setShader(new android.graphics.LinearGradient(a0, 0, Math.max(a1, a0 + 1), 0,
                    colorTemp(min[i]), colorTemp(max[i]), android.graphics.Shader.TileMode.CLAMP));
            r.set(a0, by, Math.max(a1, a0 + 2 * u), by + 4 * u);
            c.drawRoundRect(r, 2 * u, 2 * u, p);
            p.setShader(null);
            texto(c, min[i] + "°", cx, top + 106 * u, 13, 0xB4FFFFFF, false, Paint.Align.CENTER);
        }
    }
}
