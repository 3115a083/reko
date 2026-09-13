package de.reko.app;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

final class DocumentStore {
    static final long MAX_BYTES = 25L * 1024 * 1024;
    private final Context context;
    private final RekoDb db;

    static final class Result {
        final boolean duplicate;
        final long attachmentId;
        final String name;
        Result(boolean duplicate,long attachmentId,String name){this.duplicate=duplicate;this.attachmentId=attachmentId;this.name=name;}
    }

    DocumentStore(Context context,RekoDb db){this.context=context.getApplicationContext();this.db=db;}

    Result importUri(long profileId,Uri uri,String fallbackMime) throws Exception {
        validateExternalContentUri(uri);
        String mime=context.getContentResolver().getType(uri); if(mime==null)mime=fallbackMime;
        String name=displayName(uri);
        try(InputStream in=context.getContentResolver().openInputStream(uri)){
            if(in==null) throw new IOException("Datei nicht lesbar");
            return importStream(profileId,in,name,mime);
        }
    }

    Result importFile(long profileId,File file,String name,String mime) throws Exception {
        if(file==null || !file.isFile() || file.length()>MAX_BYTES) throw new IOException("Ungültige Datei");
        String canonical=file.getCanonicalPath();String files=context.getFilesDir().getCanonicalPath();String cache=context.getCacheDir().getCanonicalPath();
        if(!(canonical.startsWith(files+File.separator)||canonical.startsWith(cache+File.separator)))throw new SecurityException("Datei außerhalb des App-Speichers");
        try(InputStream in=new FileInputStream(file)){return importStream(profileId,in,name,mime);}
    }

    private void validateExternalContentUri(Uri uri)throws Exception{
        if(uri==null || !"content".equalsIgnoreCase(uri.getScheme())) throw new SecurityException("Nur content-URIs erlaubt");
        String authority=uri.getAuthority();if(authority==null||authority.isBlank())throw new SecurityException("Fehlende URI-Autorität");
        String own=context.getPackageName();if(authority.equals(own)||authority.startsWith(own+"."))throw new SecurityException("Eigene Provider nicht als externe Quelle erlaubt");
        String path=uri.getPath();if(path==null)throw new SecurityException("Fehlender URI-Pfad");
        Path normalized=FileSystems.getDefault().getPath(path).normalize();
        if(normalized.startsWith("/data")||normalized.startsWith("/proc")||normalized.startsWith("/sys")||normalized.startsWith("/dev"))throw new SecurityException("Privater Systempfad nicht erlaubt");
    }

    private Result importStream(long profileId,InputStream raw,String name,String declaredMime) throws Exception {
        BufferedInputStream in=new BufferedInputStream(raw); in.mark(16);
        byte[] header=new byte[8]; int headerLen=in.read(header); in.reset();
        String mime=sniff(header,headerLen,declaredMime);
        String ext="application/pdf".equals(mime)?".pdf":"image/png".equals(mime)?".png":".jpg";
        String safeName=sanitizeName(name,ext);
        File dir=new File(context.getFilesDir(),"documents/p"+profileId); if(!dir.exists() && !dir.mkdirs()) throw new IOException("Speicherordner nicht verfügbar");
        File temp=new File(dir,".import-"+UUID.randomUUID());
        MessageDigest digest=MessageDigest.getInstance("SHA-256"); long total=0;
        try(OutputStream out=new FileOutputStream(temp)){
            byte[] buf=new byte[8192]; int n;
            while((n=in.read(buf))!=-1){ if(n==0)continue; total+=n; if(total>MAX_BYTES)throw new IOException("Datei größer als 25 MB"); digest.update(buf,0,n); out.write(buf,0,n); }
        } catch(Exception e){temp.delete();throw e;}
        String hash=hex(digest.digest());
        if(db.hasAttachmentHash(profileId,hash)){temp.delete();return new Result(true,0,safeName);}
        File finalFile=new File(dir,UUID.randomUUID()+ext);
        if(!temp.renameTo(finalFile)){copy(temp,finalFile);temp.delete();}
        long id;
        try{id=db.addAttachment(profileId,safeName,mime,hash,finalFile.getAbsolutePath(),total);}catch(Exception e){finalFile.delete();throw e;}
        return new Result(false,id,safeName);
    }

    private static String sniff(byte[] h,int n,String declared) throws IOException {
        if(n>=4 && h[0]=='%' && h[1]=='P' && h[2]=='D' && h[3]=='F') return "application/pdf";
        if(n>=3 && (h[0]&0xff)==0xff && (h[1]&0xff)==0xd8 && (h[2]&0xff)==0xff) return "image/jpeg";
        if(n>=8 && (h[0]&0xff)==0x89 && h[1]=='P' && h[2]=='N' && h[3]=='G') return "image/png";
        if("application/pdf".equals(declared)||"image/jpeg".equals(declared)||"image/png".equals(declared)) throw new IOException("Dateiinhalt passt nicht zum erlaubten Format");
        throw new IOException("Nicht unterstütztes Dateiformat");
    }

    private String displayName(Uri uri){
        String name="beleg";
        try(Cursor c=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst()){String v=c.getString(0);if(v!=null&&!v.isBlank())name=v;}}
        catch(Exception ignored){}
        return name;
    }

    private static String sanitizeName(String name,String ext){
        String v=name==null?"beleg":name.replaceAll("[\\r\\n\\t\\u0000-\\u001f]","_").replaceAll("[\\/\\\\]","_").trim();
        if(v.isEmpty())v="beleg"; if(v.length()>120)v=v.substring(0,120);
        if(!v.toLowerCase(Locale.ROOT).endsWith(ext))v+=ext; return v;
    }
    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.ROOT,"%02x",x));return s.toString();}
    private static void copy(File a,File b)throws IOException{try(InputStream in=new FileInputStream(a);OutputStream out=new FileOutputStream(b)){byte[] x=new byte[8192];int n;while((n=in.read(x))!=-1)out.write(x,0,n);}}
}
