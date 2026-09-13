package de.reko.app;

import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import java.io.*;

public final class DocumentEditorActivity extends AppCompatActivity {
    private Bitmap bitmap;
    private ImageView preview;
    private Slider left,top,right,bottom;
    private long profileId;
    private String sourceName,mime;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        profileId=getIntent().getLongExtra("profileId",-1);String path=getIntent().getStringExtra("path");sourceName=getIntent().getStringExtra("name");mime=getIntent().getStringExtra("mime");
        if(profileId<0||path==null||!("image/jpeg".equals(mime)||"image/png".equals(mime))){finish();return;}
        bitmap=decode(new File(path));if(bitmap==null){Toast.makeText(this,"Bild konnte nicht geöffnet werden.",Toast.LENGTH_LONG).show();finish();return;}
        buildUi();
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(12),dp(18),dp(18));
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,i)->{androidx.core.graphics.Insets b=i.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(dp(18)+b.left,dp(12)+b.top,dp(18)+b.right,dp(18)+b.bottom);return i;});
        TextView title=new TextView(this);title.setText("Beleg bearbeiten");title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall);root.addView(title);
        TextView hint=new TextView(this);hint.setText("Original bleibt unverändert. Zuschneiden über die vier Randregler, Drehen in 90°-Schritten.");hint.setPadding(0,dp(6),0,dp(12));root.addView(hint);
        preview=new ImageView(this);preview.setAdjustViewBounds(true);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setImageBitmap(bitmap);root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
        left=slider(root,"Links abschneiden");top=slider(root,"Oben abschneiden");right=slider(root,"Rechts abschneiden");bottom=slider(root,"Unten abschneiden");
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setPadding(0,dp(10),0,0);
        MaterialButton rotate=new MaterialButton(this);rotate.setText("90° drehen");rotate.setOnClickListener(v->rotate());actions.addView(rotate,new LinearLayout.LayoutParams(0,-2,1));
        MaterialButton save=new MaterialButton(this);save.setText("Kopie speichern");save.setOnClickListener(v->save());LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,-2,1);sp.leftMargin=dp(8);actions.addView(save,sp);root.addView(actions);
        setContentView(root);
    }

    private Slider slider(LinearLayout root,String label){TextView t=new TextView(this);t.setText(label);root.addView(t);Slider s=new Slider(this);s.setValueFrom(0);s.setValueTo(40);s.setStepSize(1);s.setValue(0);root.addView(s,new LinearLayout.LayoutParams(-1,-2));return s;}
    private void rotate(){Matrix m=new Matrix();m.postRotate(90);Bitmap next=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,true);if(next!=bitmap)bitmap.recycle();bitmap=next;preview.setImageBitmap(bitmap);}
    private void save(){
        float l=left.getValue()/100f,t=top.getValue()/100f,r=right.getValue()/100f,b=bottom.getValue()/100f;
        if(l+r>=0.9f||t+b>=0.9f){Toast.makeText(this,"Zuschneidebereich ist zu klein.",Toast.LENGTH_LONG).show();return;}
        int x=Math.round(bitmap.getWidth()*l),y=Math.round(bitmap.getHeight()*t);int w=bitmap.getWidth()-x-Math.round(bitmap.getWidth()*r),h=bitmap.getHeight()-y-Math.round(bitmap.getHeight()*b);
        Bitmap cropped=Bitmap.createBitmap(bitmap,x,y,w,h);File tmp=new File(getCacheDir(),"edited-"+System.nanoTime()+("image/png".equals(mime)?".png":".jpg"));
        try(OutputStream out=new FileOutputStream(tmp)){
            Bitmap.CompressFormat f="image/png".equals(mime)?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.JPEG;if(!cropped.compress(f,92,out))throw new IOException("Speichern fehlgeschlagen");
            String editedName="bearbeitet-"+(sourceName==null?"beleg":sourceName);DocumentStore.Result result=new DocumentStore(this,new RekoDb(this)).importFile(profileId,tmp,editedName,mime);
            Toast.makeText(this,result.duplicate?"Diese bearbeitete Version existiert bereits.":"Bearbeitete Kopie gespeichert.",Toast.LENGTH_LONG).show();setResult(RESULT_OK);finish();
        }catch(Exception e){Toast.makeText(this,"Bearbeitete Kopie konnte nicht gespeichert werden.",Toast.LENGTH_LONG).show();}finally{tmp.delete();if(cropped!=bitmap)cropped.recycle();}
    }

    private Bitmap decode(File file){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getAbsolutePath(),o);int sample=1;while(o.outWidth/sample>2048||o.outHeight/sample>2048)sample*=2;o.inJustDecodeBounds=false;o.inSampleSize=sample;return BitmapFactory.decodeFile(file.getAbsolutePath(),o);}
    @Override protected void onDestroy(){super.onDestroy();if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
