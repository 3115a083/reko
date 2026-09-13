package de.reko.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

final class IcsParser {
    static final class Event {
        final String start,end,summary,location;
        Event(String start,String end,String summary,String location){this.start=start;this.end=end;this.summary=summary;this.location=location;}
        @Override public String toString(){return start+" · "+(location.isEmpty()?summary:location+" · "+summary);}
    }

    static ArrayList<Event> parse(InputStream in,long maxBytes)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;long total=0;
        while((n=in.read(b))!=-1){total+=n;if(total>maxBytes)throw new IOException("ICS-Datei zu groß");out.write(b,0,n);}
        String text=out.toString(StandardCharsets.UTF_8);if(!text.contains("BEGIN:VCALENDAR"))throw new IOException("Keine gültige ICS-Datei");
        String[] raw=text.replace("\r\n","\n").replace('\r','\n').split("\n");ArrayList<String> lines=new ArrayList<>();
        for(String line:raw){if((line.startsWith(" ")||line.startsWith("\t"))&&!lines.isEmpty())lines.set(lines.size()-1,lines.get(lines.size()-1)+line.substring(1));else lines.add(line);}
        ArrayList<Event> result=new ArrayList<>();boolean event=false;String start="",end="",summary="",location="";
        for(String line:lines){
            if("BEGIN:VEVENT".equals(line)){event=true;start=end=summary=location="";continue;}
            if("END:VEVENT".equals(line)){if(event&&!start.isEmpty())result.add(new Event(start,end.isEmpty()?start:end,clean(summary),clean(location)));event=false;if(result.size()>=200)break;continue;}
            if(!event)continue;
            int colon=line.indexOf(':');if(colon<0)continue;String key=line.substring(0,colon).toUpperCase(Locale.ROOT);String value=line.substring(colon+1);
            if(key.startsWith("DTSTART"))start=date(value);else if(key.startsWith("DTEND"))end=date(value);else if(key.equals("SUMMARY"))summary=value;else if(key.equals("LOCATION"))location=value;
        }
        return result;
    }

    private static String date(String v){String digits=v.replaceAll("[^0-9]","");if(digits.length()<8)return "";String d=digits.substring(0,8);return d.substring(0,4)+"-"+d.substring(4,6)+"-"+d.substring(6,8);}
    private static String clean(String v){return v.replace("\\n"," ").replace("\\,",",").replace("\\;",";").replace("\\\\","\\").trim();}
}
