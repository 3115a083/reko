package de.reko.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private final RekoDb db = new RekoDb(this);
    private LinearLayout body;
    private TextView title, profileLabel;
    private long profileId;
    private ActivityResultLauncher<String> createBackup, openBackup, calendarPermission;

    @Override protected void onCreate(Bundle b){ super.onCreate(b); setupLaunchers(); profileId=db.activeProfile(this); buildShell(); handleIntent(getIntent()); }
    @Override protected void onNewIntent(Intent intent){ super.onNewIntent(intent); setIntent(intent); handleIntent(intent); }

    private void setupLaunchers(){
        createBackup=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),uri->{if(uri!=null)writeBackup(uri);});
        openBackup=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{if(uri!=null)readBackup(uri);});
        calendarPermission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),ok->toast(ok?"Kalenderzugriff erlaubt. Kalenderauswahl folgt in einer späteren Ausbaustufe.":"Kalenderzugriff nicht erteilt."));
    }

    private void buildShell(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),0);
        title=new TextView(this);title.setTextSize(26);title.setTypeface(null,1);root.addView(title);
        profileLabel=new TextView(this);profileLabel.setPadding(0,dp(4),0,dp(8));profileLabel.setOnClickListener(v->chooseProfile());root.addView(profileLabel);
        ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,0,0,dp(12));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        BottomNavigationView nav=new BottomNavigationView(this);nav.getMenu().add(0,1,0,"Reise").setIcon(android.R.drawable.ic_menu_mylocation);nav.getMenu().add(0,2,1,"Bewirtung").setIcon(android.R.drawable.ic_menu_agenda);nav.getMenu().add(0,3,2,"Übersicht").setIcon(android.R.drawable.ic_menu_view);nav.getMenu().add(0,4,3,"Einstellungen").setIcon(android.R.drawable.ic_menu_preferences);nav.setOnItemSelectedListener(i->{show(i.getItemId());return true;});root.addView(nav);setContentView(root);refreshProfileLabel();nav.setSelectedItemId(3);
    }
    private void show(int id){ body.removeAllViews(); if(id==1)tripForm(); else if(id==2)hospitalityForm(); else if(id==3)overview(); else settings(); }
    private void refreshProfileLabel(){ String name="Profil";for(String[] p:db.profiles())if(Long.parseLong(p[0])==profileId)name=p[1];profileLabel.setText("Aktives Profil: "+name+"  ▾"); }
    private void chooseProfile(){ ArrayList<String[]> ps=db.profiles();String[] names=new String[ps.size()+1];for(int i=0;i<ps.size();i++)names[i]=ps.get(i)[1];names[ps.size()]="+ Neues Profil";new AlertDialog.Builder(this).setTitle("Profil wählen").setItems(names,(d,w)->{if(w==ps.size())newProfile();else{profileId=Long.parseLong(ps.get(w)[0]);db.setActiveProfile(this,profileId);refreshProfileLabel();overview();}}).show(); }
    private void newProfile(){ EditText e=new EditText(this);e.setHint("Profilname");new AlertDialog.Builder(this).setTitle("Neues Profil").setView(e).setPositiveButton("Anlegen",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty()){profileId=db.addProfile(n);db.setActiveProfile(this,profileId);refreshProfileLabel();overview();}}).setNegativeButton("Abbrechen",null).show(); }

    private TextInputEditText field(String hint){ TextInputLayout l=new TextInputLayout(this);TextInputEditText e=new TextInputEditText(this);e.setHint(hint);l.addView(e);body.addView(l,new LinearLayout.LayoutParams(-1,-2));return e; }
    private MaterialButton button(String text){ MaterialButton b=new MaterialButton(this);b.setText(text);body.addView(b,new LinearLayout.LayoutParams(-1,-2));return b; }
    private String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.GERMANY).format(new Date());}

    private void tripForm(){ title.setText("Reise");TextView h=note("Reise erfassen. Pflichtfelder bleiben lokal auf diesem Gerät.");TextInputEditText start=field("Startdatum, YYYY-MM-DD");start.setText(today());TextInputEditText end=field("Enddatum, YYYY-MM-DD");end.setText(today());TextInputEditText dest=field("Ziel / Ort");TextInputEditText purpose=field("Beruflicher Anlass");TextInputEditText notes=field("Notizen, optional");button("Reise speichern").setOnClickListener(v->{if(blank(dest)||blank(purpose)){toast("Ziel und Anlass fehlen.");return;}db.addTrip(profileId,s(start),s(end),s(dest),s(purpose),s(notes));toast("Reise gespeichert.");overview();}); }
    private void hospitalityForm(){ title.setText("Bewirtung");note("Bewirtung erfassen. Teilnehmer und geschäftlicher Anlass sind Pflichtfelder.");TextInputEditText date=field("Datum, YYYY-MM-DD");date.setText(today());TextInputEditText place=field("Ort / Betrieb");TextInputEditText amount=field("Betrag in EUR");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);TextInputEditText people=field("Teilnehmer");TextInputEditText purpose=field("Geschäftlicher Anlass");TextInputEditText tip=field("Trinkgeld in EUR, optional");tip.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);button("Bewirtung speichern").setOnClickListener(v->{if(blank(place)||blank(amount)||blank(people)||blank(purpose)){toast("Bitte Pflichtfelder ergänzen.");return;}try{long cents=Math.round(Double.parseDouble(s(amount).replace(',','.'))*100);long tipC=s(tip).isEmpty()?0:Math.round(Double.parseDouble(s(tip).replace(',','.'))*100);db.addHospitality(profileId,s(date),s(place),cents,s(people),s(purpose),tipC);toast("Bewirtung gespeichert.");overview();}catch(Exception ex){toast("Betrag ist ungültig.");}}); }
    private void overview(){ title.setText("Übersicht");ArrayList<String> items=db.summary(profileId);if(items.isEmpty())note("Noch keine Einträge in diesem Profil.");for(String x:items){MaterialCardView c=new MaterialCardView(this);c.setRadius(dp(14));c.setCardElevation(dp(1));TextView t=new TextView(this);t.setText(x);t.setTextSize(16);t.setPadding(dp(14),dp(12),dp(14),dp(12));c.addView(t);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));body.addView(c,p);} }
    private void settings(){ title.setText("Einstellungen");button("Profil wechseln / verwalten").setOnClickListener(v->chooseProfile());button("Profil-Backup exportieren").setOnClickListener(v->createBackup.launch("reko-backup-"+System.currentTimeMillis()+".json"));button("Backup in aktives Profil importieren").setOnClickListener(v->confirmImport());button("Kalenderzugriff aktivieren").setOnClickListener(v->requestCalendar());button("Öffentliches GitHub-Repository").setOnClickListener(v->open("https://github.com/3115a083/reko"));button("Nach Updates suchen").setOnClickListener(v->open("https://github.com/3115a083/reko/releases"));note("Datenschutz: Local-first. Keine Telemetrie. Keine zentrale ReKo-Datenspeicherung. Android-Systembackup ist deaktiviert.");note("Hinweis: ReKo ersetzt keine Rechts- oder Steuerberatung. Keine Gewähr für Vollständigkeit, Aktualität oder steuerliche Anerkennung.");TextView f=note("Vibecoded with ❤️");f.setGravity(Gravity.CENTER); }
    private void confirmImport(){new AlertDialog.Builder(this).setTitle("Backup importieren?").setMessage("Importierte Datensätze werden dem aktiven Profil hinzugefügt. Bestehende Daten werden nicht überschrieben.").setPositiveButton("Datei wählen",(d,w)->openBackup.launch(new String[]{"application/json","text/plain"})).setNegativeButton("Abbrechen",null).show();}
    private void requestCalendar(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED)toast("Kalenderzugriff ist bereits erlaubt.");else calendarPermission.launch(Manifest.permission.READ_CALENDAR);}
    private void handleIntent(Intent i){String a=i.getAction();if("de.reko.app.NEW_TRIP".equals(a)){show(1);return;}if("de.reko.app.NEW_HOSPITALITY".equals(a)){show(2);return;}if(Intent.ACTION_SEND.equals(a)){Uri u=i.getParcelableExtra(Intent.EXTRA_STREAM);if(u!=null)importShared(u,i.getType());}}
    private void importShared(Uri uri,String mime){try{String safe="shared-"+System.currentTimeMillis();File out=new File(getFilesDir(),safe);MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=getContentResolver().openInputStream(uri);OutputStream os=new FileOutputStream(out)){if(in==null)throw new IOException("Datei nicht lesbar");byte[] buf=new byte[8192];int n;long total=0;while((n=in.read(buf))>0){total+=n;if(total>25L*1024*1024)throw new IOException("Datei größer als 25 MB");md.update(buf,0,n);os.write(buf,0,n);}}toast("Geteilte Datei lokal übernommen. Zuordnung zu Vorgängen folgt in einer späteren Ausbaustufe.");}catch(Exception e){toast("Datei konnte nicht sicher importiert werden.");}}
    private void writeBackup(Uri uri){try(OutputStream o=getContentResolver().openOutputStream(uri)){if(o==null)throw new IOException();o.write(db.exportProfile(profileId).toString(2).getBytes(StandardCharsets.UTF_8));toast("Backup exportiert.");}catch(Exception e){toast("Backup-Export fehlgeschlagen.");}}
    private void readBackup(Uri uri){try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[8192];int n;long total=0;while((n=in.read(x))>0){total+=n;if(total>5L*1024*1024)throw new IOException("zu groß");b.write(x,0,n);}db.importInto(profileId,new JSONObject(b.toString(StandardCharsets.UTF_8)));toast("Backup importiert.");overview();}catch(Exception e){toast("Backup ungültig oder beschädigt.");}}
    private void open(String url){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
    private TextView note(String text){TextView t=new TextView(this);t.setText(text);t.setTextSize(14);t.setPadding(0,dp(8),0,dp(12));body.addView(t);return t;}
    private boolean blank(TextInputEditText e){return s(e).isEmpty();}private String s(TextInputEditText e){return e.getText()==null?"":e.getText().toString().trim();}private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
