package com.abuwessam.nfccontact;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ClipData;
import android.content.ClipboardManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

public class MainActivity extends Activity {
    private final String name = "أحمد رشيد - أبو وسام";
    private final String phone = "+963938342496";
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(10, 37, 64));
        getWindow().setNavigationBarColor(Color.rgb(10, 37, 64));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(18), dp(22), dp(22));
        root.setBackgroundColor(Color.rgb(242,247,251));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);

        TextView logo = new TextView(this);
        logo.setText("◉  NFC");
        logo.setTextSize(28); logo.setTextColor(Color.WHITE); logo.setGravity(Gravity.CENTER);
        logo.setBackgroundColor(Color.rgb(10, 37, 64));
        root.addView(logo, new LinearLayout.LayoutParams(-1, dp(78)));

        TextView title = new TextView(this);
        title.setText("رقمي عبر NFC");
        title.setTextSize(27); title.setTextColor(Color.rgb(10,37,64)); title.setGravity(Gravity.CENTER);
        title.setPadding(0,dp(22),0,dp(4)); root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("أبو وسام  •  بطاقة اتصال رقمية");
        sub.setTextSize(16); sub.setTextColor(Color.DKGRAY); sub.setGravity(Gravity.CENTER);
        root.addView(sub);

        TextView contact = new TextView(this);
        contact.setText(name + "\n" + phone);
        contact.setTextSize(20); contact.setTextColor(Color.rgb(10,37,64));
        contact.setGravity(Gravity.CENTER); contact.setPadding(dp(8),dp(20),dp(8),dp(16));
        root.addView(contact);

        try {
            BitMatrix matrix = new MultiFormatWriter().encode("BEGIN:VCARD\nVERSION:3.0\nFN:" + name + "\nTEL;TYPE=CELL:" + phone + "\nEND:VCARD", BarcodeFormat.QR_CODE, dp(240), dp(240));
            Bitmap bmp = Bitmap.createBitmap(matrix.getWidth(), matrix.getHeight(), Bitmap.Config.RGB_565);
            for (int x=0;x<matrix.getWidth();x++) for (int y=0;y<matrix.getHeight();y++) bmp.setPixel(x,y,matrix.get(x,y)?Color.BLACK:Color.WHITE);
            android.widget.ImageView qr = new android.widget.ImageView(this);
            qr.setImageBitmap(bmp); qr.setAdjustViewBounds(true);
            LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(dp(240),dp(240)); qp.gravity=Gravity.CENTER;
            root.addView(qr, qp);
        } catch (Exception e) { }

        TextView help = new TextView(this);
        help.setText("لإرسال بياناتك لهاتف آخر: اعرض رمز QR ودعه يمسحه بكاميرا الهاتف. لا يحتاج إلى تثبيت هذا التطبيق. يمكن أيضًا مشاركة بطاقة الاتصال مباشرة.");
        help.setTextSize(14); help.setTextColor(Color.DKGRAY); help.setGravity(Gravity.CENTER);
        help.setPadding(0,dp(14),0,dp(14)); root.addView(help);

        Button copy = button("نسخ رقم الهاتف");
        copy.setOnClickListener(v -> {
            ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("رقم أبو وسام", phone));
            Toast.makeText(this,"تم نسخ الرقم",Toast.LENGTH_SHORT).show();
        }); root.addView(copy, lp());

        Button share = button("مشاركة بطاقة الاتصال");
        share.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_INSERT);
            i.setType(ContactsContract.Contacts.CONTENT_TYPE);
            i.putExtra(ContactsContract.Intents.Insert.NAME, name);
            i.putExtra(ContactsContract.Intents.Insert.PHONE, phone);
            try { startActivity(i); } catch(Exception e) { Toast.makeText(this,"تعذر فتح جهات الاتصال",Toast.LENGTH_SHORT).show(); }
        }); root.addView(share, lp());

        Button sms = button("إرسال الرقم برسالة");
        sms.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
            i.putExtra("sms_body", name + "\n" + phone);
            try { startActivity(i); } catch(Exception e) { }
        }); root.addView(sms, lp());

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        TextView nfc = new TextView(this);
        nfc.setText(adapter == null ? "هاتفك لا يحتوي على NFC. استخدم رمز QR أو المشاركة." :
            (adapter.isEnabled() ? "ميزة NFC مفعّلة على هاتفك، لكن نقل البيانات بمجرد ملامسة أي هاتف غير مضمون؛ استخدم QR لضمان التوافق." : "يمكن تفعيل NFC من إعدادات الهاتف، لكن الهاتف الآخر قد لا يستقبل البيانات بمجرد الملامسة."));
        nfc.setTextSize(13); nfc.setTextColor(Color.GRAY); nfc.setGravity(Gravity.CENTER); nfc.setPadding(0,dp(18),0,dp(4));
        root.addView(nfc);
        setContentView(scroll);
    }

    private Button button(String text) {
        Button b = new Button(this); b.setText(text); b.setTextSize(15);
        b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(17,91,139)));
        return b;
    }
    private LinearLayout.LayoutParams lp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54)); p.setMargins(0,dp(5),0,dp(5)); return p;
    }
}
