package com.abuwessam.share;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import androidx.core.content.FileProvider;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.app.DownloadManager;
import android.os.Environment;

import fi.iki.elonen.NanoHTTPD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_FILES = 41;
    private final List<Uri> selectedUris = Collections.synchronizedList(new ArrayList<Uri>());
    private final List<String> selectedNames = Collections.synchronizedList(new ArrayList<String>());
    private TextView status;
    private TextView filesView;
    private ImageView qrView;
    private ShareServer server;
    private boolean highSpeed = true;
    private volatile boolean discoveryRunning = false;
    private Thread discoveryThread;
    private final android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private android.webkit.ValueCallback<Uri[]> fileChooserCallback;
    private final android.os.Handler speedHandler = new android.os.Handler();
    private final Runnable speedTicker = new Runnable() {
        @Override public void run() {
            if (server != null && status != null) {
                status.setText("سرعة النقل: " + formatRate(server.transferSpeed) + (server.transferTotal > 0 ? String.format(java.util.Locale.US, " — %.0f%%", Math.min(100.0, server.transferBytes * 100.0 / server.transferTotal)) : "") + "\n" + server.transferLabel);
                speedHandler.postDelayed(this, 500);
            }
        }
    };
    private String formatRate(double rate) {
        if (rate >= 1048576) return String.format(java.util.Locale.US, "%.2f MB/s", rate / 1048576.0);
        if (rate >= 1024) return String.format(java.util.Locale.US, "%.1f KB/s", rate / 1024.0);
        return String.format(java.util.Locale.US, "%.0f B/s", rate);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF102A43);
        getWindow().setNavigationBarColor(0xFF102A43);
        buildUi();
    }

    private void buildUi() {
        // Classic SHAREit-inspired layout, recolored and branded for Abu Wessam.
        final int ink = 0xFF24213A;
        final int purple = 0xFF6C42C1;
        final int orange = 0xFFFF8A3D;
        final int pale = 0xFFF6F3FC;
        getWindow().setStatusBarColor(0xFF56339D);
        getWindow().setNavigationBarColor(0xFF56339D);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(pale);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, dp(18));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        // Top navigation bar similar to the original SHAREit home screen.
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16), dp(10), dp(16), dp(10));
        top.setBackgroundColor(0xFF56339D);
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(58)));

        TextView menu = new TextView(this);
        menu.setText("☰");
        menu.setTextSize(25);
        menu.setTextColor(Color.WHITE);
        menu.setGravity(Gravity.CENTER);
        top.addView(menu, new LinearLayout.LayoutParams(dp(42), -1));

        TextView brand = new TextView(this);
        brand.setText("أبو وسام SHARE");
        brand.setTextSize(21);
        brand.setTypeface(null, 1);
        brand.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams brandP = new LinearLayout.LayoutParams(0, -2, 1);
        brandP.leftMargin = dp(8);
        top.addView(brand, brandP);

        TextView history = new TextView(this);
        history.setText("◷");
        history.setTextSize(27);
        history.setTextColor(Color.WHITE);
        history.setGravity(Gravity.CENTER);
        history.setOnClickListener(v -> Toast.makeText(this, "سجل النقل: ستظهر هنا عمليات النقل أثناء الجلسة", Toast.LENGTH_LONG).show());
        top.addView(history, new LinearLayout.LayoutParams(dp(42), -1));

        // Profile / transfer summary banner.
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(roundBg(0xFF56339D, dp(0)));
        root.addView(hero, new LinearLayout.LayoutParams(-1, dp(142)));

        TextView avatar = new TextView(this);
        avatar.setText("AW");
        avatar.setTextSize(25);
        avatar.setTypeface(null, 1);
        avatar.setTextColor(0xFF56339D);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(roundBg(Color.WHITE, dp(50)));
        hero.addView(avatar, new LinearLayout.LayoutParams(dp(72), dp(72)));

        LinearLayout heroText = new LinearLayout(this);
        heroText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams heroTextP = new LinearLayout.LayoutParams(0, -2, 1);
        heroTextP.leftMargin = dp(14);
        hero.addView(heroText, heroTextP);

        TextView greeting = new TextView(this);
        greeting.setText("مرحبًا بك في أبو وسام SHARE");
        greeting.setTextSize(16);
        greeting.setTypeface(null, 1);
        greeting.setTextColor(Color.WHITE);
        heroText.addView(greeting);

        TextView subtitle = new TextView(this);
        subtitle.setText("أرسل الصور والفيديوهات والملفات والتطبيقات");
        subtitle.setTextSize(12);
        subtitle.setTextColor(0xFFE8DFFC);
        subtitle.setPadding(0, dp(6), 0, 0);
        heroText.addView(subtitle);

        TextView transferNote = new TextView(this);
        transferNote.setText("نقل محلي عبر Wi-Fi • دون إنترنت");
        transferNote.setTextSize(12);
        transferNote.setTextColor(0xFFFFD4B8);
        transferNote.setPadding(0, dp(8), 0, 0);
        heroText.addView(transferNote);

        TextView section = new TextView(this);
        section.setText("مشاركة الملفات");
        section.setTextSize(17);
        section.setTypeface(null, 1);
        section.setTextColor(ink);
        section.setPadding(dp(20), dp(18), dp(20), dp(8));
        root.addView(section);

        // The two large primary actions are intentionally prominent, like SHAREit's original home.
        Button send = makeButton("➤   إرسال", purple);
        send.setTextSize(20);
        send.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams actionP = new LinearLayout.LayoutParams(-1, dp(62));
        actionP.setMargins(dp(18), dp(2), dp(18), dp(10));
        root.addView(send, actionP);
        send.setOnClickListener(v -> showDeviceDiscovery());

        Button receive = makeButton("⬇   استقبال", orange);
        receive.setTextSize(20);
        receive.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams receiveP = new LinearLayout.LayoutParams(-1, dp(62));
        receiveP.setMargins(dp(18), 0, dp(18), dp(14));
        root.addView(receive, receiveP);
        receive.setOnClickListener(v -> startSharing());

        // Content-type shortcuts, arranged as a classic transfer app category strip.
        LinearLayout categories = new LinearLayout(this);
        categories.setOrientation(LinearLayout.HORIZONTAL);
        categories.setGravity(Gravity.CENTER);
        categories.setPadding(dp(10), dp(10), dp(10), dp(10));
        categories.setBackgroundColor(Color.WHITE);
        root.addView(categories, new LinearLayout.LayoutParams(-1, dp(94)));
        String[] categoryNames = {"الصور", "الفيديو", "الموسيقى", "التطبيقات", "الملفات"};
        String[] categoryIcons = {"▧", "▶", "♫", "▣", "▤"};
        for (int i = 0; i < categoryNames.length; i++) {
            final String cat = categoryNames[i];
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            categories.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
            TextView glyph = new TextView(this);
            glyph.setText(categoryIcons[i]);
            glyph.setTextSize(24);
            glyph.setTextColor(i % 2 == 0 ? purple : orange);
            glyph.setGravity(Gravity.CENTER);
            item.addView(glyph, new LinearLayout.LayoutParams(-1, dp(36)));
            TextView label = new TextView(this);
            label.setText(cat);
            label.setTextSize(10);
            label.setTextColor(ink);
            label.setGravity(Gravity.CENTER);
            item.addView(label, new LinearLayout.LayoutParams(-1, dp(26)));
            item.setOnClickListener(v -> {
                Toast.makeText(this, "اختر الملفات من الهاتف ثم استخدم الإرسال. التصنيف: " + cat, Toast.LENGTH_SHORT).show();
                chooseFiles();
            });
        }

        LinearLayout speedCard = new LinearLayout(this);
        speedCard.setOrientation(LinearLayout.VERTICAL);
        speedCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        speedCard.setBackground(roundBg(Color.WHITE, dp(16)));
        LinearLayout.LayoutParams speedCardP = new LinearLayout.LayoutParams(-1, -2);
        speedCardP.setMargins(dp(18), dp(14), dp(18), dp(8));
        root.addView(speedCard, speedCardP);

        TextView speedTitle = new TextView(this);
        speedTitle.setText("وضع سرعة النقل");
        speedTitle.setTextSize(14);
        speedTitle.setTypeface(null, 1);
        speedTitle.setTextColor(ink);
        speedCard.addView(speedTitle);

        LinearLayout speeds = new LinearLayout(this);
        speeds.setOrientation(LinearLayout.HORIZONTAL);
        speeds.setPadding(0, dp(8), 0, 0);
        speedCard.addView(speeds, new LinearLayout.LayoutParams(-1, -2));
        Button fast = makeButton("⚡ سرعة عالية", purple);
        Button normal = makeButton("سرعة عادية", 0xFF8C879B);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dp(48), 1);
        sp.setMargins(dp(3), 0, dp(3), 0);
        speeds.addView(fast, sp);
        speeds.addView(normal, sp);
        fast.setOnClickListener(v -> { highSpeed = true; fast.setBackground(roundBg(purple, dp(14))); normal.setBackground(roundBg(0xFF8C879B, dp(14))); Toast.makeText(this, "وضع السرعة العالية؛ السرعة الفعلية تعتمد على Wi-Fi والهاتفين.", Toast.LENGTH_LONG).show(); });
        normal.setOnClickListener(v -> { highSpeed = false; normal.setBackground(roundBg(purple, dp(14))); fast.setBackground(roundBg(0xFF8C879B, dp(14))); Toast.makeText(this, "تم اختيار السرعة العادية.", Toast.LENGTH_SHORT).show(); });

        Button bluetooth = makeButton("ᛒ   مشاركة التطبيق عبر البلوتوث", 0xFF3F8F7A);
        LinearLayout.LayoutParams btP = new LinearLayout.LayoutParams(-1, dp(52));
        btP.setMargins(dp(18), dp(4), dp(18), dp(8));
        root.addView(bluetooth, btP);
        bluetooth.setOnClickListener(v -> shareAppApk());

        LinearLayout qrCard = new LinearLayout(this);
        qrCard.setOrientation(LinearLayout.VERTICAL);
        qrCard.setGravity(Gravity.CENTER);
        qrCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        qrCard.setBackground(roundBg(Color.WHITE, dp(18)));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(dp(18), dp(4), dp(18), dp(8));
        root.addView(qrCard, cardParams);

        TextView qrTitle = new TextView(this);
        qrTitle.setText("رمز الاتصال والاستقبال");
        qrTitle.setTextSize(15);
        qrTitle.setTypeface(null, 1);
        qrTitle.setTextColor(ink);
        qrCard.addView(qrTitle);

        qrView = new ImageView(this);
        qrView.setAdjustViewBounds(true);
        qrView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrView.setVisibility(View.GONE);
        LinearLayout.LayoutParams qrp = new LinearLayout.LayoutParams(dp(220), dp(220));
        qrp.topMargin = dp(8);
        qrCard.addView(qrView, qrp);

        status = new TextView(this);
        status.setText("جاهز — اختر إرسالًا أو استقبالًا.");
        status.setTextSize(13);
        status.setTextColor(0xFF5E5870);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(6), dp(10), dp(6), dp(6));
        qrCard.addView(status, new LinearLayout.LayoutParams(-1, -2));

        Button stop = makeButton("إيقاف المشاركة", 0xFFB84040);
        LinearLayout.LayoutParams stopP = new LinearLayout.LayoutParams(-1, dp(48));
        stopP.setMargins(dp(18), dp(4), dp(18), 0);
        root.addView(stop, stopP);
        stop.setOnClickListener(v -> stopSharing());

        TextView signature = new TextView(this);
        signature.setText("تصميم خاص بأبو وسام");
        signature.setTextSize(11);
        signature.setTextColor(0xFF8C879B);
        signature.setGravity(Gravity.CENTER);
        signature.setPadding(0, dp(12), 0, 0);
        root.addView(signature, new LinearLayout.LayoutParams(-1, -2));

        setContentView(scroll);
    }

    private void scanQr() {
        com.google.zxing.integration.android.IntentIntegrator integrator = new com.google.zxing.integration.android.IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(com.google.zxing.integration.android.IntentIntegrator.QR_CODE);
        integrator.setPrompt("امسح رمز الاستقبال إذا لم يظهر الجهاز");
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    private void showDeviceDiscovery() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(18));
        page.setBackgroundColor(0xFFF6F3FC);
        Button back = makeButton("رجوع", 0xFF56339D);
        page.addView(back, new LinearLayout.LayoutParams(-1, dp(48)));
        TextView title = new TextView(this);
        title.setText("البحث عن أجهزة الاستقبال");
        title.setTextColor(0xFF24213A); title.setTextSize(22); title.setTypeface(null, 1); title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(18), 0, dp(8)); page.addView(title, new LinearLayout.LayoutParams(-1, -2));
        TextView hint = new TextView(this);
        hint.setText("افتح «استقبال» في الهاتف الآخر، واجعل الهاتفين على شبكة Wi-Fi نفسها.");
        hint.setTextColor(0xFF6B647C); hint.setTextSize(14); hint.setGravity(Gravity.CENTER);
        page.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        TextView scanning = new TextView(this);
        scanning.setText("◉  جارٍ البحث عن الأجهزة القريبة…"); scanning.setTextSize(16); scanning.setTextColor(0xFF6C42C1);
        scanning.setGravity(Gravity.CENTER); scanning.setPadding(0, dp(20), 0, dp(16));
        page.addView(scanning, new LinearLayout.LayoutParams(-1, -2));
        android.widget.ProgressBar spinner = new android.widget.ProgressBar(this);
        spinner.setIndeterminate(true); LinearLayout.LayoutParams spinP = new LinearLayout.LayoutParams(dp(44), dp(44)); spinP.gravity = Gravity.CENTER;
        page.addView(spinner, spinP);
        LinearLayout devices = new LinearLayout(this); devices.setOrientation(LinearLayout.VERTICAL);
        page.addView(devices, new LinearLayout.LayoutParams(-1, 0, 1));
        Button qr = makeButton("الاتصال عبر رمز QR بدل البحث", 0xFFFF8A3D);
        page.addView(qr, new LinearLayout.LayoutParams(-1, dp(52)));
        qr.setOnClickListener(v -> scanQr()); back.setOnClickListener(v -> buildUi()); setContentView(page);
        final java.util.Set<String> found = java.util.Collections.synchronizedSet(new java.util.HashSet<String>());
        final long started = System.currentTimeMillis();
        final Runnable updateHint = new Runnable() {
            @Override public void run() {
                if (isFinishing()) return;
                if (found.isEmpty() && System.currentTimeMillis() - started < 9000) {
                    scanning.setText("◉  جارٍ البحث عن أجهزة الاستقبال…"); uiHandler.postDelayed(this, 700);
                } else if (found.isEmpty()) {
                    spinner.setVisibility(View.GONE);
                    scanning.setText("لم نعثر على جهاز. افتح «استقبال» في الهاتف الآخر وتأكد من اتصال الهاتفين بالشبكة نفسها.");
                    scanning.setTextColor(0xFFB84040);
                }
            }
        };
        uiHandler.postDelayed(updateHint, 700);
        new Thread(() -> {
            long until = System.currentTimeMillis() + 9000;
            while (System.currentTimeMillis() < until) {
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setBroadcast(true); socket.setSoTimeout(650);
                    byte[] request = "ABUWESSAM_DISCOVER_V1".getBytes("UTF-8");
                    socket.send(new DatagramPacket(request, request.length, java.net.InetAddress.getByName("255.255.255.255"), 8766));
                    long waitUntil = System.currentTimeMillis() + 800;
                    while (System.currentTimeMillis() < waitUntil) {
                        try {
                            byte[] buf = new byte[1024]; DatagramPacket reply = new DatagramPacket(buf, buf.length); socket.receive(reply);
                            String response = new String(reply.getData(), 0, reply.getLength(), "UTF-8");
                            if (response.startsWith("ABUWESSAM_DEVICE_V1|")) {
                                String[] parts = response.split("\\|", 3);
                                if (parts.length == 3 && found.add(parts[2])) {
                                    final String deviceName = parts[1], url = parts[2];
                                    uiHandler.post(() -> {
                                        spinner.setVisibility(View.GONE); scanning.setText("تم العثور على جهاز استقبال"); scanning.setTextColor(0xFF23865A);
                                        Button device = makeButton("📱  " + deviceName + "\nاضغط للاتصال", 0xFF6C42C1);
                                        LinearLayout.LayoutParams rowP = new LinearLayout.LayoutParams(-1, dp(76)); rowP.bottomMargin = dp(8);
                                        devices.addView(device, rowP); device.setOnClickListener(v -> openScannedPage(url));
                                    });
                                }
                            }
                        } catch (java.net.SocketTimeoutException timeout) { break; }
                    }
                } catch (Exception ignored) { }
                try { Thread.sleep(350); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); break; }
            }
        }, "AbuWessamDeviceScan").start();
    }

    private void startDiscoveryResponder() {
        if (discoveryRunning) return;
        discoveryRunning = true;
        discoveryThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(null)) {
                socket.setReuseAddress(true); socket.bind(new InetSocketAddress(8766)); socket.setBroadcast(true); socket.setSoTimeout(1000);
                byte[] buffer = new byte[1024];
                while (discoveryRunning) {
                    try {
                        DatagramPacket request = new DatagramPacket(buffer, buffer.length); socket.receive(request);
                        String message = new String(request.getData(), request.getOffset(), request.getLength(), "UTF-8");
                        if ("ABUWESSAM_DISCOVER_V1".equals(message)) {
                            String ip = getLocalIp(); if (ip == null) ip = request.getAddress().getHostAddress();
                            byte[] reply = ("ABUWESSAM_DEVICE_V1|أبو وسام SHARE|http://" + ip + ":8765").getBytes("UTF-8");
                            socket.send(new DatagramPacket(reply, reply.length, request.getAddress(), request.getPort()));
                        }
                    } catch (Exception ignored) { }
                }
            } catch (Exception ignored) { }
        }, "AbuWessamDiscoveryResponder");
        discoveryThread.setDaemon(true); discoveryThread.start();
    }

    private void stopDiscoveryResponder() {
        discoveryRunning = false;
        if (discoveryThread != null) discoveryThread.interrupt();
        discoveryThread = null;
    }

    private void openScannedPage(String url) {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(0xFFF3F6FB);
        Button back = makeButton("رجوع إلى أبو وسام SHARE", 0xFF102A43);
        page.addView(back, match());
        WebView web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, android.webkit.ValueCallback<Uri[]> callback, android.webkit.WebChromeClient.FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("*/*");
                try { startActivityForResult(pick, 42); }
                catch (Exception e) { fileChooserCallback = null; callback.onReceiveValue(null); return false; }
                return true;
            }
        });
        web.setDownloadListener((downloadUrl, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
                request.setTitle("تنزيل ملف من أبو وسام SHARE");
                request.setDescription("جارٍ تنزيل الملف");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AbuWessam_" + System.currentTimeMillis());
                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                manager.enqueue(request);
                Toast.makeText(this, "بدأ تنزيل الملف", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "تعذّر تنزيل الملف: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        page.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        back.setOnClickListener(v -> buildUi());
        setContentView(page);
        web.loadUrl(url);
    }

    private void showQr(String text) {
        try {
            com.google.zxing.common.BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 600, 600);
            Bitmap bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565);
            for (int x = 0; x < 600; x++) {
                for (int y = 0; y < 600; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            qrView.setImageBitmap(bitmap);
            qrView.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            status.setText("تعذّر إنشاء رمز QR.");
        }
    }

    private GradientDrawable roundBg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private Button makeButton(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setBackground(roundBg(color, dp(14)));
        if (android.os.Build.VERSION.SDK_INT >= 21) b.setElevation(dp(2));
        return b;
    }

    private void shareAppApk() {
        try {
            File source = new File(getApplicationInfo().sourceDir);
            File copy = new File(getCacheDir(), "Abu-Wessam-SHARE.apk");
            try (InputStream in = new FileInputStream(source); FileOutputStream out = new FileOutputStream(copy)) {
                byte[] buffer = new byte[highSpeed ? 262144 : 32768]; int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", copy);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/vnd.android.package-archive");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "إرسال أبو وسام SHARE عبر البلوتوث أو تطبيق آخر"));
        } catch (Exception e) {
            Toast.makeText(this, "تعذّر تجهيز ملف التطبيق للمشاركة: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private LinearLayout.LayoutParams match() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(8);
        return p;
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private void chooseFiles() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, PICK_FILES);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        com.google.zxing.integration.android.IntentResult scanResult =
            com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            String scanned = scanResult.getContents();
            if (scanned != null && (scanned.startsWith("http://") || scanned.startsWith("https://"))) {
                try { openScannedPage(scanned); }
                catch (Exception e) { status.setText("تعذّر فتح صفحة المشاركة داخل التطبيق."); }
            } else if (scanned != null) {
                Toast.makeText(this, "رمز غير صالح. امسح رمز أبو وسام SHARE.", Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode == 42) {
            if (fileChooserCallback != null) {
                Uri[] result = (resultCode == RESULT_OK && data != null && data.getData() != null) ? new Uri[]{data.getData()} : null;
                fileChooserCallback.onReceiveValue(result);
                fileChooserCallback = null;
            }
            return;
        }
        if (requestCode != PICK_FILES || resultCode != RESULT_OK || data == null) return;
        if (data.getClipData() != null) {
            for (int n = 0; n < data.getClipData().getItemCount(); n++) addUri(data.getClipData().getItemAt(n).getUri());
        } else if (data.getData() != null) addUri(data.getData());
        refreshFiles();
    }

    private void addUri(Uri uri) {
        if (!selectedUris.contains(uri)) {
            selectedUris.add(uri);
            selectedNames.add(getName(uri));
        }
    }

    private String getName(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (col >= 0) return c.getString(col);
            }
        } catch (Exception ignored) { } finally { if (c != null) c.close(); }
        return "ملف";
    }

    private void refreshFiles() {
        StringBuilder b = new StringBuilder("الملفات المختارة: ").append(selectedUris.size());
        synchronized (selectedNames) {
            for (String name : selectedNames) b.append("\n• ").append(name);
        }
        filesView.setText(b.toString());
    }

    private void startSharing() {
        try {
            if (server == null) {
                server = new ShareServer();
                server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            }
            String ip = getLocalIp();
            if (ip == null) ip = "تعذّر تحديد IP؛ تأكد من اتصال Wi‑Fi";
            status.setText("جاهز للاستقبال — دع الهاتف الآخر يضغط إرسال ويمسح الرمز.\nhttp://" + ip + ":8765\nالوضع: " + (highSpeed ? "سرعة عالية" : "سرعة عادية"));
            showQr("http://" + ip + ":8765");
            startDiscoveryResponder();
            speedHandler.removeCallbacks(speedTicker);
            speedHandler.post(speedTicker);
            Toast.makeText(this, "بدأت المشاركة على المنفذ 8765", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            status.setText("تعذّر تشغيل المشاركة: " + e.getMessage());
            Toast.makeText(this, "تعذّر بدء المشاركة", Toast.LENGTH_LONG).show();
        }
    }

    private void stopSharing() {
        stopDiscoveryResponder();
        if (server != null) { server.stop(); server = null; }
        speedHandler.removeCallbacks(speedTicker);
        status.setText("الحالة: متوقف");
    }

    private String getLocalIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> addrs = intf.getInetAddresses(); addrs.hasMoreElements();) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0 && addr.isSiteLocalAddress()) return addr.getHostAddress();
                }
            }
        } catch (Exception ignored) { }
        try {
            WifiManager wm = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
            int ip = wm.getConnectionInfo().getIpAddress();
            return String.format(java.util.Locale.US, "%d.%d.%d.%d", ip & 255, (ip >> 8) & 255, (ip >> 16) & 255, (ip >> 24) & 255);
        } catch (Exception ignored) { return null; }
    }

    @Override protected void onDestroy() {
        stopSharing();
        super.onDestroy();
    }

    private class ShareServer extends NanoHTTPD {
        volatile long transferBytes = 0;
        volatile long transferTotal = 0;
        volatile double transferSpeed = 0;
        volatile String transferLabel = "بانتظار نقل الملفات";
        private long lastAt = 0;
        private long lastBytes = 0;
        ShareServer() { super("0.0.0.0", 8765); }

        private synchronized void updateProgress(long bytes, long total, String label) {
            long now = System.currentTimeMillis();
            transferSpeed = lastAt > 0 && now > lastAt && bytes >= lastBytes ? (bytes - lastBytes) * 1000.0 / (now - lastAt) : 0;
            transferBytes = bytes; transferTotal = total; transferLabel = label;
            lastAt = now; lastBytes = bytes;
        }

        @Override public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            if (Method.GET.equals(session.getMethod()) && "/status".equals(uri)) {
                return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", "{\"bytes\":" + transferBytes + ",\"total\":" + transferTotal + ",\"speed\":" + (long)transferSpeed + "}");
            }
            if (Method.GET.equals(session.getMethod()) && uri.startsWith("/progress")) {
                try {
                    Map<String,String> q = session.getParms();
                    transferBytes = Long.parseLong(q.getOrDefault("bytes", "0"));
                    transferTotal = Long.parseLong(q.getOrDefault("total", "0"));
                    transferSpeed = Double.parseDouble(q.getOrDefault("speed", "0"));
                    transferLabel = "استقبال ملف من الهاتف الآخر";
                } catch (Exception ignored) { }
                return newFixedLengthResponse("ok");
            }
            if (Method.GET.equals(session.getMethod()) && "/".equals(uri)) return htmlPage();
            if (Method.GET.equals(session.getMethod()) && uri.startsWith("/file/")) {
                try {
                    int index = Integer.parseInt(uri.substring("/file/".length()));
                    if (index < 0 || index >= selectedUris.size()) return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "الملف غير موجود");
                    Uri fileUri = selectedUris.get(index);
                    String name = selectedNames.get(index);
                    InputStream original = getContentResolver().openInputStream(fileUri);
                    if (original == null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "تعذّر فتح الملف");
                    long total = getFileSize(fileUri);
                    updateProgress(0, total, "إرسال: " + name);
                    InputStream stream = new java.io.FilterInputStream(original) {
                        long sent = 0;
                        private void tick(int n) { if (n > 0) { sent += n; updateProgress(sent, total, "إرسال: " + name); } }
                        @Override public int read() throws java.io.IOException { int b = super.read(); if (b >= 0) tick(1); return b; }
                        @Override public int read(byte[] b, int off, int len) throws java.io.IOException { int n = super.read(b, off, len); tick(n); return n; }
                    };
                    Response r = newChunkedResponse(Response.Status.OK, "application/octet-stream", stream);
                    r.addHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encode(name));
                    return r;
                } catch (Exception e) { return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "طلب غير صالح"); }
            }
            if (Method.POST.equals(session.getMethod()) && "/upload".equals(uri)) return receiveUpload(session);
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found");
        }

        private long getFileSize(Uri uri) {
            Cursor cur = null;
            try {
                cur = getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null);
                if (cur != null && cur.moveToFirst()) {
                    int col = cur.getColumnIndex(OpenableColumns.SIZE);
                    if (col >= 0 && !cur.isNull(col)) return cur.getLong(col);
                }
            } catch (Exception ignored) { } finally { if (cur != null) cur.close(); }
            return 0;
        }

        private Response htmlPage() {
            StringBuilder h = new StringBuilder("<!doctype html><html lang='ar' dir='rtl'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>أبو وسام SHARE</title><body style='font-family:sans-serif;max-width:760px;margin:24px auto;padding:0 16px;background:#f3f6fa;color:#102a43'><h1>أبو وسام SHARE</h1><section style='background:white;border-radius:16px;padding:14px;margin:12px 0'><b>سرعة النقل</b><p id='rate'>0 KB/s</p><div style='height:28px;background:#e5ddf6;border-radius:18px;overflow:hidden;border:1px solid #d0c2ed'><div id='bar' style='height:100%;width:0%;background:linear-gradient(90deg,#6c42c1,#ff8a3d);border-radius:18px;transition:width .18s'></div></div><p id='pct'>0%</p></section><h2>تنزيل الملفات</h2><ul>");
            synchronized (selectedNames) {
                for (int i=0; i<selectedNames.size(); i++) h.append("<li style='margin:12px 0'><a href='/file/").append(i).append("'>").append(escape(selectedNames.get(i))).append("</a></li>");
            }
            if (selectedNames.isEmpty()) h.append("<li>لم يحدد المضيف ملفات للإرسال.</li>");
            h.append("</ul><hr><h2>إرسال ملف إلى هذا الهاتف</h2><label for='file' style='display:block;background:#f1ebfc;border:2px dashed #8b6ad0;border-radius:18px;padding:26px 14px;text-align:center;color:#56339d;font-weight:bold;cursor:pointer'>＋<br>اضغط هنا لاختيار ملف من هاتفك<br><small style='font-weight:normal;color:#6b647c'>صور، فيديو، مستندات وملفات</small></label><input id='file' type='file' required style='display:none' onchange="document.getElementById('chosenFile').textContent=this.files.length?this.files[0].name:'لم يتم اختيار ملف'"><p id='chosenFile' style='color:#6b647c'>لم يتم اختيار ملف</p><p><button style='width:100%;border:0;border-radius:14px;background:#6c42c1;color:white;padding:15px;font-size:17px;font-weight:bold' onclick='sendFile()'>إرسال الملف</button></p><p id='msg'>اختر ملفًا لبدء الإرسال.</p><script>function human(n){if(n>=1048576)return (n/1048576).toFixed(2)+' MB/s';if(n>=1024)return (n/1024).toFixed(1)+' KB/s';return Math.round(n)+' B/s'}function sendFile(){let f=document.getElementById('file').files[0];if(!f)return;let x=new XMLHttpRequest(),t=Date.now();x.open('POST','/upload');x.upload.onprogress=e=>{if(e.lengthComputable){let s=e.loaded/Math.max(.001,(Date.now()-t)/1000);document.getElementById('rate').textContent=human(s);document.getElementById('bar').style.width=(e.loaded/e.total*100)+'%';document.getElementById('pct').textContent=Math.floor(e.loaded/e.total*100)+'%';document.getElementById('msg').textContent='جارٍ الإرسال: '+human(s);fetch('/progress?bytes='+e.loaded+'&total='+e.total+'&speed='+s).catch(()=>{});}};x.onload=()=>document.getElementById('msg').textContent=x.status===200?'اكتمل الإرسال':'تعذّر الإرسال';x.onerror=()=>document.getElementById('msg').textContent='انقطع الاتصال';let d=new FormData();d.append('file',f,f.name);x.send(d)}function poll(){fetch('/status').then(r=>r.json()).then(s=>{if(s.total>0){document.getElementById('rate').textContent=human(s.speed);document.getElementById('bar').style.width=Math.min(100,s.bytes*100/s.total)+'%';document.getElementById('pct').textContent=Math.floor(Math.min(100,s.bytes*100/s.total))+'%';}}).catch(()=>{});setTimeout(poll,500)}poll();</script><p>اترك تطبيق أبو وسام SHARE مفتوحًا على الهاتف المضيف أثناء النقل.</p></body></html>");
            return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", h.toString());
        }

        private Response receiveUpload(IHTTPSession session) {
            try {
                Map<String,String> files = new HashMap<String,String>();
                session.parseBody(files);
                String tempPath = files.get("file");
                if (tempPath == null) return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "لم يتم اختيار ملف");
                String original = session.getParms().get("file");
                if (original == null || original.trim().isEmpty()) original = "received-file";
                original = new File(original).getName().replaceAll("[^\\p{L}\\p{N}._ -]", "_");
                if (original.length() > 100) original = original.substring(original.length()-100);
                File dir = getExternalFilesDir(null);
                if (dir == null) dir = getFilesDir();
                File dest = new File(dir, System.currentTimeMillis() + "_" + original);
                long total = new File(tempPath).length();
                updateProgress(0, total, "استقبال: " + original);
                try (InputStream in = new FileInputStream(tempPath); FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buffer = new byte[highSpeed ? 262144 : 32768]; int count; long copied = 0;
                    while ((count = in.read(buffer)) != -1) { out.write(buffer, 0, count); copied += count; updateProgress(copied, total, "حفظ: " + original); }
                }
                String page = "<html lang='ar' dir='rtl'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><body style='font-family:sans-serif;padding:24px'><h2>تم استلام الملف</h2><p>" + escape(original) + "</p><p>حُفظ داخل مجلد التطبيق. يمكنك العودة للصفحة الرئيسية لإرسال ملفات أخرى.</p><a href='/'>عودة</a></body></html>";
                return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", page);
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "فشل استقبال الملف: " + e.getMessage());
            }
        }

        private String encode(String s) {
            try { return URLEncoder.encode(s, "UTF-8").replace("+", "%20"); } catch (Exception e) { return "file"; }
        }
        private String escape(String s) {
            if (s == null) return "";
            return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
        }
    }
}
