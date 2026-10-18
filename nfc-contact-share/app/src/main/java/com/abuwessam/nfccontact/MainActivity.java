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
 private String ownerName="أحمد رشيد - أبو وسام", phone="+963938342496";
 private LinearLayout root; private TextView contact; private ImageView qr;
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 @Override public void onCreate(Bundle b){
  super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(10,37,64));getWindow().setNavigationBarColor(Color.rgb(10,37,64));
  root=new LinearLayout(this);root.setOrientation(1);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(20),dp(16),dp(20),dp(22));root.setBackgroundColor(Color.rgb(242,247,251));
  ScrollView sc=new ScrollView(this);sc.addView(root);
  TextView logo=new TextView(this);logo.setText("◉ NFC");logo.setTextSize(27);logo.setTextColor(-1);logo.setGravity(Gravity.CENTER);logo.setBackgroundColor(Color.rgb(10,37,64));root.addView(logo,new LinearLayout.LayoutParams(-1,dp(72)));
  TextView title=new TextView(this);title.setText("رقمي عبر NFC");title.setTextSize(27);title.setTextColor(Color.rgb(10,37,64));title.setGravity(Gravity.CENTER);title.setPadding(0,dp(18),0,dp(4));root.addView(title);
  TextView sub=new TextView(this);sub.setText("أبو وسام • بطاقة اتصال رقمية");sub.setTextSize(16);sub.setGravity(Gravity.CENTER);root.addView(sub);
  contact=new TextView(this);contact.setTextSize(19);contact.setTextColor(Color.rgb(10,37,64));contact.setGravity(Gravity.CENTER);contact.setPadding(dp(8),dp(18),dp(8),dp(12));root.addView(contact);
  qr=new ImageView(this);qr.setAdjustViewBounds(true);LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(dp(230),dp(230));qp.gravity=Gravity.CENTER;root.addView(qr,qp);
  TextView help=new TextView(this);help.setText("اعرض رمز QR ليقوم الطرف الآخر بمسحه بكاميرا هاتفه. لا يحتاج إلى تثبيت التطبيق. يمكنك تغيير الاسم والرقم من زر تعديل بياناتي.");help.setTextSize(14);help.setTextColor(Color.DKGRAY);help.setGravity(Gravity.CENTER);help.setPadding(0,dp(12),0,dp(12));root.addView(help);
  addButton("تعديل الاسم والرقم",v->editContact());
  addButton("نسخ رقم الهاتف",v->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("رقم الهاتف",phone));Toast.makeText(this,"تم نسخ الرقم",Toast.LENGTH_SHORT).show();});
  addButton("مشاركة بطاقة الاتصال",v->shareVcard());
  addButton("إرسال الرقم برسالة",v->{Intent i=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"));i.putExtra("sms_body",ownerName+"\n"+phone);startActivity(i);});
  TextView note=new TextView(this);note.setText("ملاحظة: NFC لا يضمن نقل البيانات تلقائيًا إلى كل الهواتف. رمز QR هو البديل الأكثر توافقًا.");note.setTextSize(13);note.setTextColor(Color.GRAY);note.setGravity(Gravity.CENTER);note.setPadding(0,dp(16),0,0);root.addView(note);
  setContentView(sc);refreshCard();
 }
 private void refreshCard(){contact.setText(ownerName+"\n"+phone);try{String v="BEGIN:VCARD\nVERSION:3.0\nFN:"+ownerName+"\nTEL;TYPE=CELL:"+phone+"\nEND:VCARD";BitMatrix m=new MultiFormatWriter().encode(v,BarcodeFormat.QR_CODE,dp(230),dp(230));Bitmap bm=Bitmap.createBitmap(m.getWidth(),m.getHeight(),Bitmap.Config.RGB_565);for(int x=0;x<m.getWidth();x++)for(int y=0;y<m.getHeight();y++)bm.setPixel(x,y,m.get(x,y)?Color.BLACK:Color.WHITE);qr.setImageBitmap(bm);}catch(Exception e){Toast.makeText(this,"تعذر إنشاء رمز QR",Toast.LENGTH_SHORT).show();}}
 private void editContact(){LinearLayout f=new LinearLayout(this);f.setOrientation(1);EditText n=new EditText(this);n.setSingleLine(true);n.setHint("الاسم");n.setText(ownerName);f.addView(n);EditText p=new EditText(this);p.setSingleLine(true);p.setHint("رقم الهاتف مع مفتاح الدولة");p.setInputType(InputType.TYPE_CLASS_PHONE);p.setText(phone);f.addView(p);new android.app.AlertDialog.Builder(this).setTitle("تعديل بيانات الاتصال").setView(f).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{String nn=n.getText().toString().trim(),pp=p.getText().toString().trim();if(nn.isEmpty()||pp.isEmpty()){Toast.makeText(this,"أدخل الاسم والرقم",Toast.LENGTH_SHORT).show();return;}ownerName=nn;phone=pp;refreshCard();Toast.makeText(this,"تم تحديث البطاقة ورمز QR",Toast.LENGTH_SHORT).show();}).show();}
 private void shareVcard(){String v="BEGIN:VCARD\nVERSION:3.0\nFN:"+ownerName+"\nTEL;TYPE=CELL:"+phone+"\nEND:VCARD";Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/vcard");i.putExtra(Intent.EXTRA_TEXT,v);i.putExtra(Intent.EXTRA_SUBJECT,"بطاقة اتصال "+ownerName);startActivity(Intent.createChooser(i,"مشاركة بطاقة الاتصال"));}
 private void addButton(String t,android.view.View.OnClickListener l){Button b=new Button(this);b.setText(t);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(17,91,139)));b.setOnClickListener(l);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(4),0,dp(4));root.addView(b,p);}
}