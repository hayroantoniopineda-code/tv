package com.mitv.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Estado de la VPN y panel que abre la app de VPN del usuario. */
final class Vpn {
    static final String[][] APPS = {
            {"Proton VPN", "ch.protonvpn.android"}, {"NordVPN", "com.nordvpn.android"},
            {"Surfshark", "com.surfshark.vpnclient.android"}, {"ExpressVPN", "com.expressvpn.vpn"},
            {"Private Internet Access", "com.privateinternetaccess.android"}, {"Windscribe", "com.windscribe.vpn"},
            {"1.1.1.1 WARP", "com.cloudflare.onedotonedotonedotone"}, {"WireGuard", "com.wireguard.android"},
            {"OpenVPN", "net.openvpn.openvpn"}
    };
    static final String[] PAISES = {"Estados Unidos", "Honduras", "México", "Canadá", "España", "Reino Unido",
            "Alemania", "Francia", "Japón", "Brasil", "Colombia", "Argentina"};

    static boolean activa(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork();
            if (n == null) return false;
            NetworkCapabilities nc = cm.getNetworkCapabilities(n);
            return nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        } catch (Exception e) {
            return false;
        }
    }

    static Intent lanzar(Context c, String pkg) {
        PackageManager pm = c.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        return i;
    }

    static List<String[]> instaladas(Context c) {
        List<String[]> l = new ArrayList<>();
        for (String[] a : APPS) if (lanzar(c, a[1]) != null) l.add(a);
        return l;
    }

    static void abrirApp(Activity a) {
        String pkg = Datos.p(a).getString("vpn_app", "");
        if (pkg.isEmpty()) {
            List<String[]> l = instaladas(a);
            if (!l.isEmpty()) pkg = l.get(0)[1];
        }
        Intent i = pkg.isEmpty() ? null : lanzar(a, pkg);
        if (i == null) {
            Toast.makeText(a, "Instala una app de VPN (Proton, NordVPN, Surfshark…) y elígela en Ajustes", Toast.LENGTH_LONG).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        a.startActivity(i);
    }

    static void panel(final MainActivity a) {
        final SharedPreferences p = Datos.p(a);
        final String ult = p.getString("vpn_ultimo", "");
        final boolean on = activa(a);
        final List<String> items = new ArrayList<>();
        if (!ult.isEmpty()) items.add("↻  Reconectar: " + ult);
        for (String s : PAISES) items.add(s);
        items.add(on ? "Desconectar" : "Abrir mi app de VPN");
        new AlertDialog.Builder(a)
                .setTitle(on ? "VPN conectada" : "VPN desconectada")
                .setItems(items.toArray(new String[0]), (d, w) -> {
                    int base = ult.isEmpty() ? 0 : 1;
                    if (w >= base && w < base + PAISES.length) {
                        p.edit().putString("vpn_ultimo", PAISES[w - base]).apply();
                        Toast.makeText(a, "Elige " + PAISES[w - base] + " en tu app de VPN", Toast.LENGTH_LONG).show();
                    }
                    abrirApp(a);
                }).show();
    }
}
