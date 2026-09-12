package de.reko.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private final RekoDb db = new RekoDb(this);
    private LinearLayout body;
    private TextView title;
    private MaterialButton profileButton;
    private BottomNavigationView nav;
    private long profileId;
    private ActivityResultLauncher<String> createBackup, calendarPermission;
    private ActivityResultLauncher<String[]> openBackup;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setupLaunchers();
        profileId=db.activeProfile(this);
        buildShell();
        handleIntent(getIntent());
    }
    @Override protected void onNewIntent(Intent intent){ super.onNewIntent(intent); setIntent(intent); handleIntent(intent); }

    private void setupLaunchers(){
        createBackup=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),uri->{if(uri!=null)writeBackup(uri);});
        openBackup=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{if(uri!=null)readBackup(uri);});
        calendarPermission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),ok->toast(ok?"Kalenderzugriff erlaubt. Kalenderauswahl folgt in einer späteren Ausbaustufe.":"Kalenderzugriff nicht erteilt."));
    }

    private void buildShell(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(MaterialColors.getColor(root, com.google.android.material.R.attr.colorSurface));
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            return insets;
        });

        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20),dp(16),dp(20),dp(10));
        title=new TextView(this);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        header.addView(title,new LinearLayout.LayoutParams(-1,-2));
        profileButton=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);
        profileButton.setAllCaps(false);
        profileButton.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        profileButton.setIconResource(R.drawable.ic_profile);
        profileButton.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        profileButton.setOnClickListener(v->chooseProfile());
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2); pp.topMargin=dp(10); header.addView(profileButton,pp);
        root.addView(header);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20),dp(6),dp(20),dp(24));
        scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        nav=new BottomNavigationView(this);
        nav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        nav.getMenu().add(0,1,0,"Übersicht").setIcon(R.drawable.ic_overview);
        nav.getMenu().add(0,2,1,"Reise").setIcon(R.drawable.ic_trip);
        nav.getMenu().add(0,3,2,"Bewirtung").setIcon(R.drawable.ic_hospitality);
        nav.getMenu().add(0,4,3,"Einstellungen").setIcon(R.drawable.ic_settings);
        nav.setOnItemSelectedListener(i->{show(i.getItemId());return true;});
        root.addView(nav,new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);
        refreshProfileLabel();
        nav.setSelectedItemId(1);
    }

    private void show(int id){ body.removeAllViews(); if(id==1)overview(); else if(id==2)tripForm(); else if(id==3)hospitalityForm(); else settings(); }
    private void refreshProfileLabel(){ String name="Profil";for(String[] p:db.profiles())if(Long.parseLong(p[0])==profileId)name=p[1];profileButton.setText("Aktives Profil: "+name); }
    private void chooseProfile(){ ArrayList<String[]> ps=db.profiles();String[] names=new String[ps.size()+1];for(int i=0;i<ps.size();i++)names[i]=ps.get(i)[1];names[ps.size()]="+ Neues Profil";new AlertDialog.Builder(this).setTitle("Profil wählen").setItems(names,(d,w)->{if(w==ps.size())newProfile();else{profileId=Long.parseLong(ps.get(w)[0]);db.setActiveProfile(this,profileId);refreshProfileLabel();nav.setSelectedItemId(1);}}).show(); }
    private void newProfile(){ EditText e=new EditText(this);e.setHint("Profilname");new AlertDialog.Builder(this).setTitle("Neues Profil").setView(e).setPositiveButton("Anlegen",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty()){profileId=db.addProfile(n);db.setActiveProfile(this,profileId);refreshProfileLabel();nav.setSelectedItemId(1);}}).setNegativeButton("Abbrechen",null).show(); }

    private TextInputEditText field(String hint){
        TextInputLayout l=new TextInputLayout(this);
        l.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        l.setHint(hint);
        TextInputEditText e=new TextInputEditText(l.getContext());
        l.addView(e,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);body.addView(l,p);return e;
    }
    private MaterialButton button(String text){ MaterialButton b=new MaterialButton(this);b.setText(text);b.setAllCaps(false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);body.addView(b,p);return b; }
    private MaterialButton textButton(String text,int icon,View.OnClickListener listener){MaterialButton b=new MaterialButton(this);b.setText(text);b.setAllCaps(false);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setIconResource(icon);b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));int primary=MaterialColors.getColor(b,com.google.android.material.R.attr.colorPrimary);b.setTextColor(primary);b.setIconTint(android.content.res.ColorStateList.valueOf(primary));b.setOnClickListener(listener);return b;}
    private String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.GERMANY).format(new Date());}

    private void tripForm(){ title.setText("Reise");sectionIntro("Neue Reise","Reisedaten bleiben lokal auf diesem Gerät.",R.drawable.ic_trip);TextInputEditText start=field("Startdatum, YYYY-MM-DD");start.setText(today());TextInputEditText end=field("Enddatum, YYYY-MM-DD");end.setText(today());TextInputEditText dest=field("Ziel / Ort");TextInputEditText purpose=field("Beruflicher Anlass");TextInputEditText notes=field("Notizen, optional");MaterialButton save=button("Reise speichern");save.setIconResource(R.drawable.ic_save);save.setOnClickListener(v->{if(blank(dest)||blank(purpose)){toast("Ziel und Anlass fehlen.");return;}db.addTrip(profileId,s(start),s(end),s(dest),s(purpose),s(notes));toast("Reise gespeichert.");nav.setSelectedItemId(1);}); }
    private void hospitalityForm(){ title.setText("Bewirtung");sectionIntro("Bewirtung erfassen","Teilnehmer und geschäftlicher Anlass sind Pflichtangaben.",R.drawable.ic_hospitality);TextInputEditText date=field("Datum, YYYY-MM-DD");date.setText(today());TextInputEditText place=field("Ort / Betrieb");TextInputEditText amount=field("Betrag in EUR");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);TextInputEditText people=field("Teilnehmer");TextInputEditText purpose=field("Geschäftlicher Anlass");TextInputEditText tip=field("Trinkgeld in EUR, optional");tip.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);MaterialButton save=button("Bewirtung speichern");save.setIconResource(R.drawable.ic_save);save.setOnClickListener(v->{if(blank(place)||blank(amount)||blank(people)||blank(purpose)){toast("Bitte Pflichtfelder ergänzen.");return;}try{long cents=Math.round(Double.parseDouble(s(amount).replace(',','.'))*100);long tipC=s(tip).isEmpty()?0:Math.round(Double.parseDouble(s(tip).replace(',','.'))*100);db.addHospitality(profileId,s(date),s(place),cents,s(people),s(purpose),tipC);toast("Bewirtung gespeichert.");nav.setSelectedItemId(1);}catch(Exception ex){toast("Betrag ist ungültig.");}}); }
    private void overview(){ title.setText("Übersicht");sectionIntro("Deine Einträge","Reisen und Bewirtungen des aktiven Profils.",R.drawable.ic_overview);ArrayList<String> items=db.summary(profileId);if(items.isEmpty())emptyState();for(String x:items){MaterialCardView c=new MaterialCardView(this);c.setRadius(dp(20));c.setCardElevation(0);c.setStrokeWidth(dp(1));c.setStrokeColor(MaterialColors.getColor(c,com.google.android.material.R.attr.colorOutlineVariant));TextView t=new TextView(this);t.setText(x);t.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);t.setPadding(dp(16),dp(16),dp(16),dp(16));c.addView(t);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(12));body.addView(c,p);} }

    private void settings(){
        title.setText("Einstellungen");
        settingsCard("Profil & Daten",new String[]{"Profil wechseln / verwalten","Profil-Backup exportieren","Backup in aktives Profil importieren"},new View.OnClickListener[]{v->chooseProfile(),v->createBackup.launch("reko-backup-"+System.currentTimeMillis()+".json"),v->confirmImport()});
        settingsCard("Berechtigungen",new String[]{"Kalenderzugriff aktivieren"},new View.OnClickListener[]{v->requestCalendar()});
        settingsCard("Datenschutz",new String[]{"Local-first. Keine Telemetrie. Keine zentrale ReKo-Datenspeicherung. Android-Systembackup ist deaktiviert.","ReKo ersetzt keine Rechts- oder Steuerberatung. Keine Gewähr für Vollständigkeit, Aktualität oder steuerliche Anerkennung."},null);

        Space spacer=new Space(this);body.addView(spacer,new LinearLayout.LayoutParams(1,dp(24)));
        MaterialDivider divider=new MaterialDivider(this);body.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        TextView footerTitle=new TextView(this);footerTitle.setText("ReKo");footerTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);footerTitle.setPadding(0,dp(18),0,dp(4));body.addView(footerTitle);
        body.addView(textButton("GitHub Repository",R.drawable.ic_github,v->open("https://github.com/3115a083/reko")),new LinearLayout.LayoutParams(-1,-2));
        body.addView(textButton("Nach Updates suchen",R.drawable.ic_update,v->open("https://github.com/3115a083/reko/releases")),new LinearLayout.LayoutParams(-1,-2));
        TextView f=new TextView(this);f.setText("Vibecoded with ❤️");f.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);f.setGravity(Gravity.CENTER);f.setPadding(0,dp(12),0,dp(4));body.addView(f,new LinearLayout.LayoutParams(-1,-2));
    }

    private void settingsCard(String heading,String[] rows,View.OnClickListener[] listeners){
        MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(20));card.setCardElevation(0);card.setStrokeWidth(dp(1));card.setStrokeColor(MaterialColors.getColor(card,com.google.android.material.R.attr.colorOutlineVariant));
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(14),dp(16),dp(14));
        TextView h=new TextView(this);h.setText(heading);h.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);box.addView(h);
        for(int i=0;i<rows.length;i++){
            if(listeners!=null){MaterialButton b=new MaterialButton(this);b.setText(rows[i]);b.setAllCaps(false);b.setGravity(Gravity.START);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));b.setTextColor(MaterialColors.getColor(b,com.google.android.material.R.attr.colorPrimary));b.setOnClickListener(listeners[i]);box.addView(b,new LinearLayout.LayoutParams(-1,-2));}
            else{TextView t=new TextView(this);t.setText(rows[i]);t.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);t.setPadding(0,dp(10),0,0);box.addView(t);}
        }
        card.addView(box);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);body.addView(card,p);
    }

    private void sectionIntro(String heading,String text,int icon){MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(20));card.setCardElevation(0);card.setCardBackgroundColor(MaterialColors.getColor(card,com.google.android.material.R.attr.colorSecondaryContainer));LinearLayout row=new LinearLayout(this);row.setPadding(dp(16),dp(14),dp(16),dp(14));row.setGravity(Gravity.CENTER_VERTICAL);ImageView iv=new ImageView(this);iv.setImageResource(icon);iv.setImageTintList(android.content.res.ColorStateList.valueOf(MaterialColors.getColor(iv,com.google.android.material.R.attr.colorOnSecondaryContainer)));row.addView(iv,new LinearLayout.LayoutParams(dp(28),dp(28)));LinearLayout texts=new LinearLayout(this);texts.setOrientation(LinearLayout.VERTICAL);texts.setPadding(dp(14),0,0,0);TextView h=new TextView(this);h.setText(heading);h.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);TextView d=new TextView(this);d.setText(text);d.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);texts.addView(h);texts.addView(d);row.addView(texts,new LinearLayout.LayoutParams(0,-2,1));card.addView(row);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(18);body.addView(card,p);}
    private void emptyState(){MaterialCardView c=new MaterialCardView(this);c.setRadius(dp(20));c.setCardElevation(0);TextView t=new TextView(this);t.setText("Noch keine Einträge. Erfasse deine erste Reise oder Bewirtung über die Navigation unten.");t.setGravity(Gravity.CENTER);t.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);t.setPadding(dp(24),dp(28),dp(24),dp(28));c.addView(t);body.addView(c,new LinearLayout.LayoutParams(-1,-2));}

    private void confirmImport(){new AlertDialog.Builder(this).setTitle("Backup importieren?").setMessage("Importierte Datensätze werden dem aktiven Profil hinzugefügt. Bestehende Daten werden nicht überschrieben.").setPositiveButton("Datei wählen",(d,w)->openBackup.launch(new String[]{"application/json","text/plain"})).setNegativeButton("Abbrechen",null).show();}
    private void requestCalendar(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED)toast("Kalenderzugriff ist bereits erlaubt.");else calendarPermission.launch(Manifest.permission.READ_CALENDAR);}
    private void handleIntent(Intent i){String a=i.getAction();if("de.reko.app.NEW_TRIP".equals(a)){nav.setSelectedItemId(2);return;}if("de.reko.app.NEW_HOSPITALITY".equals(a)){nav.setSelectedItemId(3);return;}if(Intent.ACTION_SEND.equals(a)){Uri u=i.getParcelableExtra(Intent.EXTRA_STREAM);if(u!=null)importShared(u,i.getType());}}

    private void importShared(Uri uri,String mime){
        try{
            if(!"content".equalsIgnoreCase(uri.getScheme())) throw new SecurityException("Nur content-URIs erlaubt");
            String authority=uri.getAuthority();
            if(authority==null || authority.isBlank()) throw new SecurityException("Fehlende URI-Autorität");
            if(authority.equals(getPackageName()) || authority.startsWith(getPackageName()+".")) throw new SecurityException("Eigene Provider nicht als Share-Quelle erlaubt");
            String path=uri.getPath();
            if(path==null) throw new SecurityException("Fehlender URI-Pfad");
            Path normalized=FileSystems.getDefault().getPath(path).normalize();
            if(normalized.startsWith("/data") || normalized.startsWith("/proc") || normalized.startsWith("/sys") || normalized.startsWith("/dev")) throw new SecurityException("Privater Systempfad nicht erlaubt");
            String resolvedMime=getContentResolver().getType(uri);
            if(resolvedMime==null) resolvedMime=mime;
            if(!("application/pdf".equals(resolvedMime) || "image/jpeg".equals(resolvedMime) || "image/png".equals(resolvedMime))) throw new SecurityException("Dateityp nicht erlaubt");

            String safe="shared-"+System.currentTimeMillis();
            File out=new File(getFilesDir(),safe);
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            try(InputStream in=getContentResolver().openInputStream(uri);OutputStream os=new FileOutputStream(out)){
                if(in==null)throw new IOException("Datei nicht lesbar");
                byte[] buf=new byte[8192];int n;long total=0;
                while((n=in.read(buf))>0){total+=n;if(total>25L*1024*1024)throw new IOException("Datei größer als 25 MB");md.update(buf,0,n);os.write(buf,0,n);}
            }
            toast("Geteilte Datei lokal übernommen. Zuordnung zu Vorgängen folgt in einer späteren Ausbaustufe.");
        }catch(Exception e){toast("Datei konnte nicht sicher importiert werden.");}
    }

    private void writeBackup(Uri uri){try(OutputStream o=getContentResolver().openOutputStream(uri)){if(o==null)throw new IOException();o.write(db.exportProfile(profileId).toString(2).getBytes(StandardCharsets.UTF_8));toast("Backup exportiert.");}catch(Exception e){toast("Backup-Export fehlgeschlagen.");}}
    private void readBackup(Uri uri){try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[8192];int n;long total=0;while((n=in.read(x))>0){total+=n;if(total>5L*1024*1024)throw new IOException("zu groß");b.write(x,0,n);}db.importInto(profileId,new JSONObject(b.toString(StandardCharsets.UTF_8)));toast("Backup importiert.");nav.setSelectedItemId(1);}catch(Exception e){toast("Backup ungültig oder beschädigt.");}}
    private void open(String url){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
    private boolean blank(TextInputEditText e){return s(e).isEmpty();}private String s(TextInputEditText e){return e.getText()==null?"":e.getText().toString().trim();}private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
