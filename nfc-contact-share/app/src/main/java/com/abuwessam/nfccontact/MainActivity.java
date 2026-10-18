package com.abuwessam.nfccontact;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
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

    private int dp(float n) { return (int) (n * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            ownerName = prefs.getString(KEY_NAME, ownerName);
            phone = prefs.getString(KEY_PHONE, phone);
            getWindow().setStatusBarColor(Color.rgb(10,37,64));
            getWindow().setNavigationBarColor(Color.rgb(10,37,64));
            root = new LinearLayout(this); root.setOrientation(1); root.setGravity(Gravity.CENTER_HORIZONTAL);
            root.setPadding(dp(20),dp(16),dp(20),dp(22)); root.setBackgroundColor(Color.rgb(242,247,251));
            ScrollView sc = new ScrollView(this); sc.addView(root);
            TextView logo = new TextView(this); logo.setText("◉ NFC"); logo.setTextSize(27); logo.setTextColor(Color.WHITE);
            logo.setGravity(Gravity.CENTER); logo.setBackgroundColor(Color.rgb(10,37,64)); root.addView(logo,new LinearLayout.LayoutParams(-1,dp(72)));
            TextView title = new TextView(this); title.setText("رقمي عبر NFC"); title.setTextSize(27); title.setTextColor(Color.rgb(10,37,64));
            title.setGravity(Gravity.CENTER); title.setPadding(0,dp(18),0,dp(4)); root.addView(title);
            TextView sub = new TextView(this); sub.setText("أبو وسام • بطاقة اتصال رقمية"); sub.setTextSize(16); sub.setGravity(Gravity.CENTER); root.addView(sub);
            contact = new TextView(this); contact.setTextSize(19); contact.setTextColor(Color.rgb(10,37,64)); contact.setGravity(Gravity.CENTER);
            contact.setPadding(dp(8),dp(18),dp(8),dp(12)); root.addView(contact);
            qr = new ImageView(this); qr.setAdjustViewBounds(true);
            LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(dp(230),dp(230)); qp.gravity=Gravity.CENTER; root.addView(qr,qp);
            TextView help = new TextView(this); help.setText("شارك ملف جهة اتصال VCF ليتمكن الطرف الآخر من فتحه وحفظ الاسم والرقم في جهات الاتصال. لا يحتاج الطرف الآخر إلى تثبيت هذا التطبيق.");
            help.setTextSize(14); help.setTextColor(Color.DKGRAY); help.setGravity(Gravity.CENTER); help.setPadding(0,dp(12),0,dp(12)); root.addView(help);
            addButton("تعديل الاسم والرقم",v->editContact());
            addButton("مشاركة جهة الاتصال (VCF)",v->shareVcard());
            addButton("إرسال جهة الاتصال برسالة",v->sendSms());
            addButton("نسخ رقم الهاتف",v->{try{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("رقم الهاتف",phone));Toast.makeText(this,"تم نسخ الرقم",Toast.LENGTH_SHORT).show();}catch(Exception e){showError("تعذر نسخ الرقم");}});
            TextView note = new TextView(this); note.setText("ملاحظة: ملف VCF هو بطاقة اتصال قابلة للاستيراد. أما NFC فلا يضمن الإرسال التلقائي إلى كل الهواتف."); note.setTextSize(13);
            note.setTextColor(Color.GRAY); note.setGravity(Gravity.CENTER); note.setPadding(0,dp(16),0,0); root.addView(note);
            setContentView(sc); refreshCard();
        } catch(Exception e) { showError("تعذر تشغيل التطبيق. أعد فتحه، وإذا استمر التوقف فأرسل رسالة الخطأ."); }
    }

    private String makeVcard() {
        String safeName = ownerName.replace("\\","\\\\").replace(";","\\;").replace(",","\\,").replace("\n"," ");
        return "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:" + safeName + "\r\nTEL;TYPE=CELL:" + phone + "\r\nEND:VCARD\r\n";
    }

    private void refreshCard() {
        contact.setText(ownerName+"\n"+phone);
        try {
            int size=dp(230); BitMatrix m=new MultiFormatWriter().encode(makeVcard(),BarcodeFormat.QR_CODE,size,size);
            Bitmap bm=Bitmap.createBitmap(m.getWidth(),m.getHeight(),Bitmap.Config.RGB_565);
            for(int x=0;x<m.getWidth();x++) for(int y=0;y<m.getHeight();y++) bm.setPixel(x,y,m.get(x,y)?Color.BLACK:Color.WHITE);
            qr.setImageBitmap(bm);
        } catch(Exception e) { qr.setImageDrawable(null); Toast.makeText(this,"تعذر إنشاء رمز QR",Toast.LENGTH_LONG).show(); }
    }

    private void editContact() {
        LinearLayout f=new LinearLayout(this); f.setOrientation(1); int pad=dp(12); f.setPadding(pad,0,pad,0);
        EditText n=new EditText(this); n.setSingleLine(true); n.setHint("الاسم"); n.setText(ownerName); f.addView(n);
        EditText p=new EditText(this); p.setSingleLine(true); p.setHint("رقم الهاتف مع مفتاح الدولة"); p.setInputType(InputType.TYPE_CLASS_PHONE); p.setText(phone); f.addView(p);
        new android.app.AlertDialog.Builder(this).setTitle("تعديل بيانات الاتصال").setView(f).setNegativeButton("إلغاء",null)
          .setPositiveButton("حفظ",(d,w)->{String nn=n.getText().toString().trim(),pp=p.getText().toString().trim();
            if(nn.isEmpty()||pp.isEmpty()){Toast.makeText(this,"أدخل الاسم والرقم",Toast.LENGTH_SHORT).show();return;}
            ownerName=nn;phone=pp;getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY_NAME,ownerName).putString(KEY_PHONE,phone).apply();
            refreshCard();Toast.makeText(this,"تم حفظ البيانات وتحديث رمز QR",Toast.LENGTH_SHORT).show();}).show();
    }

    private void shareVcard() {
        try {
            File dir = new File(getCacheDir(),"contacts");
            if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("Cannot create cache directory");
            File file = new File(dir,"abu-wessam-contact.vcf");
            try (FileOutputStream out = new FileOutputStream(file,false)) {
                out.write(makeVcard().getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            Uri uri = FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/vcard");
            send.putExtra(Intent.EXTRA_STREAM,uri);
            send.putExtra(Intent.EXTRA_SUBJECT,"جهة اتصال: "+ownerName);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send,"إرسال جهة الاتصال كملف VCF"));
        } catch(Exception e) { showError("تعذر تجهيز ملف VCF للمشاركة"); }
    }

    private void sendSms() {
        try {
            File dir = new File(getCacheDir(),"contacts");
            if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("Cannot create cache directory");
            File file = new File(dir,"abu-wessam-contact.vcf");
            try (FileOutputStream out = new FileOutputStream(file,false)) { out.write(makeVcard().getBytes(StandardCharsets.UTF_8)); }
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
            Intent send=new Intent(Intent.ACTION_SEND); send.setType("text/vcard"); send.putExtra(Intent.EXTRA_STREAM,uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send,"اختر تطبيق إرسال ملف جهة الاتصال"));
        } catch(Exception e) { showError("تعذر إرسال ملف VCF. استخدم زر مشاركة جهة الاتصال."); }
    }

    private void showError(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
    private void addButton(String t,android.view.View.OnClickListener l) {
        Button b=new Button(this); b.setText(t); b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(17,91,139))); b.setOnClickListener(l);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52)); p.setMargins(0,dp(4),0,dp(4)); root.addView(b,p);
    }
}