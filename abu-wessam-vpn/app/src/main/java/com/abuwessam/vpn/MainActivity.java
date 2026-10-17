package com.abuwessam.vpn;

import android.app.Activity;
import android.os.Bundle;
import android.os.StrictMode;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class MainActivity extends Activity {
    private final int navy = Color.rgb(11, 22, 44);
    private final int teal = Color.rgb(0, 190, 170);
    private LinearLayout list;
    private TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<Server> servers = new ArrayList<>();

    static class Server {
        String host, ip, country, countryCode, ping, speed, config;
        Server(String[] c) {
            host = val(c,0); ip = val(c,1); ping = val(c,3);
            speed = val(c,4); country = val(c,5); countryCode = val(c,6);
            config = val(c,14);
        }
        static String val(String[] a, int i) { return i < a.length ? a[i] : ""; }
        String speedText() {
            try { return String.format(Locale.US, "%.1f Mbps", Double.parseDouble(speed)/1000000.0); }
            catch (Exception e) { return "غير معروف"; }
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        buildUi();
        loadServers();
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(8, 8, 8, 8);
        return t;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245,247,251));
        root.setPadding(18, 18, 18, 18);
        scroll.addView(root);
        setContentView(scroll);

        TextView brand = text("🛡  أبو وسام VPN", 28, Color.WHITE, true);
        brand.setBackgroundColor(navy);
        root.addView(brand, new LinearLayout.LayoutParams(-1, 105));

        TextView subtitle = text("سيرفرات عامة مجانية • تتغير حسب التوفر", 14, Color.DKGRAY, false);
        root.addView(subtitle);

        Button refresh = new Button(this);
        refresh.setText("تحديث قائمة السيرفرات");
        refresh.setTextColor(Color.WHITE);
        refresh.setBackgroundTintList(android.content.res.ColorStateList.valueOf(teal));
        root.addView(refresh, new LinearLayout.LayoutParams(-1, -2));
        refresh.setOnClickListener(v -> loadServers());

        status = text("جارٍ تحميل السيرفرات…", 14, navy, true);
        root.addView(status);

        TextView warning = text("تنبيه: هذه نسخة أولية تعرض السيرفرات وتتيح تصدير ملف الإعداد. الاتصال الفعلي يتم عبر عميل OpenVPN متوافق بعد استيراد الملف، وليس داخل التطبيق بعد.", 13, Color.rgb(110,70,0), false);
        warning.setBackgroundColor(Color.rgb(255,243,205));
        root.addView(warning);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        TextView footer = text("المصدر: VPN Gate العام. السيرفرات تطوعية وقد تكون بطيئة أو غير آمنة للاستخدام الحساس.", 12, Color.GRAY, false);
        root.addView(footer);
    }

    private void loadServers() {
        status.setText("جارٍ الاتصال بمصدر السيرفرات…");
        list.removeAllViews();
        new Thread(() -> {
            ArrayList<Server> found = new ArrayList<>();
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://www.vpngate.net/api/iphone/");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "AbuWessamVPN/1.0");
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String line; boolean headerSeen = false;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("*")) continue;
                    if (!headerSeen) { headerSeen = true; continue; }
                    String[] cols = parseCsv(line);
                    if (cols.length >= 15 && !cols[14].isEmpty()) {
                        Server s = new Server(cols);
                        if (!s.ip.isEmpty() && !s.config.isEmpty()) found.add(s);
                    }
                }
                br.close();
                Collections.sort(found, (a,b) -> {
                    long sa = parseLong(a.speed), sb = parseLong(b.speed);
                    return Long.compare(sb, sa);
                });
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "تعذر الوصول إلى المصدر" : e.getMessage();
                handler.post(() -> status.setText("تعذر تحميل السيرفرات: " + msg));
            } finally { if (conn != null) conn.disconnect(); }
            handler.post(() -> {
                servers.clear(); servers.addAll(found);
                if (!found.isEmpty()) {
                    status.setText("السيرفرات المتاحة الآن: " + found.size() + " — مرتبة حسب السرعة المعلنة");
                    showServers();
                } else if (status.getText().toString().startsWith("تعذر")) {
                    // Preserve the connection error message.
                } else {
                    status.setText("لم تصل قائمة سيرفرات. اضغط تحديث وحاول لاحقًا.");
                }
            });
        }).start();
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0; }
    }

    private String[] parseCsv(String line) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i=0; i<line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i+1 < line.length() && line.charAt(i+1) == '"') { cur.append('"'); i++; }
                else quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                out.add(cur.toString()); cur.setLength(0);
            } else cur.append(ch);
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private void showServers() {
        list.removeAllViews();
        int count = Math.min(40, servers.size());
        for (int i=0; i<count; i++) {
            Server s = servers.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(12, 10, 12, 10);
            card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.setMargins(0, 8, 0, 8);
            list.addView(card, cp);

            TextView title = text("🌐 " + s.country + " (" + s.countryCode + ")", 18, navy, true);
            card.addView(title);
            TextView details = text("IP: " + s.ip + "\nPing: " + (s.ping.isEmpty() ? "غير معروف" : s.ping + " ms") + "   •   السرعة المعلنة: " + s.speedText(), 13, Color.DKGRAY, false);
            card.addView(details);

            Button export = new Button(this);
            export.setText("تصدير إعداد OpenVPN لهذا السيرفر");
            card.addView(export);
            export.setOnClickListener(v -> exportConfig(s));
        }
    }

    private void exportConfig(Server s) {
        try {
            byte[] decoded = android.util.Base64.decode(s.config, android.util.Base64.DEFAULT);
            File file = new File(getExternalFilesDir(null), "AbuWessam-" + s.countryCode + "-" + s.ip.replace(".","_") + ".ovpn");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(decoded); fos.close();

            Uri uri = Uri.fromFile(file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/x-openvpn-profile");
            share.putExtra(Intent.EXTRA_SUBJECT, "Abu Wessam VPN - " + s.country);
            share.putExtra(Intent.EXTRA_TEXT, "ملف إعداد OpenVPN لسيرفر عام مجاني. استورده في تطبيق OpenVPN متوافق.");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // File URI access is not supported by many receiving apps; use a content URI through chooser fallback.
            Intent chooser = Intent.createChooser(share, "افتح أو أرسل ملف إعداد VPN");
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تجهيز ملف السيرفر: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
