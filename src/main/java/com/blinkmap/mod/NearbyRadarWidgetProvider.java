package com.blinkmap.mod;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.net.Uri;
import android.widget.RemoteViews;
import java.util.List;

public final class NearbyRadarWidgetProvider extends AppWidgetProvider {
    private static final String ACTION="app.blinkmod.WIDGET_RADAR_REFRESH";
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)update(c,m,id);}
    @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);if(ACTION.equals(i.getAction())){int id=i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);if(id>=0)update(c,AppWidgetManager.getInstance(c),id);}}
    static void update(Context c,AppWidgetManager manager,int widgetId){int layout=FriendWidgetProvider.id(c,"layout","blinkmod_nearby_widget");if(layout==0)return;RemoteViews v=new RemoteViews(c.getPackageName(),layout);List<MarkerStore.Marker> all=MarkerStore.load(c);int radar=FriendWidgetProvider.id(c,"id","nearby_radar"),refresh=FriendWidgetProvider.id(c,"id","nearby_refresh");v.setImageViewBitmap(radar,radar(c,all));for(int i=0;i<3;i++){int row=FriendWidgetProvider.id(c,"id","nearby_"+(i+1));if(i<all.size()){MarkerStore.Marker m=all.get(i);v.setTextViewText(row,m.displayName()+"  ·  "+MarkerStore.distance(m.distance));Intent open=new Intent(Intent.ACTION_VIEW,Uri.parse("blink:///map/marker/?id="+m.id)).setPackage(c.getPackageName());v.setOnClickPendingIntent(row,PendingIntent.getActivity(c,widgetId*10+i,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));}else v.setTextViewText(row,i==0?"Открой карту Blink":"");}Intent r=new Intent(c,NearbyRadarWidgetProvider.class).setAction(ACTION).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);v.setOnClickPendingIntent(refresh,PendingIntent.getBroadcast(c,widgetId,r,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));manager.updateAppWidget(widgetId,v);}
    private static Bitmap radar(Context c,List<MarkerStore.Marker> markers){int w=240,h=240;Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas x=new Canvas(b);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);x.drawColor(Color.rgb(31,31,37));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.rgb(62,62,73));for(int r:new int[]{38,72,106})x.drawCircle(120,120,r,p);x.drawLine(14,120,226,120,p);x.drawLine(120,14,120,226,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);x.drawCircle(120,120,6,p);Location own=MarkerStore.lastLocation(c);float max=1000f;for(int i=0;i<markers.size()&&i<8;i++)if(markers.get(i).distance>max)max=markers.get(i).distance;max=Math.min(Math.max(max,1000f),20000f);if(own!=null)for(int i=0;i<markers.size()&&i<8;i++){MarkerStore.Marker m=markers.get(i);float[] res=new float[3];Location.distanceBetween(own.getLatitude(),own.getLongitude(),m.latitude,m.longitude,res);double a=Math.toRadians(res[1]);float radius=Math.min(103f,res[0]/max*103f);float px=120+(float)Math.sin(a)*radius,py=120-(float)Math.cos(a)*radius;p.setColor(i==0?Color.rgb(128,77,255):Color.rgb(190,167,255));x.drawCircle(px,py,i==0?11:7,p);}return b;}
}
