package com.blinkmap.mod;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import android.widget.RemoteViews;
import java.io.File;
import java.util.List;

public final class FriendWidgetProvider extends AppWidgetProvider {
    static final String PREFS="blinkmod_settings";
    private static final String ACTION_REFRESH="app.blinkmod.WIDGET_REFRESH";
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)update(c,m,id);}
    @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);if(ACTION_REFRESH.equals(i.getAction())){AppWidgetManager m=AppWidgetManager.getInstance(c);int id=i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);if(id>=0)update(c,m,id);else for(int x:m.getAppWidgetIds(new ComponentName(c,FriendWidgetProvider.class)))update(c,m,x);}}
    static void update(Context c,AppWidgetManager manager,int widgetId){
        int layout=id(c,"layout","blinkmod_friend_widget");if(layout==0)return;
        RemoteViews v=new RemoteViews(c.getPackageName(),layout);SharedPreferences p=c.getSharedPreferences(PREFS,0);
        long selected=p.getLong("widget_friend_"+widgetId,-1L);MarkerStore.Marker marker=selected<0?null:MarkerStore.find(c,selected);
        if(marker==null){List<MarkerStore.Marker> all=MarkerStore.load(c);if(!all.isEmpty()){marker=all.get(0);p.edit().putLong("widget_friend_"+widgetId,marker.id).apply();}}
        int name=id(c,"id","widget_name"),detail=id(c,"id","widget_detail"),meta=id(c,"id","widget_meta"),preview=id(c,"id","widget_map_preview"),root=id(c,"id","widget_root"),refresh=id(c,"id","widget_refresh"),settings=id(c,"id","widget_settings");
        if(preview!=0)v.setImageViewBitmap(preview,mapPreview(c,marker));
        if(marker==null){v.setTextViewText(name,"BlinkMod");v.setTextViewText(detail,"Открой карту Blink");v.setTextViewText(meta,"Затем нажми обновить");}
        else{
            boolean age=p.getBoolean("widget_age_"+widgetId,true),battery=p.getBoolean("widget_battery_"+widgetId,true),speed=p.getBoolean("widget_speed_"+widgetId,true);
            v.setTextViewText(name,marker.displayName());String d=MarkerStore.distance(marker.distance);if(age)d+=" · "+MarkerStore.age(marker.updatedAt);v.setTextViewText(detail,d);
            StringBuilder s=new StringBuilder();if(battery&&marker.charge!=null)s.append(marker.charge).append("% заряд");if(speed&&marker.speed>1f){if(s.length()>0)s.append(" · ");s.append(Math.round(marker.speed)).append(" км/ч");}if(s.length()==0)s.append("позиция из Blink");v.setTextViewText(meta,s.toString());
            Intent open=new Intent(Intent.ACTION_VIEW,Uri.parse("blink:///map/marker/?id="+marker.id)).setPackage(c.getPackageName());v.setOnClickPendingIntent(root,PendingIntent.getActivity(c,widgetId,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        }
        Intent r=new Intent(c,FriendWidgetProvider.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);v.setOnClickPendingIntent(refresh,PendingIntent.getBroadcast(c,widgetId,r,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        Intent cfg=new Intent(c,FriendWidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId).putExtra("edit",true);v.setOnClickPendingIntent(settings,PendingIntent.getActivity(c,widgetId+100000,cfg,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));manager.updateAppWidget(widgetId,v);
    }
    @Override public void onDeleted(Context c,int[] ids){SharedPreferences.Editor e=c.getSharedPreferences(PREFS,0).edit();for(int id:ids){e.remove("widget_friend_"+id);e.remove("widget_zoom_"+id);e.remove("widget_age_"+id);e.remove("widget_battery_"+id);e.remove("widget_speed_"+id);}e.apply();}
    static File mapFile(Context c,long markerId){File d=new File(c.getCacheDir(),"blinkmod_widget_maps");if(!d.exists())d.mkdirs();return new File(d,"friend_"+markerId+".png");}
    static Bitmap mapPreview(Context c,MarkerStore.Marker marker){if(marker!=null){Bitmap b=BitmapFactory.decodeFile(mapFile(c,marker.id).getAbsolutePath());if(b!=null)return b;}return fallbackPreview(marker);}
    private static Bitmap fallbackPreview(MarkerStore.Marker marker){final int size=320;Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Path clip=new Path();clip.addRoundRect(new RectF(0,0,size,size),55,55,Path.Direction.CW);c.clipPath(clip);c.drawColor(Color.rgb(35,35,41));long seed=marker==null?7L:marker.id;p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.rgb(59,59,68));p.setStrokeWidth(28);float shift=(float)(Math.abs(seed)%55)-27;c.drawLine(-30,80+shift,350,185+shift,p);c.drawLine(45-shift,-25,210-shift,350,p);p.setColor(Color.rgb(48,48,57));p.setStrokeWidth(15);c.drawLine(-15,260-shift,340,38-shift,p);c.drawLine(0,145-shift,330,245-shift,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(75,128,77,255));c.drawCircle(160,160,50,p);p.setColor(Color.rgb(128,77,255));c.drawCircle(160,160,25,p);p.setColor(Color.WHITE);c.drawCircle(160,160,8,p);return b;}
    static int id(Context c,String type,String name){return c.getResources().getIdentifier(name,type,c.getPackageName());}
}
