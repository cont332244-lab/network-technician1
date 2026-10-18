package com.abuwessam.nfccontact;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.graphics.drawable.GradientDrawable;
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

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            ownerName = prefs.getString(KEY_NAME, ownerName);
            phone = prefs.getString(KEY_PHONE, phone);
            getWindow().setStatusBarColor(Color.rgb(13, 34, 66));
            getWindow().setNavigationBarColor(Color.rgb(13, 34, 66));

            root = new LinearLayout(this);
            root.setOrientation(1); root.setGravity(Gravity.CENTER_HORIZONTAL);
            root.setPadding(dp(18),dp(18),dp(18),dp(24));
            root.setBackgroundColor(Color.rgb(244,247,252));
            ScrollView sc = new ScrollView(this); sc.setFillViewport(true); sc.addView(root);

            LinearLayout hero = new LinearLayout(this);
            hero.setOrientation(1); hero.setGravity(Gravity.CENTER);
            hero.setPadding(dp(14),dp(18),dp(14),dp(18));
            hero.setBackground(shape(Color.rgb(13,34,66),24));
            root.addView(hero,new LinearLayout.LayoutParams(-1,dp(145)));

            TextView logo = new TextView(this);
            logo.setText("✦  NFC  ✦"); logo.setTextSize(22); logo.setTextColor(Color.rgb(94,234,212));
            logo.setGravity(Gravity.CENTER); hero.addView(logo);
            TextView title = new TextView(this);
            title.setText("رقمي عبر NFC"); title.setTextSize(25); title.setTypeface(null,1);
            title.setTextColor(Color.WHITE); title.setGravity(Gravity.CENTER);
            title.setPadding(0,dp(8),0,dp(4)); hero.addView(title);
            TextView subtitle = new TextView(this);
            subtitle.setText("بطاقة اتصال • أبو وسام"); subtitle.setTextSize(14);
            subtitle.setTextColor(Color.rgb(205,220,240)); subtitle.setGravity(Gravity.CENTER); hero.addView(subtitle);

            TextView section = new TextView(this); section.setText("بطاقة الاتصال الخاصة بك");
            section.setTextSize(17); section.setTypeface(null,1); section.setTextColor(Color.rgb(30,55,90));
            section.setGravity(Gravity.CENTER); section.setPadding(0,dp(20),0,dp(8)); root.addView(section);

            LinearLayout card = new LinearLayout(this); card.setOrientation(1); card.setGravity(Gravity.CENTER);
            card.setPadding(dp(12),dp(14),dp(12),dp(14)); card.setBackground(shape(Color.WHITE,22));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,0,0,dp(14)); root.addView(card,cp);
            contact = new TextView(this); contact.setTextSize(19); contact.setTypeface(null,1);
            contact.setTextColor(Color.rgb(13,34,66)); contact.setGravity(Gravity.CENTER);
            contact.setPadding(dp(8),dp(4),dp(8),dp(10)); card.addView(contact);
            qr = new ImageView(this); qr.setAdjustViewBounds(true);
            LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(dp(220),dp(220)); qp.gravity=Gravity.CENTER; card.addView(qr,qp);

            TextView help = new TextView(this);
            help.setText("أرسل جهة الاتصال كملف VCF ليتمكن المستلم من فتحها وحفظ الاسم والرقم في هاتفه.");
            help.setTextSize(14); help.setTextColor(Color.rgb(82,99,122)); help.setGravity(Gravity.CENTER);
            help.setPadding(dp(8),dp(8),dp(8),dp(14)); root.addView(help);

            addButton("إرسال جهة الاتصال  •  VCF", Color.rgb(13,148,136), Color.WHITE, v->shareVcard());
            addButton("تعديل الاسم والرقم", Color.rgb(49,86,166), Color.WHITE, v->editContact());

            TextView note = new TextView(this);
            note.setText("الإرسال فقط — لا يستقبل التطبيق ملفات.\nNFC لا يرسل تلقائيًا إلى جميع الهواتف؛ استخدم زر VCF للمشاركة.");
            note.setTextSize(12); note.setTextColor(Color.rgb(112,125,145)); note.setGravity(Gravity.CENTER);
            note.setPadding(0,dp(16),0,0); root.addView(note);

            setContentView(sc); refreshCard();
        } catch(Exception e) { showError("تعذر تشغيل التطبيق. أعد فتحه وحاول مجددًا."); }
    }

    private String makeVcard() {
        String safeName=ownerName.replace("\\","\\\\").replace(";","\\;").replace(",","\\,").replace("\n"," ");
        return "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:"+safeName+"\r\nTEL;TYPE=CELL:"+phone+"\r\nEND:VCARD\r\n";
    }

    private void refreshCard() {
        contact.setText(ownerName+"\n"+phone);
        try {
            int size=dp(220); BitMatrix m=new MultiFormatWriter().encode(makeVcard(),BarcodeFormat.QR_CODE,size,size);
            Bitmap bm=Bitmap.createBitmap(m.getWidth(),m.getHeight(),Bitmap.Config.RGB_565);
            for(int x=0;x<m.getWidth();x++) for(int y=0;y<m.getHeight();y++) bm.setPixel(x,y,m.get(x,y)?Color.BLACK:Color.WHITE);
            qr.setImageBitmap(bm);
        } catch(Exception e) { qr.setImageDrawable(null); Toast.makeText(this,"تعذر إنشاء رمز QR",Toast.LENGTH_LONG).show(); }
    }

    private void editContact() {
        LinearLayout f=new LinearLayout(this); f.setOrientation(1); int pad=dp(12); f.setPadding(pad,0,pad,0);
        EditText n=new EditText(this); n.setSingleLine(true); n.setHint("الاسم"); n.setText(ownerName); f.addView(n);
        EditText p=new EditText(this); p.setSingleLine(true); p.setHint("رقم الهاتف مع مفتاح الدولة");
        p.setInputType(InputType.TYPE_CLASS_PHONE); p.setText(phone); f.addView(p);
        new android.app.AlertDialog.Builder(this).setTitle("تعديل بيانات الاتصال").setView(f)
          .setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{
            String nn=n.getText().toString().trim(), pp=p.getText().toString().trim();
            if(nn.isEmpty()||pp.isEmpty()){Toast.makeText(this,"أدخل الاسم والرقم",Toast.LENGTH_SHORT).show();return;}
            ownerName=nn; phone=pp;
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY_NAME,ownerName).putString(KEY_PHONE,phone).apply();
            refreshCard(); Toast.makeText(this,"تم حفظ بيانات جهة الاتصال",Toast.LENGTH_SHORT).show();
          }).show();
    }

    private void shareVcard() {
        try {
            File dir=new File(getCacheDir(),"contacts");
            if(!dir.exists()&&!dir.mkdirs()) throw new java.io.IOException("Cannot create cache directory");
            File file=new File(dir,"abu-wessam-contact.vcf");
            try(FileOutputStream out=new FileOutputStream(file,false)){out.write(makeVcard().getBytes(StandardCharsets.UTF_8));out.flush();}
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
            Intent send=new Intent(Intent.ACTION_SEND); send.setType("text/vcard");
            send.putExtra(Intent.EXTRA_STREAM,uri); send.putExtra(Intent.EXTRA_SUBJECT,"جهة اتصال: "+ownerName);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send,"إرسال جهة الاتصال"));
        } catch(Exception e) { showError("تعذر تجهيز ملف VCF للمشاركة"); }
    }

    private void showError(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }

    private void addButton(String text,int bg,int fg,android.view.View.OnClickListener click) {
        Button b=new Button(this); b.setText(text); b.setTextColor(fg); b.setTextSize(16); b.setTypeface(null,1);
        b.setAllCaps(false); b.setBackground(shape(bg,18)); b.setElevation(dp(3)); b.setOnClickListener(click);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58)); p.setMargins(0,dp(5),0,dp(5)); root.addView(b,p);
    }
}