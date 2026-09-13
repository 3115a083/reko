package de.reko.app;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.util.*;

final class RekoDb extends SQLiteOpenHelper {
    private static final String DB = "reko.db";
    private static final int VERSION = 2;

    RekoDb(Context c) { super(c, DB, null, VERSION); }

    @Override public void onConfigure(SQLiteDatabase db){ super.onConfigure(db); db.setForeignKeyConstraintsEnabled(true); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE profiles(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE trips(id INTEGER PRIMARY KEY AUTOINCREMENT,profile_id INTEGER NOT NULL,start_date TEXT NOT NULL,end_date TEXT NOT NULL,destination TEXT NOT NULL,purpose TEXT NOT NULL,notes TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,FOREIGN KEY(profile_id) REFERENCES profiles(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE hospitality(id INTEGER PRIMARY KEY AUTOINCREMENT,profile_id INTEGER NOT NULL,date TEXT NOT NULL,place TEXT NOT NULL,amount_cents INTEGER NOT NULL,participants TEXT NOT NULL,purpose TEXT NOT NULL,tip_cents INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,FOREIGN KEY(profile_id) REFERENCES profiles(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE attachments(id INTEGER PRIMARY KEY AUTOINCREMENT,profile_id INTEGER NOT NULL,name TEXT NOT NULL,mime TEXT NOT NULL,sha256 TEXT NOT NULL,path TEXT NOT NULL,owner_type TEXT NOT NULL DEFAULT 'inbox',owner_id INTEGER NOT NULL DEFAULT 0,size_bytes INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,FOREIGN KEY(profile_id) REFERENCES profiles(id) ON DELETE CASCADE)");
        db.execSQL("CREATE UNIQUE INDEX idx_attachment_hash ON attachments(profile_id,sha256)");
        db.execSQL("CREATE TABLE calendar_allowlist(profile_id INTEGER NOT NULL,calendar_id INTEGER NOT NULL,display_name TEXT NOT NULL DEFAULT '',PRIMARY KEY(profile_id,calendar_id),FOREIGN KEY(profile_id) REFERENCES profiles(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE rule_values(profile_id INTEGER NOT NULL,rule_key TEXT NOT NULL,override_value TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(profile_id,rule_key),FOREIGN KEY(profile_id) REFERENCES profiles(id) ON DELETE CASCADE)");
        ContentValues v = new ContentValues(); v.put("name","Standard"); v.put("created_at",System.currentTimeMillis()); db.insert("profiles",null,v);
    }

    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
        if(oldVersion < 2){
            db.execSQL("ALTER TABLE attachments ADD COLUMN owner_type TEXT NOT NULL DEFAULT 'inbox'");
            db.execSQL("ALTER TABLE attachments ADD COLUMN owner_id INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE attachments ADD COLUMN size_bytes INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_attachment_hash ON attachments(profile_id,sha256)");
            db.execSQL("CREATE TABLE IF NOT EXISTS calendar_allowlist(profile_id INTEGER NOT NULL,calendar_id INTEGER NOT NULL,display_name TEXT NOT NULL DEFAULT '',PRIMARY KEY(profile_id,calendar_id),FOREIGN KEY(profile_id) REFERENCES profiles(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS rule_values(profile_id INTEGER NOT NULL,rule_key TEXT NOT NULL,override_value TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(profile_id,rule_key),FOREIGN KEY(profile_id) REFERENCES profiles(id) ON DELETE CASCADE)");
        }
    }

    long activeProfile(Context c){
        long id=c.getSharedPreferences("reko",0).getLong("profile",1);
        try(Cursor x=getReadableDatabase().rawQuery("SELECT id FROM profiles WHERE id=?",new String[]{String.valueOf(id)})){ if(x.moveToFirst()) return id; }
        try(Cursor x=getReadableDatabase().rawQuery("SELECT id FROM profiles ORDER BY id LIMIT 1",null)){ if(x.moveToFirst()){ long fallback=x.getLong(0); setActiveProfile(c,fallback); return fallback; } }
        ContentValues v=new ContentValues();v.put("name","Standard");v.put("created_at",System.currentTimeMillis());long fallback=getWritableDatabase().insertOrThrow("profiles",null,v);setActiveProfile(c,fallback);return fallback;
    }
    void setActiveProfile(Context c,long id){ c.getSharedPreferences("reko",0).edit().putLong("profile",id).apply(); }
    ArrayList<String[]> profiles(){ ArrayList<String[]> r=new ArrayList<>(); try(Cursor c=getReadableDatabase().rawQuery("SELECT id,name FROM profiles ORDER BY id",null)){ while(c.moveToNext()) r.add(new String[]{c.getString(0),c.getString(1)}); } return r; }
    String profileName(long id){try(Cursor c=getReadableDatabase().rawQuery("SELECT name FROM profiles WHERE id=?",new String[]{String.valueOf(id)})){return c.moveToFirst()?c.getString(0):"Profil";}}
    long addProfile(String name){ ContentValues v=new ContentValues();v.put("name",name.trim());v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insertOrThrow("profiles",null,v); }
    void renameProfile(long id,String name){ContentValues v=new ContentValues();v.put("name",name.trim());getWritableDatabase().update("profiles",v,"id=?",new String[]{String.valueOf(id)});}
    boolean deleteProfile(long id){ if(profiles().size()<=1) return false; return getWritableDatabase().delete("profiles","id=?",new String[]{String.valueOf(id)})>0; }

    long addTrip(long p,String start,String end,String dest,String purpose,String notes){ ContentValues v=new ContentValues();v.put("profile_id",p);v.put("start_date",start);v.put("end_date",end);v.put("destination",dest.trim());v.put("purpose",purpose.trim());v.put("notes",notes.trim());v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insertOrThrow("trips",null,v); }
    long addHospitality(long p,String date,String place,long cents,String people,String purpose,long tip){ ContentValues v=new ContentValues();v.put("profile_id",p);v.put("date",date);v.put("place",place.trim());v.put("amount_cents",cents);v.put("participants",people.trim());v.put("purpose",purpose.trim());v.put("tip_cents",tip);v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insertOrThrow("hospitality",null,v); }

    ArrayList<String[]> tripChoices(long p){ArrayList<String[]> r=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,start_date,destination FROM trips WHERE profile_id=? ORDER BY id DESC LIMIT 50",new String[]{String.valueOf(p)})){while(c.moveToNext())r.add(new String[]{c.getString(0),c.getString(1)+" · "+c.getString(2)});}return r;}
    ArrayList<String[]> hospitalityChoices(long p){ArrayList<String[]> r=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,date,place FROM hospitality WHERE profile_id=? ORDER BY id DESC LIMIT 50",new String[]{String.valueOf(p)})){while(c.moveToNext())r.add(new String[]{c.getString(0),c.getString(1)+" · "+c.getString(2)});}return r;}

    ArrayList<String> summary(long p){ ArrayList<String> r=new ArrayList<>(); try(Cursor c=getReadableDatabase().rawQuery("SELECT start_date,destination,purpose FROM trips WHERE profile_id=? ORDER BY id DESC LIMIT 30",new String[]{String.valueOf(p)})){ while(c.moveToNext()) r.add("Reise · "+c.getString(0)+" · "+c.getString(1)+"\n"+c.getString(2)); } try(Cursor c=getReadableDatabase().rawQuery("SELECT date,place,amount_cents,purpose FROM hospitality WHERE profile_id=? ORDER BY id DESC LIMIT 30",new String[]{String.valueOf(p)})){ while(c.moveToNext()) r.add("Bewirtung · "+c.getString(0)+" · "+c.getString(1)+" · "+String.format(Locale.GERMANY,"%.2f €",c.getLong(2)/100.0)+"\n"+c.getString(3)); } return r; }

    long addAttachment(long p,String name,String mime,String sha,String path,long size){ContentValues v=new ContentValues();v.put("profile_id",p);v.put("name",name);v.put("mime",mime);v.put("sha256",sha);v.put("path",path);v.put("size_bytes",size);v.put("owner_type","inbox");v.put("owner_id",0);v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insertOrThrow("attachments",null,v);}
    boolean hasAttachmentHash(long p,String sha){try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM attachments WHERE profile_id=? AND sha256=? LIMIT 1",new String[]{String.valueOf(p),sha})){return c.moveToFirst();}}
    void assignAttachment(long id,long p,String ownerType,long ownerId){ContentValues v=new ContentValues();v.put("owner_type",ownerType);v.put("owner_id",ownerId);getWritableDatabase().update("attachments",v,"id=? AND profile_id=?",new String[]{String.valueOf(id),String.valueOf(p)});}
    ArrayList<String[]> attachments(long p){ArrayList<String[]> r=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,name,mime,sha256,path,size_bytes,owner_type,owner_id FROM attachments WHERE profile_id=? ORDER BY id DESC",new String[]{String.valueOf(p)})){while(c.moveToNext())r.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7)});}return r;}
    void deleteAttachment(long id,long p){getWritableDatabase().delete("attachments","id=? AND profile_id=?",new String[]{String.valueOf(id),String.valueOf(p)});}

    Set<Long> calendarIds(long p){Set<Long> s=new HashSet<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT calendar_id FROM calendar_allowlist WHERE profile_id=?",new String[]{String.valueOf(p)})){while(c.moveToNext())s.add(c.getLong(0));}return s;}
    void setCalendars(long p,List<Long> ids,List<String> names){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{d.delete("calendar_allowlist","profile_id=?",new String[]{String.valueOf(p)});for(int i=0;i<ids.size();i++){ContentValues v=new ContentValues();v.put("profile_id",p);v.put("calendar_id",ids.get(i));v.put("display_name",names.get(i));d.insertOrThrow("calendar_allowlist",null,v);}d.setTransactionSuccessful();}finally{d.endTransaction();}}

    Map<String,String> ruleOverrides(long p){Map<String,String> m=new LinkedHashMap<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT rule_key,override_value FROM rule_values WHERE profile_id=? ORDER BY rule_key",new String[]{String.valueOf(p)})){while(c.moveToNext())m.put(c.getString(0),c.getString(1));}return m;}
    void setRuleOverride(long p,String key,String value){ContentValues v=new ContentValues();v.put("profile_id",p);v.put("rule_key",key);v.put("override_value",value.trim());v.put("updated_at",System.currentTimeMillis());getWritableDatabase().insertWithOnConflict("rule_values",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    void clearRuleOverride(long p,String key){getWritableDatabase().delete("rule_values","profile_id=? AND rule_key=?",new String[]{String.valueOf(p),key});}

    JSONObject exportProfile(long p) throws JSONException {
        JSONObject root=new JSONObject();root.put("format","reko-backup");root.put("version",2);root.put("profileName",profileName(p));root.put("exportedAt",System.currentTimeMillis());
        root.put("trips",queryJson("SELECT start_date,end_date,destination,purpose,notes,created_at FROM trips WHERE profile_id=?",p,new String[]{"start_date","end_date","destination","purpose","notes","created_at"}));
        root.put("hospitality",queryJson("SELECT date,place,amount_cents,participants,purpose,tip_cents,created_at FROM hospitality WHERE profile_id=?",p,new String[]{"date","place","amount_cents","participants","purpose","tip_cents","created_at"}));
        root.put("attachments",queryJson("SELECT name,mime,sha256,path,size_bytes,owner_type,owner_id,created_at FROM attachments WHERE profile_id=?",p,new String[]{"name","mime","sha256","path","size_bytes","owner_type","owner_id","created_at"}));
        root.put("calendars",queryJson("SELECT calendar_id,display_name FROM calendar_allowlist WHERE profile_id=?",p,new String[]{"calendar_id","display_name"}));
        root.put("rules",queryJson("SELECT rule_key,override_value,updated_at FROM rule_values WHERE profile_id=?",p,new String[]{"rule_key","override_value","updated_at"}));
        return root;
    }
    private JSONArray queryJson(String sql,long p,String[] cols) throws JSONException { JSONArray a=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(p)})){while(c.moveToNext()){JSONObject o=new JSONObject();for(int i=0;i<cols.length;i++){if(c.getType(i)==Cursor.FIELD_TYPE_INTEGER)o.put(cols[i],c.getLong(i));else o.put(cols[i],c.getString(i));}a.put(o);}}return a;}

    void importInto(long p,JSONObject root) throws JSONException {
        if(!"reko-backup".equals(root.optString("format"))) throw new JSONException("Ungültiges Backup");
        int version=root.optInt("version"); if(version<1 || version>2) throw new JSONException("Nicht unterstützte Backup-Version");
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
            JSONArray t=root.optJSONArray("trips");if(t!=null)for(int i=0;i<t.length();i++){JSONObject o=t.getJSONObject(i);addTrip(p,o.getString("start_date"),o.getString("end_date"),o.getString("destination"),o.getString("purpose"),o.optString("notes"));}
            JSONArray h=root.optJSONArray("hospitality");if(h!=null)for(int i=0;i<h.length();i++){JSONObject o=h.getJSONObject(i);addHospitality(p,o.getString("date"),o.getString("place"),o.getLong("amount_cents"),o.getString("participants"),o.getString("purpose"),o.optLong("tip_cents"));}
            JSONArray rules=root.optJSONArray("rules");if(rules!=null)for(int i=0;i<rules.length();i++){JSONObject o=rules.getJSONObject(i);setRuleOverride(p,o.getString("rule_key"),o.getString("override_value"));}
            d.setTransactionSuccessful();
        }finally{d.endTransaction();}
    }
}
