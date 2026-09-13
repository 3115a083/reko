package de.reko.app;

import android.content.*;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.*;

final class BackupManager {
    private static final long MAX_BACKUP_BYTES=200L*1024*1024;
    private final Context context; private final RekoDb db; private final DocumentStore documents;
    BackupManager(Context c,RekoDb db){context=c.getApplicationContext();this.db=db;documents=new DocumentStore(context,db);}

    void exportProfile(long profileId,Uri target)throws Exception{
        JSONObject manifest=db.exportProfile(profileId);
        try(OutputStream raw=context.getContentResolver().openOutputStream(target); ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(require(raw)))){
            put(zip,"manifest.json",manifest.toString(2).getBytes(StandardCharsets.UTF_8));
            for(String[] a:db.attachments(profileId)){
                File f=new File(a[4]); if(!f.isFile())continue;
                String entry="documents/"+a[3]+DocumentStore.extension(a[2]);
                zip.putNextEntry(new ZipEntry(entry));
                try(InputStream in=new FileInputStream(f)){copyLimited(in,zip,DocumentStore.MAX_BYTES);}
                zip.closeEntry();
            }
        }
    }

    int importProfile(long profileId,Uri source)throws Exception{
        validateExternalContentUri(source);
        File dir=new File(context.getCacheDir(),"restore-"+UUID.randomUUID());if(!dir.mkdirs())throw new IOException("Temporärer Speicher nicht verfügbar");
        JSONObject manifest=null;Map<String,File> docs=new HashMap<>();long total=0;
        try(InputStream raw=context.getContentResolver().openInputStream(source); ZipInputStream zip=new ZipInputStream(new BufferedInputStream(require(raw)))){
            ZipEntry e;
            while((e=zip.getNextEntry())!=null){
                if(e.isDirectory())continue;String name=e.getName();
                if(name.contains("..")||name.startsWith("/")||name.startsWith("\\"))throw new SecurityException("Unsicherer ZIP-Pfad");
                if("manifest.json".equals(name)){
                    ByteArrayOutputStream b=new ByteArrayOutputStream();total+=copyLimited(zip,b,5L*1024*1024);manifest=new JSONObject(b.toString(StandardCharsets.UTF_8));
                } else if(name.startsWith("documents/") && docs.size()<1000){
                    File f=new File(dir,UUID.randomUUID().toString());try(OutputStream out=new FileOutputStream(f)){long n=copyLimited(zip,out,DocumentStore.MAX_BYTES);total+=n;}docs.put(name.substring("documents/".length()),f);
                }
                if(total>MAX_BACKUP_BYTES)throw new IOException("Backup zu groß");
                zip.closeEntry();
            }
        }finally{if(manifest==null){deleteTree(dir);}}
        if(manifest==null||!"reko-backup".equals(manifest.optString("format"))||manifest.optInt("version")!=2){deleteTree(dir);throw new JSONException("Ungültiges Backup");}
        db.importInto(profileId,manifest);
        int imported=0;JSONArray attachments=manifest.optJSONArray("attachments");
        if(attachments!=null)for(int i=0;i<attachments.length();i++){
            JSONObject a=attachments.getJSONObject(i);String sha=a.optString("sha256");String mime=a.optString("mime");File f=findDoc(docs,sha,mime);if(f==null)continue;
            DocumentStore.Result r=documents.importFile(profileId,f,a.optString("name","beleg"),mime);if(!r.duplicate)imported++;
        }
        deleteTree(dir);return imported;
    }

    private void validateExternalContentUri(Uri uri)throws Exception{
        if(uri==null||!"content".equalsIgnoreCase(uri.getScheme()))throw new SecurityException("Nur content-URIs erlaubt");
        String authority=uri.getAuthority();if(authority==null||authority.isBlank())throw new SecurityException("Fehlende URI-Autorität");
        String own=context.getPackageName();if(authority.equals(own)||authority.startsWith(own+"."))throw new SecurityException("Eigene Provider nicht als externe Quelle erlaubt");
        String rawPath=uri.getPath();if(rawPath==null)throw new SecurityException("Fehlender URI-Pfad");Path normalized=FileSystems.getDefault().getPath(rawPath).normalize();
        if(normalized.startsWith("/data")||normalized.startsWith("/proc")||normalized.startsWith("/sys")||normalized.startsWith("/dev"))throw new SecurityException("Privater Systempfad nicht erlaubt");
    }

    private static File findDoc(Map<String,File> docs,String sha,String mime){return docs.get(sha+DocumentStore.extension(mime));}
    private static OutputStream require(OutputStream o)throws IOException{if(o==null)throw new IOException("Ziel nicht verfügbar");return o;}
    private static InputStream require(InputStream i)throws IOException{if(i==null)throw new IOException("Quelle nicht verfügbar");return i;}
    private static void put(ZipOutputStream z,String name,byte[] bytes)throws IOException{z.putNextEntry(new ZipEntry(name));z.write(bytes);z.closeEntry();}
    private static long copyLimited(InputStream in,OutputStream out,long max)throws IOException{byte[] b=new byte[8192];long total=0;int n;while((n=in.read(b))!=-1){total+=n;if(total>max)throw new IOException("Datei zu groß");out.write(b,0,n);}return total;}
    private static void deleteTree(File f){if(f==null)return;if(f.isDirectory()){File[] c=f.listFiles();if(c!=null)for(File x:c)deleteTree(x);}f.delete();}
}
