package com.inaki.micoche;
import android.content.*;
public final class AutoParkingPrefs {
 private static SharedPreferences p(Context c){return c.getSharedPreferences("auto_parking",Context.MODE_PRIVATE);}
 public static boolean enabled(Context c){return p(c).getBoolean("enabled",false);}
 public static String address(Context c){return p(c).getString("device_address","");}
 public static String deviceName(Context c){return p(c).getString("device_name","");}
 public static boolean seenConnected(Context c){return p(c).getBoolean("seen_connected",false);}
 public static void configure(Context c,String a,String n){p(c).edit().putBoolean("enabled",true).putString("device_address",a).putString("device_name",n).putBoolean("seen_connected",false).apply();}
 public static void setSeenConnected(Context c,boolean v){p(c).edit().putBoolean("seen_connected",v).apply();}
 public static void setLastEvent(Context c,String s){p(c).edit().putString("last_event",s).apply();}
 public static String lastEvent(Context c){return p(c).getString("last_event","Todavía sin eventos");}
 public static void markDisconnect(Context c,long t){p(c).edit().putLong("disconnect_at",t).putBoolean("parking_pending",false).apply();}
 public static void setParkingPending(Context c,boolean v){p(c).edit().putBoolean("parking_pending",v).apply();}
 public static boolean parkingPending(Context c){return p(c).getBoolean("parking_pending",false);}
 public static long disconnectAt(Context c){return p(c).getLong("disconnect_at",0);}
}
