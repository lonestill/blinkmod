package com.blinkmap.mod;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Read-only access to Blink's own cached map markers for widget surfaces. */
public final class MarkerStore {
    public static final class Marker {
        public final long id, updatedAt;
        public final String name, username;
        public final double latitude, longitude;
        public final Integer charge;
        public final float speed;
        public float distance = -1f;
        Marker(long id,String name,String username,double lat,double lon,long updated,Integer charge,float speed){
            this.id=id;this.name=name==null?"":name;this.username=username==null?"":username;
            latitude=lat;longitude=lon;updatedAt=updated;this.charge=charge;this.speed=speed;
        }
        public String displayName(){return name.length()>0?name:(username.length()>0?"@"+username:"Друг");}
    }
    private MarkerStore(){}

    public static List<Marker> load(Context context){
        ArrayList<Marker> out=new ArrayList<>();
        String[] names;
        try{names=context.databaseList();}catch(Throwable e){return out;}
        for(String dbName:names){
            SQLiteDatabase db=null;Cursor c=null;
            try{
                File f=context.getDatabasePath(dbName);if(f==null||!f.exists())continue;
                db=SQLiteDatabase.openDatabase(f.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
                c=db.rawQuery("SELECT profile_id,profile_name,profile_nickname,latitude,longitude,last_update_ts,charge,speed_km_h FROM MarkerEntity",null);
                while(c.moveToNext()){
                    Integer charge=c.isNull(6)?null:c.getInt(6);
                    out.add(new Marker(c.getLong(0),c.getString(1),c.getString(2),c.getDouble(3),c.getDouble(4),c.getLong(5),charge,c.getFloat(7)));
                }
                if(!out.isEmpty())break;
            }catch(Throwable ignored){}finally{if(c!=null)c.close();if(db!=null)db.close();}
        }
        Location own=lastLocation(context);
        if(own!=null){float[] result=new float[1];for(Marker m:out){Location.distanceBetween(own.getLatitude(),own.getLongitude(),m.latitude,m.longitude,result);m.distance=result[0];}}
        Collections.sort(out,new Comparator<Marker>(){@Override public int compare(Marker a,Marker b){
            if(a.distance>=0&&b.distance>=0)return Float.compare(a.distance,b.distance);
            return Long.compare(normalizeTime(b.updatedAt),normalizeTime(a.updatedAt));
        }});
        return out;
    }

    public static Marker find(Context context,long id){for(Marker m:load(context))if(m.id==id)return m;return null;}
    public static Location lastLocation(Context c){
        try{LocationManager lm=(LocationManager)c.getSystemService(Context.LOCATION_SERVICE);Location best=null;for(String p:lm.getProviders(true)){Location l=lm.getLastKnownLocation(p);if(l!=null&&(best==null||l.getTime()>best.getTime()))best=l;}return best;}catch(Throwable ignored){return null;}
    }
    public static long normalizeTime(long value){return value>0&&value<100000000000L?value*1000L:value;}
    public static String age(long value){long t=normalizeTime(value),d=System.currentTimeMillis()-t;if(t<=0)return"нет данных";if(d<0)d=0;if(d<60000)return"только что";long m=d/60000;if(m<60)return m+" мин назад";long h=m/60;if(h<24)return h+" ч назад";return h/24+" дн назад";}
    public static String distance(float meters){if(meters<0)return"расстояние неизвестно";if(meters<1000)return Math.round(meters)+" м";return String.format(java.util.Locale.US,"%.1f км",meters/1000f);}
}
