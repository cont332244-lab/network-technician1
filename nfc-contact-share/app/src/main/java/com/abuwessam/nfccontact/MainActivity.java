package com.abuwessam.nfccontact;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

public class MainActivity extends Activity {
    private static final String PREFS = "contact_settings";
    private static final String KEY_NAME = "owner_name";
    private static final String KEY_PHONE = "owner_phone";
    private String ownerName = "أحمد رشيد - أبو وسام";
    private String phone = "+963938342496";
    private LinearLayout root;
    private TextView contact;
    private ImageView qr;

    private int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            ownerName = prefs.getString(KEY_NAME, ownerName);
            phone = prefs.getString(KEY_PHONE, phone);

            getWindow().setStatusBarColor(Color.rgb(10, 37, 64));
            getWindow().setNavigationBarColor(Color.rgb(10, 37, 64));
            root = new LinearLayout(this);
            root.setOrientation(1);
            root.setGravity(Gravity.CENTER_HORIZONTAL);
            root.setPadding(dp(20), dp(16), dp(20), dp(22));
            root.setBackgroundColor(Color.rgb(242, 247, 251));

            ScrollView sc = new ScrollView(this);
            sc.addView(root);

            TextView logo = new TextView(this);
            logo.setText("◉ NFC");
            logo.setTextSize(27);
            logo.setTextColor(Color.WHITE);
            logo.setGravity(Gravity.CENTER);
            logo.setBackgroundColor(Color.rgb(10, 37, 64));
            root.addView(logo, new LinearLayout.LayoutParams(-1, dp(72)));

            TextView title = new TextView(this);
            title.setText("رقمي عبر NFC");
            title.setTextSize(27);
            title.setTextColor(Color.rgb(10, 37, 64));
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, dp(18), 0, dp(4));
            root.addView(title);

            TextView sub = new TextView(this);
            sub.setText("أبو وسام • بطاقة اتصال رقمية");
            sub.setTextSize(16);
            sub.setGravity(Gravity.CENTER);
            root.addView(sub);

            contact = new TextView(this);
            contact.setTextSize(19);
            contact.setTextColor(Color.rgb(10, 37, 64));
            contact.setGravity(Gravity.CENTER);
            contact.setPadding(dp(8), dp(18), dp(8), dp(12));
            root.addView(contact);

            qr = new ImageView(this);
            qr.setAdjustViewBounds(true);
            LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(dp(230), dp(230));
            qp.gravity = Gravity.CENTER;
            root.addView(qr, qp);

            TextView help = new TextView(this);
            help.setText("اعرض رمز QR ليقوم الطرف الآخر بمسحه بكاميرا هاتفه. لا يحتاج إلى تثبيت التطبيق. يمكنك تغيير الاسم والرقم من زر تعديل بياناتي.");
            help.setTextSize(14);
            help.setTextColor(Color.DKGRAY);
            help.setGravity(Gravity.CENTER);
            help.setPadding(0, dp(12), 0, dp(12));
            root.addView(help);

            addButton("تعديل الاسم والرقم", v -> editContact());
            addButton("نسخ رقم الهاتف", v -> {
                try {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("رقم الهاتف", phone));
                    Toast.makeText(this, "تم نسخ الرقم", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    showError("تعذر نسخ الرقم");
                }
            });
            addButton("مشاركة بطاقة الاتصال", v -> shareVcard());
            addButton("إرسال الرقم برسالة", v -> sendSms());

            TextView note = new TextView(this);
            note.setText("ملاحظة: NFC لا يضمن نقل البيانات تلقائيًا إلى كل الهواتف. رمز QR هو البديل الأكثر توافقًا.");
            note.setTextSize(13);
            note.setTextColor(Color.GRAY);
            note.setGravity(Gravity.CENTER);
            note.setPadding(0, dp(16), 0, 0);
            root.addView(note);

            setContentView(sc);
            refreshCard();
        } catch (Exception e) {
            showError("تعذر تشغيل التطبيق. أعد فتحه، وإذا استمر التوقف فأرسل رسالة الخطأ.");
        }
    }

    private void refreshCard() {
        contact.setText(ownerName + "\n" + phone);
        try {
            String v = "BEGIN:VCARD\nVERSION:3.0\nFN:" + ownerName
                    + "\nTEL;TYPE=CELL:" + phone + "\nEND:VCARD";
            int size = dp(230);
            BitMatrix m = new MultiFormatWriter().encode(v, BarcodeFormat.QR_CODE, size, size);
            Bitmap bm = Bitmap.createBitmap(m.getWidth(), m.getHeight(), Bitmap.Config.RGB_565);
            for (int x = 0; x < m.getWidth(); x++) {
                for (int y = 0; y < m.getHeight(); y++) {
                    bm.setPixel(x, y, m.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            qr.setImageBitmap(bm);
        } catch (Exception e) {
            qr.setImageDrawable(null);
            Toast.makeText(this, "تعذر إنشاء رمز QR", Toast.LENGTH_LONG).show();
        }
    }

    private void editContact() {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(1);
        int pad = dp(12);
        f.setPadding(pad, 0, pad, 0);
        EditText n = new EditText(this);
        n.setSingleLine(true);
        n.setHint("الاسم");
        n.setText(ownerName);
        f.addView(n);
        EditText p = new EditText(this);
        p.setSingleLine(true);
        p.setHint("رقم الهاتف مع مفتاح الدولة");
        p.setInputType(InputType.TYPE_CLASS_PHONE);
        p.setText(phone);
        f.addView(p);

        new android.app.AlertDialog.Builder(this)
                .setTitle("تعديل بيانات الاتصال")
                .setView(f)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حفظ", (d, w) -> {
                    String nn = n.getText().toString().trim();
                    String pp = p.getText().toString().trim();
                    if (nn.isEmpty() || pp.isEmpty()) {
                        Toast.makeText(this, "أدخل الاسم والرقم", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ownerName = nn;
                    phone = pp;
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(KEY_NAME, ownerName)
                            .putString(KEY_PHONE, phone)
                            .apply();
                    refreshCard();
                    Toast.makeText(this, "تم حفظ البيانات وتحديث رمز QR", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void shareVcard() {
        try {
            String v = "BEGIN:VCARD\nVERSION:3.0\nFN:" + ownerName
                    + "\nTEL;TYPE=CELL:" + phone + "\nEND:VCARD";
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, v);
            i.putExtra(Intent.EXTRA_SUBJECT, "بطاقة اتصال " + ownerName);
            startActivity(Intent.createChooser(i, "مشاركة بطاقة الاتصال"));
        } catch (Exception e) {
            showError("لا يوجد تطبيق متاح للمشاركة");
        }
    }

    private void sendSms() {
        try {
            Intent i = new Intent(Intent.ACTION_SENDTO);
            i.setData(Uri.parse("smsto:"));
            i.putExtra("sms_body", ownerName + "\n" + phone);
            startActivity(i);
        } catch (Exception e) {
            showError("لا يوجد تطبيق رسائل متاح على الهاتف");
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void addButton(String t, android.view.View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(t);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(17, 91, 139)));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.setMargins(0, dp(4), 0, dp(4));
        root.addView(b, p);
    }
}