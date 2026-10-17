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
import android.graphics.Color;\nimport android.graphics.drawable.GradientDrawable;\nimport androidx.core.content.FileProvider;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import fi.iki.elonen.NanoHTTPD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
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

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF102A43);
        getWindow().setNavigationBarColor(0xFF102A43);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFFF3F6FB);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(18), dp(22), dp(18), dp(22));
        hero.setBackground(roundBg(0xFF102A43, dp(24)));
        root.addView(hero, match());

        TextView logo = new TextView(this);
        logo.setText("AW");
        logo.setTextSize(23);
        logo.setTypeface(null, 1);
        logo.setTextColor(0xFF102A43);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(roundBg(0xFFFFFFFF, dp(40)));
        hero.addView(logo, new LinearLayout.LayoutParams(dp(66), dp(66)));

        TextView brand = new TextView(this);
        brand.setText("أبو وسام SHARE");
        brand.setTextSize(27);
        brand.setTextColor(Color.WHITE);
        brand.setGravity(Gravity.CENTER);
        brand.setTypeface(null, 1);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(10);
        hero.addView(brand, bp);

        TextView intro = new TextView(this);
        intro.setText("مشاركة ملفاتك بسرعة وبسهولة");
        intro.setTextSize(15);
        intro.setTextColor(0xFFD9E7F5);
        intro.setGravity(Gravity.CENTER);
        intro.setPadding(0, dp(6), 0, 0);
        hero.addView(intro);

        TextView section = new TextView(this);
        section.setText("اختر طريقة المشاركة");
        section.setTextSize(17);
        section.setTypeface(null, 1);
        section.setTextColor(0xFF102A43);
        section.setPadding(dp(4), dp(18), dp(4), dp(8));
        root.addView(section);

        Button send = makeButton("📁  اختيار ملفات للإرسال", 0xFF1769AA);
        root.addView(send, match());
        send.setOnClickListener(v -> chooseFiles());

        Button start = makeButton("▦  عرض رمز QR للاستقبال", 0xFF087F8C);
        root.addView(start, match());
        start.setOnClickListener(v -> startSharing());

        Button receive = makeButton("📷  استقبال — مسح QR", 0xFF4054B2);
        root.addView(receive, match());
        receive.setOnClickListener(v -> scanQr());

        Button bluetooth = makeButton("ᛒ  مشاركة التطبيق عبر البلوتوث", 0xFF6842A6);
        root.addView(bluetooth, match());
        bluetooth.setOnClickListener(v -> shareAppApk());

        LinearLayout qrCard = new LinearLayout(this);
        qrCard.setOrientation(LinearLayout.VERTICAL);
        qrCard.setGravity(Gravity.CENTER);
        qrCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        qrCard.setBackground(roundBg(Color.WHITE, dp(18)));
        LinearLayout.LayoutParams cardParams = match();
        cardParams.topMargin = dp(8);
        root.addView(qrCard, cardParams);

        TextView qrTitle = new TextView(this);
        qrTitle.setText("رمز الاتصال");
        qrTitle.setTextSize(16);
        qrTitle.setTypeface(null, 1);
        qrTitle.setTextColor(0xFF102A43);
        qrCard.addView(qrTitle);

        qrView = new ImageView(this);
        qrView.setAdjustViewBounds(true);
        qrView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrView.setVisibility(View.GONE);
        LinearLayout.LayoutParams qrp = new LinearLayout.LayoutParams(dp(240), dp(240));
        qrp.topMargin = dp(6);
        qrCard.addView(qrView, qrp);

        status = new TextView(this);
        status.setText("جاهز — اختر إرسالًا أو استقبالًا.");
        status.setTextSize(14);
        status.setTextColor(0xFF334E68);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(6), dp(12), dp(6), dp(12));
        qrCard.addView(status, new LinearLayout.LayoutParams(-1, -2));

        filesView = new TextView(this);
        filesView.setText("لم تختر ملفات بعد.");
        filesView.setTextSize(14);
        filesView.setTextColor(0xFF334E68);
        filesView.setPadding(dp(8), dp(8), dp(8), dp(8));
        root.addView(filesView, new LinearLayout.LayoutParams(-1, -2));

        Button stop = makeButton("إيقاف المشاركة", 0xFF9B2C2C);
        root.addView(stop, match());
        stop.setOnClickListener(v -> stopSharing());
        setContentView(scroll);
    }

    private void scanQr() {
        com.google.zxing.integration.android.IntentIntegrator integrator =
            new com.google.zxing.integration.android.IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(com.google.zxing.integration.android.IntentIntegrator.QR_CODE);
        integrator.setPrompt("صوّر رمز QR الموجود على هاتف المُرسِل");
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
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

    private GradientDrawable roundBg(int color, int radius) {\n        GradientDrawable d = new GradientDrawable();\n        d.setColor(color);\n        d.setCornerRadius(radius);\n        return d;\n    }\n\n    private Button makeButton(String label, int color) {\n        Button b = new Button(this);\n        b.setText(label);\n        b.setTextColor(Color.WHITE);\n        b.setTextSize(15);\n        b.setAllCaps(false);\n        b.setBackground(roundBg(color, dp(14)));\n        if (android.os.Build.VERSION.SDK_INT >= 21) b.setElevation(dp(2));\n        return b;\n    }\n\n    private void shareAppApk() {\n        try {\n            File source = new File(getApplicationInfo().sourceDir);\n            File copy = new File(getCacheDir(), "Abu-Wessam-SHARE.apk");\n            try (InputStream in = new FileInputStream(source); FileOutputStream out = new FileOutputStream(copy)) {\n                byte[] buffer = new byte[32768]; int count;\n                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);\n            }\n            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", copy);\n            Intent share = new Intent(Intent.ACTION_SEND);\n            share.setType("application/vnd.android.package-archive");\n            share.putExtra(Intent.EXTRA_STREAM, uri);\n            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);\n            startActivity(Intent.createChooser(share, "إرسال أبو وسام SHARE عبر البلوتوث أو تطبيق آخر"));\n        } catch (Exception e) {\n            Toast.makeText(this, "تعذّر تجهيز ملف التطبيق للمشاركة: " + e.getMessage(), Toast.LENGTH_LONG).show();\n        }\n    }\n\n    private LinearLayout.LayoutParams match() {
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
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(scanned))); }
                catch (Exception e) { status.setText("تعذّر فتح رابط المشاركة."); }
            } else if (scanned != null) {
                Toast.makeText(this, "رمز غير صالح. امسح رمز أبو وسام SHARE.", Toast.LENGTH_LONG).show();
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
            status.setText("جاهز للإرسال! امسح الرمز من الهاتف الآخر.\nhttp://" + ip + ":8765");
            showQr("http://" + ip + ":8765");
            Toast.makeText(this, "بدأت المشاركة على المنفذ 8765", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            status.setText("تعذّر تشغيل المشاركة: " + e.getMessage());
            Toast.makeText(this, "تعذّر بدء المشاركة", Toast.LENGTH_LONG).show();
        }
    }

    private void stopSharing() {
        if (server != null) { server.stop(); server = null; }
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
        ShareServer() { super("0.0.0.0", 8765); }

        @Override public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            if (Method.GET.equals(session.getMethod()) && "/".equals(uri)) return htmlPage();
            if (Method.GET.equals(session.getMethod()) && uri.startsWith("/file/")) {
                try {
                    int index = Integer.parseInt(uri.substring("/file/".length()));
                    if (index < 0 || index >= selectedUris.size()) return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "الملف غير موجود");
                    Uri fileUri = selectedUris.get(index);
                    String name = selectedNames.get(index);
                    InputStream stream = getContentResolver().openInputStream(fileUri);
                    if (stream == null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "تعذّر فتح الملف");
                    Response r = newChunkedResponse(Response.Status.OK, "application/octet-stream", stream);
                    r.addHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encode(name));
                    return r;
                } catch (Exception e) { return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "طلب غير صالح"); }
            }
            if (Method.POST.equals(session.getMethod()) && "/upload".equals(uri)) return receiveUpload(session);
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found");
        }

        private Response htmlPage() {
            StringBuilder h = new StringBuilder("<!doctype html><html lang='ar' dir='rtl'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>أبو وسام SHARE</title><body style='font-family:sans-serif;max-width:760px;margin:24px auto;padding:0 16px;background:#f3f6fa;color:#102a43'><h1>أبو وسام SHARE</h1><h2>تنزيل الملفات</h2><ul>");
            synchronized (selectedNames) {
                for (int i=0; i<selectedNames.size(); i++) h.append("<li style='margin:12px 0'><a href='/file/").append(i).append("'>").append(escape(selectedNames.get(i))).append("</a></li>");
            }
            if (selectedNames.isEmpty()) h.append("<li>لم يحدد المضيف ملفات للإرسال.</li>");
            h.append("</ul><hr><h2>إرسال ملف إلى هذا الهاتف</h2><form action='/upload' method='post' enctype='multipart/form-data'><input type='file' name='file' required><p><button type='submit'>رفع الملف</button></p></form><p>اترك تطبيق أبو وسام SHARE مفتوحًا على الهاتف المضيف أثناء النقل.</p></body></html>");
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
                try (InputStream in = new FileInputStream(tempPath); FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buffer = new byte[32768]; int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
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
