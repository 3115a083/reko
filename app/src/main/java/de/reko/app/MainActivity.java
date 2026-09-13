package de.reko.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
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
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private static final String BMF_SOURCE="https://lsth.bundesfinanzministerium.de/lsth/2026/tabellarische-Uebersicht/inhalt.html";
    private static final String[] RULE_KEYS={"travel_car_km","meal_over_8h","meal_24h"};
    private static final String[] RULE_LABELS={"Fahrtkosten Kraftwagen je km","Verpflegung > 8 Stunden / An- oder Abreisetag","Verpflegung 24 Stunden"};
    private static final String[] RULE_DEFAULTS={"0,30 €","14,00 €","28,00 €"};

    private RekoDb db;
    private DocumentStore documents;
    private BackupManager backups;
    private LinearLayout body;
    private TextView title;
    private MaterialButton profileButton;
    private BottomNavigationView nav;
    private long profileId;
    private File cameraFile;

    private ActivityResultLauncher<String> createBackup, calendarPermission;
    private ActivityResultLauncher<String[]> openBackup, openDocument, openIcs;
    private ActivityResultLauncher<Uri> takePicture;
    private ActivityResultLauncher<Intent> editDocument;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        db=new RekoDb(this);documents=new DocumentStore(this,db);backups=new BackupManager(this,db);
        setupLaunchers();profileId=db.activeProfile(this);buildShell();handleIntent(getIntent());
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}

    private void setupLaunchers(){
        createBackup=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"),uri->{if(uri!=null)writeBackup(uri);});
        openBackup=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{if(uri!=null)confirmRestore(uri);});
        openDocument=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{if(uri!=null)importDocument(uri,null);});
        openIcs=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{if(uri!=null)importIcs(uri);});
        takePicture=registerForActivityResult(new ActivityResultContracts.TakePicture(),ok->{if(ok)finishCameraImport();else cleanupCamera();});
        calendarPermission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),ok->{if(ok)chooseCalendars();else toast("Kalenderzugriff nicht erteilt.");});
        editDocument=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),r->{if(r.getResultCode()==RESULT_OK)showDocuments();});
    }

    private void buildShell(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(MaterialColors.getColor(root,com.google.android.material.R.attr.colorSurface));
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.VERTICAL);header.setPadding(dp(20),dp(16),dp(20),dp(10));
        title=new TextView(this);title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(-1,-2));
        profileButton=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);profileButton.setAllCaps(false);profileButton.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);profileButton.setIconResource(R.drawable.ic_profile);profileButton.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);profileButton.setOnClickListener(v->chooseProfile());
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.topMargin=dp(10);header.addView(profileButton,pp);root.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(6),dp(20),dp(24));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav=new BottomNavigationView(this);nav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);nav.getMenu().add(0,1,0,"Übersicht").setIcon(R.drawable.ic_overview);nav.getMenu().add(0,2,1,"Reise").setIcon(R.drawable.ic_trip);nav.getMenu().add(0,3,2,"Bewirtung").setIcon(R.drawable.ic_hospitality);nav.getMenu().add(0,4,3,"Einstellungen").setIcon(R.drawable.ic_settings);nav.setOnItemSelectedListener(i->{show(i.getItemId());return true;});root.addView(nav,new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);refreshProfileLabel();nav.setSelectedItemId(1);
    }

    private void show(int id){body.removeAllViews();if(id==1)overview();else if(id==2)tripForm();else if(id==3)hospitalityForm();else settings();}
    private void refreshProfileLabel(){profileButton.setText("Aktives Profil: "+db.profileName(profileId));}

    private void chooseProfile(){
        ArrayList<String[]> ps=db.profiles();String[] names=new String[ps.size()+1];for(int i=0;i<ps.size();i++)names[i]=ps.get(i)[1];names[ps.size()]="+ Neues Profil";
        new AlertDialog.Builder(this).setTitle("Profil wählen").setItems(names,(d,w)->{if(w==ps.size())newProfile();else{profileId=Long.parseLong(ps.get(w)[0]);db.setActiveProfile(this,profileId);refreshProfileLabel();nav.setSelectedItemId(1);}}).show();
    }
    private void newProfile(){EditText e=new EditText(this);e.setHint("Profilname");new AlertDialog.Builder(this).setTitle("Neues Profil").setView(e).setPositiveButton("Anlegen",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty()){profileId=db.addProfile(n);db.setActiveProfile(this,profileId);refreshProfileLabel();nav.setSelectedItemId(1);}}).setNegativeButton("Abbrechen",null).show();}
    private void manageProfile(){new AlertDialog.Builder(this).setTitle(db.profileName(profileId)).setItems(new String[]{"Umbenennen","Profil löschen","Neues Profil anlegen"},(d,w)->{if(w==0)renameProfile();else if(w==1)confirmDeleteProfile();else newProfile();}).show();}
    private void renameProfile(){EditText e=new EditText(this);e.setText(db.profileName(profileId));new AlertDialog.Builder(this).setTitle("Profil umbenennen").setView(e).setPositiveButton("Speichern",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty()){db.renameProfile(profileId,n);refreshProfileLabel();}}).setNegativeButton("Abbrechen",null).show();}
    private void confirmDeleteProfile(){
        if(db.profiles().size()<=1){toast("Das letzte Profil kann nicht gelöscht werden.");return;}
        new AlertDialog.Builder(this).setTitle("Profil endgültig löschen?").setMessage("Reisen, Bewirtungen, Belege, Kalenderauswahl und Regelwerte dieses Profils werden lokal gelöscht.").setPositiveButton("Löschen",(d,w)->{long old=profileId;if(db.deleteProfile(old)){deleteTree(new File(getFilesDir(),"documents/p"+old));ArrayList<String[]> ps=db.profiles();profileId=Long.parseLong(ps.get(0)[0]);db.setActiveProfile(this,profileId);refreshProfileLabel();nav.setSelectedItemId(1);}}).setNegativeButton("Abbrechen",null).show();
    }

    private TextInputEditText field(String hint){TextInputLayout l=new TextInputLayout(this);l.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);l.setHint(hint);TextInputEditText e=new TextInputEditText(l.getContext());l.addView(e,new LinearLayout.LayoutParams(-1,-2));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);body.addView(l,p);return e;}
    private MaterialButton button(String text){MaterialButton b=new MaterialButton(this);b.setText(text);b.setAllCaps(false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);body.addView(b,p);return b;}
    private MaterialButton textButton(String text,int icon,View.OnClickListener listener){MaterialButton b=new MaterialButton(this);b.setText(text);b.setAllCaps(false);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setIconResource(icon);b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));int primary=MaterialColors.getColor(b,com.google.android.material.R.attr.colorPrimary);b.setTextColor(primary);b.setIconTint(android.content.res.ColorStateList.valueOf(primary));b.setOnClickListener(listener);return b;}
    private String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.GERMANY).format(new Date());}

    private void tripForm(){
        title.setText("Reise");sectionIntro("Neue Reise","Reisedaten bleiben lokal auf diesem Gerät.",R.drawable.ic_trip);
        TextInputEditText start=field("Startdatum, YYYY-MM-DD");start.setText(today());TextInputEditText end=field("Enddatum, YYYY-MM-DD");end.setText(today());TextInputEditText dest=field("Ziel / Ort");TextInputEditText purpose=field("Beruflicher Anlass");TextInputEditText notes=field("Notizen, optional");
        MaterialButton info=button("Info: steuerliche Einordnung");info.setOnClickListener(v->infoDialog("Auswärtstätigkeit","Eine beruflich veranlasste Tätigkeit außerhalb der üblichen Tätigkeitsstätte kann Reisekosten auslösen.","Beispiel: Kundentermin an einem anderen Ort.","Nicht automatisch jede Fahrt zwischen Wohnung und erster Tätigkeitsstätte als Dienstreise behandeln."));
        MaterialButton save=button("Reise speichern");save.setIconResource(R.drawable.ic_save);save.setOnClickListener(v->{if(blank(dest)||blank(purpose)){toast("Ziel und Anlass fehlen.");return;}db.addTrip(profileId,s(start),s(end),s(dest),s(purpose),s(notes));toast("Reise gespeichert.");nav.setSelectedItemId(1);});
    }

    private void hospitalityForm(){
        title.setText("Bewirtung");sectionIntro("Bewirtung erfassen","Teilnehmer und geschäftlicher Anlass sind Pflichtangaben.",R.drawable.ic_hospitality);
        TextInputEditText date=field("Datum, YYYY-MM-DD");date.setText(today());TextInputEditText place=field("Ort / Betrieb");TextInputEditText amount=field("Betrag in EUR");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);TextInputEditText people=field("Teilnehmer");TextInputEditText purpose=field("Geschäftlicher Anlass");TextInputEditText tip=field("Trinkgeld in EUR, optional");tip.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        MaterialButton info=button("Info: Bewirtungsnachweis");info.setOnClickListener(v->infoDialog("Bewirtungsnachweis","Für eine geschäftliche Bewirtung sollten Anlass, Teilnehmer, Ort, Datum und Beleg nachvollziehbar dokumentiert sein.","Beispiel: Kundengespräch mit namentlich erfassten Teilnehmern und konkretem Anlass.","Nur „Geschäftsessen“ ohne Teilnehmer oder konkreten Anlass eintragen."));
        MaterialButton save=button("Bewirtung speichern");save.setIconResource(R.drawable.ic_save);save.setOnClickListener(v->{if(blank(place)||blank(amount)||blank(people)||blank(purpose)){toast("Bitte Pflichtfelder ergänzen.");return;}try{long cents=Math.round(Double.parseDouble(s(amount).replace(',','.'))*100);long tipC=s(tip).isEmpty()?0:Math.round(Double.parseDouble(s(tip).replace(',','.'))*100);db.addHospitality(profileId,s(date),s(place),cents,s(people),s(purpose),tipC);toast("Bewirtung gespeichert.");nav.setSelectedItemId(1);}catch(Exception ex){toast("Betrag ist ungültig.");}});
    }

    private void overview(){
        title.setText("Übersicht");sectionIntro("Deine Einträge","Reisen, Bewirtungen und Belege des aktiven Profils.",R.drawable.ic_overview);
        overviewAction("Belege & Dokumente",db.attachments(profileId).size()+" lokal gespeicherte Datei(en)",v->showDocuments());
        overviewAction("Kalendervorschläge","Aus ausgewählten Android-Kalendern Reisen vorschlagen",v->showCalendarSuggestions());
        overviewAction("ICS importieren","Kalenderdatei lokal lesen und Termine als Vorschläge verwenden",v->openIcs.launch(new String[]{"text/calendar","text/plain","application/octet-stream"}));
        ArrayList<String> items=db.summary(profileId);if(items.isEmpty())emptyState();for(String x:items)entryCard(x);
    }

    private void overviewAction(String heading,String detail,View.OnClickListener listener){MaterialCardView c=card();LinearLayout box=box();TextView h=heading(heading);box.addView(h);TextView d=text(detail);d.setPadding(0,dp(4),0,dp(8));box.addView(d);MaterialButton b=new MaterialButton(this);b.setText("Öffnen");b.setAllCaps(false);b.setOnClickListener(listener);box.addView(b);c.addView(box);addCard(c);}
    private void entryCard(String value){MaterialCardView c=card();TextView t=text(value);t.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);t.setPadding(dp(16),dp(16),dp(16),dp(16));c.addView(t);addCard(c);}

    private void showDocuments(){
        body.removeAllViews();title.setText("Belege & Dokumente");sectionIntro("Lokale Dokument-Inbox","PDF, JPEG und PNG bleiben lokal. Exakte Duplikate werden per SHA-256 erkannt.",R.drawable.ic_overview);
        MaterialButton camera=button("Beleg fotografieren");camera.setOnClickListener(v->startCamera());MaterialButton file=button("Bild oder PDF auswählen");file.setOnClickListener(v->openDocument.launch(new String[]{"application/pdf","image/jpeg","image/png"}));MaterialButton back=button("Zur Übersicht");back.setOnClickListener(v->nav.setSelectedItemId(1));
        ArrayList<String[]> list=db.attachments(profileId);if(list.isEmpty()){emptyText("Noch keine Belege gespeichert.");return;}
        for(String[] a:list)documentCard(a);
    }

    private void documentCard(String[] a){
        long id=Long.parseLong(a[0]);String name=a[1],mime=a[2],path=a[4],owner=a[6];
        MaterialCardView c=card();LinearLayout box=box();box.addView(heading(name));TextView meta=text(labelMime(mime)+" · "+humanBytes(Long.parseLong(a[5]))+" · "+ownerLabel(owner));meta.setPadding(0,dp(4),0,dp(6));box.addView(meta);
        MaterialButton assign=new MaterialButton(this);assign.setText("Zuordnen");assign.setAllCaps(false);assign.setOnClickListener(v->assignAttachment(id));box.addView(assign);
        if(mime.startsWith("image/")){MaterialButton edit=new MaterialButton(this);edit.setText("Drehen / zuschneiden");edit.setAllCaps(false);edit.setOnClickListener(v->editImage(a));box.addView(edit);}
        MaterialButton share=new MaterialButton(this);share.setText("Gezielt teilen / an Lexware weitergeben");share.setAllCaps(false);share.setOnClickListener(v->shareDocument(new File(path),mime));box.addView(share);
        MaterialButton del=new MaterialButton(this);del.setText("Beleg löschen");del.setAllCaps(false);del.setOnClickListener(v->confirmDeleteAttachment(id,new File(path)));box.addView(del);c.addView(box);addCard(c);
    }

    private void importDocument(Uri uri,String fallbackMime){
        try{DocumentStore.Result r=documents.importUri(profileId,uri,fallbackMime);if(r.duplicate){toast("Exaktes Duplikat erkannt. Datei wurde nicht erneut gespeichert.");return;}toast("Beleg lokal gespeichert.");assignAttachment(r.attachmentId);}catch(Exception e){toast("Datei konnte nicht sicher importiert werden.");}
    }
    private void assignAttachment(long attachmentId){
        ArrayList<String[]> trips=db.tripChoices(profileId), hosp=db.hospitalityChoices(profileId);ArrayList<String> labels=new ArrayList<>();ArrayList<String> types=new ArrayList<>();ArrayList<Long> ids=new ArrayList<>();labels.add("Dokument-Inbox, noch nicht zugeordnet");types.add("inbox");ids.add(0L);
        for(String[] x:trips){labels.add("Reise · "+x[1]);types.add("trip");ids.add(Long.parseLong(x[0]));}for(String[] x:hosp){labels.add("Bewirtung · "+x[1]);types.add("hospitality");ids.add(Long.parseLong(x[0]));}
        new AlertDialog.Builder(this).setTitle("Beleg zuordnen").setItems(labels.toArray(new String[0]),(d,w)->{db.assignAttachment(attachmentId,profileId,types.get(w),ids.get(w));toast("Zuordnung gespeichert.");showDocuments();}).show();
    }
    private void editImage(String[] a){Intent i=new Intent(this,DocumentEditorActivity.class);i.putExtra("profileId",profileId);i.putExtra("path",a[4]);i.putExtra("name",a[1]);i.putExtra("mime",a[2]);editDocument.launch(i);}
    private void confirmDeleteAttachment(long id,File file){new AlertDialog.Builder(this).setTitle("Beleg löschen?").setMessage("Die lokal gespeicherte Datei wird endgültig entfernt.").setPositiveButton("Löschen",(d,w)->{db.deleteAttachment(id,profileId);file.delete();showDocuments();}).setNegativeButton("Abbrechen",null).show();}
    private void shareDocument(File file,String mime){try{String base=getFilesDir().getCanonicalPath()+File.separator;if(!file.getCanonicalPath().startsWith(base))throw new SecurityException();Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",file);Intent i=new Intent(Intent.ACTION_SEND);i.setType(mime);i.putExtra(Intent.EXTRA_STREAM,uri);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"Beleg gezielt teilen"));}catch(Exception e){toast("Beleg kann nicht geteilt werden.");}}

    private void startCamera(){
        try{File dir=new File(getCacheDir(),"camera");if(!dir.exists()&&!dir.mkdirs())throw new IOException();cameraFile=new File(dir,"capture-"+System.currentTimeMillis()+".jpg");Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",cameraFile);takePicture.launch(uri);}catch(Exception e){toast("Kamera konnte nicht gestartet werden.");}
    }
    private void finishCameraImport(){try{DocumentStore.Result r=documents.importFile(profileId,cameraFile,"foto-"+System.currentTimeMillis()+".jpg","image/jpeg");if(r.duplicate)toast("Exaktes Duplikat erkannt.");else assignAttachment(r.attachmentId);}catch(Exception e){toast("Foto konnte nicht gespeichert werden.");}finally{cleanupCamera();}}
    private void cleanupCamera(){if(cameraFile!=null)cameraFile.delete();cameraFile=null;}

    private void requestCalendar(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED)chooseCalendars();else calendarPermission.launch(Manifest.permission.READ_CALENDAR);}
    private void chooseCalendars(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED){requestCalendar();return;}
        ArrayList<Long> ids=new ArrayList<>();ArrayList<String> names=new ArrayList<>();
        try(Cursor c=getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,new String[]{CalendarContract.Calendars._ID,CalendarContract.Calendars.CALENDAR_DISPLAY_NAME},CalendarContract.Calendars.VISIBLE+"=1",null,CalendarContract.Calendars.CALENDAR_DISPLAY_NAME+" COLLATE NOCASE")){if(c!=null)while(c.moveToNext()&&ids.size()<100){ids.add(c.getLong(0));names.add(c.getString(1)==null?"Kalender "+c.getLong(0):c.getString(1));}}
        catch(SecurityException e){toast("Kalenderzugriff ist nicht verfügbar.");return;}
        if(ids.isEmpty()){toast("Keine sichtbaren Kalender gefunden.");return;}Set<Long> selected=db.calendarIds(profileId);boolean[] checked=new boolean[ids.size()];for(int i=0;i<ids.size();i++)checked[i]=selected.contains(ids.get(i));
        new AlertDialog.Builder(this).setTitle("Kalender für Reisevorschläge").setMultiChoiceItems(names.toArray(new String[0]),checked,(d,w,on)->checked[w]=on).setPositiveButton("Speichern",(d,w)->{ArrayList<Long> saveIds=new ArrayList<>();ArrayList<String> saveNames=new ArrayList<>();for(int i=0;i<checked.length;i++)if(checked[i]){saveIds.add(ids.get(i));saveNames.add(names.get(i));}db.setCalendars(profileId,saveIds,saveNames);toast("Kalenderauswahl gespeichert.");}).setNegativeButton("Abbrechen",null).show();
    }

    private void showCalendarSuggestions(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED){new AlertDialog.Builder(this).setTitle("Kalenderzugriff").setMessage("ReKo benötigt READ_CALENDAR technisch für den Android-Kalenderanbieter. Fachlich werden anschließend nur die von dir ausgewählten Kalender ausgewertet.").setPositiveButton("Aktivieren",(d,w)->requestCalendar()).setNegativeButton("Abbrechen",null).show();return;}
        Set<Long> allowed=db.calendarIds(profileId);if(allowed.isEmpty()){chooseCalendars();return;}
        StringBuilder in=new StringBuilder();ArrayList<String> args=new ArrayList<>();for(long id:allowed){if(in.length()>0)in.append(',');in.append('?');args.add(String.valueOf(id));}long now=System.currentTimeMillis(),until=now+180L*24*60*60*1000;args.add(String.valueOf(now));args.add(String.valueOf(until));
        String sel=CalendarContract.Events.CALENDAR_ID+" IN ("+in+") AND "+CalendarContract.Events.DTSTART+">=? AND "+CalendarContract.Events.DTSTART+"<=?";ArrayList<CalendarEvent> events=new ArrayList<>();
        try(Cursor c=getContentResolver().query(CalendarContract.Events.CONTENT_URI,new String[]{CalendarContract.Events.DTSTART,CalendarContract.Events.DTEND,CalendarContract.Events.TITLE,CalendarContract.Events.EVENT_LOCATION},sel,args.toArray(new String[0]),CalendarContract.Events.DTSTART+" ASC")){if(c!=null)while(c.moveToNext()&&events.size()<100)events.add(new CalendarEvent(date(c.getLong(0)),date(c.isNull(1)?c.getLong(0):c.getLong(1)),safe(c.getString(2)),safe(c.getString(3))));}
        catch(SecurityException e){toast("Kalenderzugriff ist nicht verfügbar.");return;}if(events.isEmpty()){toast("Keine passenden Termine in den nächsten 180 Tagen gefunden.");return;}showEventChoices("Kalendervorschläge",events);
    }

    private void importIcs(Uri uri){
        try{validateExternalUri(uri);ArrayList<IcsParser.Event> parsed;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();parsed=IcsParser.parse(in,2L*1024*1024);}if(parsed.isEmpty()){toast("Keine Termine in der ICS-Datei gefunden.");return;}ArrayList<CalendarEvent> events=new ArrayList<>();for(IcsParser.Event e:parsed)events.add(new CalendarEvent(e.start,e.end,e.summary,e.location));showEventChoices("ICS-Reisevorschläge",events);}catch(Exception e){toast("ICS-Datei ist ungültig oder konnte nicht sicher gelesen werden.");}
    }
    private void showEventChoices(String heading,ArrayList<CalendarEvent> events){String[] labels=new String[events.size()];for(int i=0;i<events.size();i++)labels[i]=events.get(i).label();new AlertDialog.Builder(this).setTitle(heading).setItems(labels,(d,w)->confirmTripSuggestion(events.get(w))).setNegativeButton("Schließen",null).show();}
    private void confirmTripSuggestion(CalendarEvent e){new AlertDialog.Builder(this).setTitle("Als Reise übernehmen?").setMessage(e.start+" bis "+e.end+"\n"+(e.location.isEmpty()?"Ohne Ort":e.location)+"\n"+e.title+"\n\nDer Termin wird nur nach dieser Bestätigung als Reise gespeichert.").setPositiveButton("Reise anlegen",(d,w)->{String dest=e.location.isEmpty()?"Kalendertermin":e.location;String purpose=e.title.isEmpty()?"Kalendertermin":e.title;db.addTrip(profileId,e.start,e.end,dest,purpose,"Aus Kalender/ICS als Vorschlag übernommen");toast("Reise gespeichert.");nav.setSelectedItemId(1);}).setNegativeButton("Abbrechen",null).show();}

    private void showRules(){
        body.removeAllViews();title.setText("Rechenwerte");sectionIntro("Versionierte Basiswerte","Standardwert, persönlicher Override, Quelle und Gültigkeit bleiben getrennt sichtbar.",R.drawable.ic_settings);Map<String,String> overrides=db.ruleOverrides(profileId);
        for(int i=0;i<RULE_KEYS.length;i++){final int ix=i;MaterialCardView c=card();LinearLayout box=box();box.addView(heading(RULE_LABELS[i]));String current=overrides.get(RULE_KEYS[i]);TextView v=text("Standard: "+RULE_DEFAULTS[i]+"\nPersönlicher Wert: "+(current==null?"Standard verwenden":current)+"\nQuelle: BMF/Lohnsteuer-Handbuch 2026\nGültigkeit: 2026, Stand App 0.2.0");v.setPadding(0,dp(6),0,dp(8));box.addView(v);MaterialButton edit=new MaterialButton(this);edit.setText("Persönlichen Wert setzen");edit.setAllCaps(false);edit.setOnClickListener(x->editRule(ix));box.addView(edit);MaterialButton reset=new MaterialButton(this);reset.setText("Auf Standard zurücksetzen");reset.setAllCaps(false);reset.setOnClickListener(x->{db.clearRuleOverride(profileId,RULE_KEYS[ix]);showRules();});box.addView(reset);c.addView(box);addCard(c);}
        MaterialButton source=button("Amtliche Quelle öffnen");source.setOnClickListener(v->open(BMF_SOURCE));MaterialButton back=button("Zurück zu Einstellungen");back.setOnClickListener(v->nav.setSelectedItemId(4));
    }
    private void editRule(int i){EditText e=new EditText(this);e.setHint("z. B. 0,30 €");String current=db.ruleOverrides(profileId).get(RULE_KEYS[i]);if(current!=null)e.setText(current);new AlertDialog.Builder(this).setTitle(RULE_LABELS[i]).setMessage("Der persönliche Wert überschreibt nur für dieses Profil den Standard. Er wird bei späteren Regelupdates nicht still ersetzt.").setView(e).setPositiveButton("Speichern",(d,w)->{String value=e.getText().toString().trim();if(!value.isEmpty())db.setRuleOverride(profileId,RULE_KEYS[i],value);showRules();}).setNegativeButton("Abbrechen",null).show();}

    private void settings(){
        title.setText("Einstellungen");
        settingsCard("Profil & Daten",new String[]{"Aktives Profil verwalten","Belege & Dokumente","Profil-Backup als ZIP exportieren","ZIP-Backup in aktives Profil importieren"},new View.OnClickListener[]{v->manageProfile(),v->showDocuments(),v->createBackup.launch("reko-profil-"+System.currentTimeMillis()+".zip"),v->openBackup.launch(new String[]{"application/zip","application/octet-stream"})});
        settingsCard("Kalender",new String[]{"Android-Kalender auswählen","ICS-Datei importieren","Reisevorschläge anzeigen"},new View.OnClickListener[]{v->requestCalendar(),v->openIcs.launch(new String[]{"text/calendar","text/plain","application/octet-stream"}),v->showCalendarSuggestions()});
        settingsCard("Rechenwerte & Hilfe",new String[]{"Versionierte Rechenwerte","Info: Aufbewahrung & Originalformat"},new View.OnClickListener[]{v->showRules(),v->infoDialog("Aufbewahrung & Originalformat","ReKo bewahrt importierte Originaldateien unverändert auf und speichert Bearbeitungen zusätzlich. Aufbewahrungsfristen und steuerliche Nachweisanforderungen können vom Einzelfall abhängen.","Beispiel: Original-PDF behalten und eine zugeschnittene Bildkopie zusätzlich speichern.","Originaldatei nach einer Bildbearbeitung löschen und nur die bearbeitete Version behalten.")});
        settingsCard("Datenschutz",new String[]{"Local-first. Keine Telemetrie. Keine zentrale ReKo-Datenspeicherung. Android-Systembackup ist deaktiviert.","OCR oder externe Dokumentenanalyse wird nicht automatisch durchgeführt.","ReKo ersetzt keine Rechts- oder Steuerberatung. Keine Gewähr für Vollständigkeit, Aktualität oder steuerliche Anerkennung."},null);
        Space spacer=new Space(this);body.addView(spacer,new LinearLayout.LayoutParams(1,dp(24)));MaterialDivider divider=new MaterialDivider(this);body.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));TextView footerTitle=new TextView(this);footerTitle.setText("ReKo");footerTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);footerTitle.setPadding(0,dp(18),0,dp(4));body.addView(footerTitle);body.addView(textButton("GitHub Repository",R.drawable.ic_github,v->open("https://github.com/3115a083/reko")),new LinearLayout.LayoutParams(-1,-2));body.addView(textButton("Nach Updates suchen",R.drawable.ic_update,v->open("https://github.com/3115a083/reko/releases")),new LinearLayout.LayoutParams(-1,-2));TextView f=new TextView(this);f.setText("Vibecoded with ❤️");f.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);f.setGravity(Gravity.CENTER);f.setPadding(0,dp(12),0,dp(4));body.addView(f,new LinearLayout.LayoutParams(-1,-2));
    }

    private void writeBackup(Uri uri){try{backups.exportProfile(profileId,uri);toast("Vollständiges Profil-Backup exportiert.");}catch(Exception e){toast("Backup-Export fehlgeschlagen.");}}
    private void confirmRestore(Uri uri){new AlertDialog.Builder(this).setTitle("Backup importieren?").setMessage("Daten und Belege werden dem aktiven Profil hinzugefügt. Bestehende Daten werden nicht still überschrieben; exakte Belegduplikate werden übersprungen.").setPositiveButton("Importieren",(d,w)->readBackup(uri)).setNegativeButton("Abbrechen",null).show();}
    private void readBackup(Uri uri){try{validateExternalUri(uri);int files=backups.importProfile(profileId,uri);toast("Backup importiert. "+files+" neue Belegdatei(en) übernommen.");nav.setSelectedItemId(1);}catch(Exception e){toast("Backup ungültig, beschädigt oder zu groß.");}}

    private void handleIntent(Intent i){String a=i.getAction();if("de.reko.app.NEW_TRIP".equals(a)){nav.setSelectedItemId(2);return;}if("de.reko.app.NEW_HOSPITALITY".equals(a)){nav.setSelectedItemId(3);return;}if(Intent.ACTION_SEND.equals(a)){Uri u=i.getParcelableExtra(Intent.EXTRA_STREAM);if(u!=null)importDocument(u,i.getType());}}

    private void validateExternalUri(Uri uri)throws Exception{if(uri==null||!"content".equalsIgnoreCase(uri.getScheme()))throw new SecurityException();String authority=uri.getAuthority();if(authority==null||authority.isBlank())throw new SecurityException();String own=getPackageName();if(authority.equals(own)||authority.startsWith(own+"."))throw new SecurityException();String p=uri.getPath();if(p==null)throw new SecurityException();Path normalized=FileSystems.getDefault().getPath(p).normalize();if(normalized.startsWith("/data")||normalized.startsWith("/proc")||normalized.startsWith("/sys")||normalized.startsWith("/dev"))throw new SecurityException();}

    private void settingsCard(String heading,String[] rows,View.OnClickListener[] listeners){MaterialCardView card=card();LinearLayout box=box();box.addView(heading(heading));for(int i=0;i<rows.length;i++){if(listeners!=null){MaterialButton b=new MaterialButton(this);b.setText(rows[i]);b.setAllCaps(false);b.setGravity(Gravity.START);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));b.setTextColor(MaterialColors.getColor(b,com.google.android.material.R.attr.colorPrimary));b.setOnClickListener(listeners[i]);box.addView(b,new LinearLayout.LayoutParams(-1,-2));}else{TextView t=text(rows[i]);t.setPadding(0,dp(10),0,0);box.addView(t);}}card.addView(box);addCard(card);}
    private void sectionIntro(String heading,String text,int icon){MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(20));card.setCardElevation(0);card.setCardBackgroundColor(MaterialColors.getColor(card,com.google.android.material.R.attr.colorSecondaryContainer));LinearLayout row=new LinearLayout(this);row.setPadding(dp(16),dp(14),dp(16),dp(14));row.setGravity(Gravity.CENTER_VERTICAL);ImageView iv=new ImageView(this);iv.setImageResource(icon);iv.setImageTintList(android.content.res.ColorStateList.valueOf(MaterialColors.getColor(iv,com.google.android.material.R.attr.colorOnSecondaryContainer)));row.addView(iv,new LinearLayout.LayoutParams(dp(28),dp(28)));LinearLayout texts=new LinearLayout(this);texts.setOrientation(LinearLayout.VERTICAL);texts.setPadding(dp(14),0,0,0);TextView h=heading(heading);TextView d=text(text);texts.addView(h);texts.addView(d);row.addView(texts,new LinearLayout.LayoutParams(0,-2,1));card.addView(row);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(18);body.addView(card,p);}
    private MaterialCardView card(){MaterialCardView c=new MaterialCardView(this);c.setRadius(dp(20));c.setCardElevation(0);c.setStrokeWidth(dp(1));c.setStrokeColor(MaterialColors.getColor(c,com.google.android.material.R.attr.colorOutlineVariant));return c;}
    private LinearLayout box(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(16),dp(14),dp(16),dp(14));return b;}
    private TextView heading(String value){TextView h=new TextView(this);h.setText(value);h.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);return h;}
    private TextView text(String value){TextView t=new TextView(this);t.setText(value);t.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);return t;}
    private void addCard(MaterialCardView c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);body.addView(c,p);}
    private void emptyState(){emptyText("Noch keine Reisen oder Bewirtungen. Erfasse den ersten Vorgang über die Navigation unten.");}
    private void emptyText(String value){MaterialCardView c=card();TextView t=text(value);t.setGravity(Gravity.CENTER);t.setPadding(dp(24),dp(28),dp(24),dp(28));c.addView(t);addCard(c);}
    private void infoDialog(String title,String explanation,String right,String wrong){new AlertDialog.Builder(this).setTitle(title).setMessage(explanation+"\n\nRichtiges Beispiel:\n"+right+"\n\nTypisches Missverständnis:\n"+wrong).setPositiveButton("Verstanden",null).show();}

    private String ownerLabel(String owner){if("trip".equals(owner))return "Reise zugeordnet";if("hospitality".equals(owner))return "Bewirtung zugeordnet";return "Inbox";}
    private String labelMime(String mime){if("application/pdf".equals(mime))return "PDF";if("image/png".equals(mime))return "PNG";return "JPEG";}
    private String humanBytes(long n){if(n<1024)return n+" B";if(n<1024*1024)return String.format(Locale.GERMANY,"%.1f KB",n/1024.0);return String.format(Locale.GERMANY,"%.1f MB",n/(1024.0*1024));}
    private String date(long millis){return new SimpleDateFormat("yyyy-MM-dd",Locale.GERMANY).format(new Date(millis));}
    private static String safe(String s){return s==null?"":s.trim();}
    private void open(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){toast("Link kann nicht geöffnet werden.");}}
    private boolean blank(TextInputEditText e){return s(e).isEmpty();}
    private String s(TextInputEditText e){return e.getText()==null?"":e.getText().toString().trim();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private static void deleteTree(File f){if(f==null)return;if(f.isDirectory()){File[] c=f.listFiles();if(c!=null)for(File x:c)deleteTree(x);}f.delete();}

    private static final class CalendarEvent{
        final String start,end,title,location;
        CalendarEvent(String start,String end,String title,String location){this.start=start;this.end=end;this.title=title;this.location=location;}
        String label(){return start+" · "+(location.isEmpty()?title:location+" · "+title);}
    }
}
