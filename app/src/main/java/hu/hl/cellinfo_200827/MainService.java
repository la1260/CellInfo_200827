package hu.hl.cellinfo_200827;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MainService extends Service implements LocationListener, Runnable {
	public static final String notificationChannelId = "feldurrantakereke";
	private Handler handler= null;
	private LocationManager locationmanager;
	private TelephonyManager telephonymanager;
	private Location location= null;
	private SQLiteDatabase database;
	private Intent updateintent= new Intent("hu.hl.cellinfo_200819.update");
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("×MainService.onStartCommand", "hülyehávo");
		createNotificationChannel();
		createNotification();
		locationmanager= (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
			ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			locationmanager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, this);
			telephonymanager= (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
			if (handler==null) {
				handler= new Handler();
				handler.postAtTime(this, Math.floorDiv(SystemClock.uptimeMillis() + 1000, 1000) * 1000);
			}
		} else {
			//tv_loc.setText("Engedélyezd a helyadatokat!");
		}
		database= openOrCreateDatabase("hu.hl.cellinfo_200819.database", MODE_PRIVATE,null);
		return START_NOT_STICKY;
	}
	public IBinder onBind(Intent intent) {
		return null;
	}
	public void onDestroy() {
		Log.d("×MainService.onDestroy", "hülyehávo");
		super.onDestroy();
		if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
			ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			locationmanager.removeUpdates(this);
			handler.removeCallbacks(this);
			handler = null;
		}
	}
	public void onLocationChanged(Location location) {
		Log.d("MainService.onLocationChanged", "hülyehávo");
		this.location= location;
	}
	public void onStatusChanged(String s, int i, Bundle bundle) {}
	public void onProviderEnabled(String s) {}
	public void onProviderDisabled(String s) {}
	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {
			NotificationManager notificationManager= getSystemService(NotificationManager.class);
			NotificationChannel notificationChannel= new NotificationChannel(notificationChannelId, notificationChannelId, NotificationManager.IMPORTANCE_DEFAULT);
			notificationManager.createNotificationChannel(notificationChannel);
		}
	}
	private void createNotification() {
		Intent notificationIntent= new Intent(getApplicationContext(), MainActivity.class);
		PendingIntent pendingIntent= PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
		Notification notification= new Notification.Builder(getApplicationContext(), notificationChannelId)
			.setContentTitle("hülyehávo")
			.setSmallIcon(R.drawable.ic_launcher_background) //ez qrva fontos, nélküle szar lesz a notification; többek között fos szöveg lesz benne, és nem fogja az activityot elindítani
			.setContentIntent(pendingIntent)
			.build();
		startForeground(1, notification);
	}

	public void run() {
		Long m= SystemClock.uptimeMillis();
		Long s= Math.floorDiv(m+1000, 1000)*1000;
		Log.d("MainService.run", "hülyehávo,"+m+","+s);
		handler.postAtTime(this, s);
		if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED && locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			CellInfo cellinfo= telephonymanager.getAllCellInfo().get(0);
			Double lat= (location==null) ? null : location.getLatitude();
			Double lon= (location==null) ? null : location.getLongitude();
			Float spd= (location==null) ? null : location.getSpeed();
			String type= "unknown";
			Integer enb= null;
			Integer lcid= null;
			Integer pci= null;
			Integer dbm= null;
			Integer ta= null;
			Long tmstmp= System.currentTimeMillis();
			switch (cellinfo.getClass().getCanonicalName()) {
				case "android.telephony.CellInfoCdma":
					break;
				case "android.telephony.CellInfoGsm":
					type= "gsm";
					CellInfoGsm cellinfogsm = (CellInfoGsm) cellinfo;
					enb= Math.floorDiv(cellinfogsm.getCellIdentity().getCid(), 10);
					lcid= Math.floorMod(cellinfogsm.getCellIdentity().getCid(), 10);
					dbm= cellinfogsm.getCellSignalStrength().getDbm();
					break;
				case "android.telephony.CellInfoLte":
					type= "lte";
					CellInfoLte cellinfolte= (CellInfoLte) cellinfo;
					enb= cellinfolte.getCellIdentity().getCi() >> 8 & 65535;
					lcid= cellinfolte.getCellIdentity().getCi() & 255;
					pci= cellinfolte.getCellIdentity().getPci();
					dbm= cellinfolte.getCellSignalStrength().getDbm();
					ta= cellinfolte.getCellSignalStrength().getTimingAdvance();
					break;
				case "android.telephony.CellInfoNr":
					break;
				case "android.telephony.CellInfoTdscdma":
					break;
				case "android.telephony.CellInfoWcdma":
					type= "wcdma";
					CellInfoWcdma cellinfowcdma= (CellInfoWcdma) cellinfo;
					enb= Math.floorDiv(cellinfowcdma.getCellIdentity().getCid() & 65535, 10);
					lcid= Math.floorMod(cellinfowcdma.getCellIdentity().getCid() & 65535, 10);
					dbm= cellinfowcdma.getCellSignalStrength().getDbm();
					break;
			}
			Cursor c= database.rawQuery("SELECT * FROM t_sessions WHERE selected=1;", null);
			c.moveToFirst();
			int session= Integer.valueOf(c.getString(c.getColumnIndex("id")));
			c.close();
			database.execSQL("INSERT INTO t_samples(session, type, enb, lcid, pci, dbm, ta, lat, lon, spd, tmstmp) VALUES ("+session+", '"+type+"', "+String.valueOf(enb)+", "+String.valueOf(lcid)+", "+String.valueOf(pci)+", "+String.valueOf(dbm)+", "+String.valueOf(ta)+", "+String.valueOf(lat)+", "+String.valueOf(lon)+", "+String.valueOf(spd)+", "+String.valueOf(tmstmp)+");");
			database.execSQL("UPDATE t_sessions SET start=CASE WHEN start IS NULL THEN "+String.valueOf(tmstmp)+" ELSE start END, stop="+String.valueOf(tmstmp)+" WHERE selected=1;");
			sendBroadcast(updateintent);
		}
	}
}
