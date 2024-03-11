package hu.hl.cellinfo_200827;

import android.graphics.Color;
import android.os.Bundle;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.IntStream;

public class MainActivity extends Activity {
	private static SimpleDateFormat simpledateformat= new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	private Intent serviceintent;
	private SQLiteDatabase database;
	private ToggleButton btn_record;
	private TextView tv_session_id;
	private TextView tv_session_begin;
	private TextView tv_session_end;
	private ToggleButton btn_grp_type;
	private ToggleButton btn_grp_enb;
	private ToggleButton btn_grp_lcid;
	private ToggleButton btn_grp_dbm;
	private ToggleButton btn_grp_ta;
	private ToggleButton btn_ord_type;
	private ToggleButton btn_ord_enb;
	private ToggleButton btn_ord_lcid;
	private ToggleButton btn_ord_dbm;
	private ToggleButton btn_ord_ta;
	private ToggleButton btn_ord_cnt;
	private LinearLayout tbl_body;
	private BroadcastReceiver receiver= new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			Log.d("×MainActivity","befosol");
			MainActivity.this.onUpdate();
		}
	};
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("×MainActivity.onCreate", "hácácá");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		database= openOrCreateDatabase("hu.hl.cellinfo_200819.database", MODE_PRIVATE,null);
		database.execSQL("CREATE TABLE IF NOT EXISTS t_samples(session INTEGER, type VARCHAR(7), enb INTEGER, lcid INTEGER, pci INTEGER, dbm INTEGER, ta INTEGER, lat DECIMAL(7, 4), lon DECIMAL(7, 4), spd DECIMAL(5, 2), tmstmp INTEGER);");
		database.execSQL("CREATE TABLE IF NOT EXISTS t_settings(field VARCHAR(255), value VARCHAR(255));");
		database.execSQL("CREATE TABLE IF NOT EXISTS t_sessions(id INTEGER NOT NULL PRIMARY KEY, start LONG, stop LONG, selected INTEGER);");
		btn_record= findViewById(R.id.btn_record);
		tv_session_id= findViewById(R.id.tv_session_id);
		tv_session_begin= findViewById(R.id.tv_session_begin);
		tv_session_end=	findViewById(R.id.tv_session_end);
		btn_grp_type= findViewById(R.id.btn_grp_type);
		btn_grp_enb= findViewById(R.id.btn_grp_enb);
		btn_grp_lcid= findViewById(R.id.btn_grp_lcid);
		btn_grp_dbm= findViewById(R.id.btn_grp_dbm);
		btn_grp_ta= findViewById(R.id.btn_grp_ta);
		btn_ord_type= findViewById(R.id.btn_ord_type);
		btn_ord_enb= findViewById(R.id.btn_ord_enb);
		btn_ord_lcid= findViewById(R.id.btn_ord_lcid);
		btn_ord_dbm= findViewById(R.id.btn_ord_dbm);
		btn_ord_ta= findViewById(R.id.btn_ord_ta);
		btn_ord_cnt= findViewById(R.id.btn_ord_cnt);
		tbl_body= findViewById(R.id.tbl_body);
		serviceintent= new Intent(getApplicationContext(), MainService.class);
		btn_record.setChecked(isMyServiceRunning(MainService.class));
		Cursor c= database.rawQuery("SELECT * FROM t_sessions;", null);
		c.moveToFirst();
		if (c.isAfterLast()) {
			database.execSQL("INSERT INTO t_sessions(id, selected) VALUES(0, 1);");
		} else {
			onUpdate();
		}
		c.close();
	}

	private boolean isMyServiceRunning(Class<MainService> mainServiceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (MainService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	protected void onResume() {
		super.onResume();
		Log.d("×MainActivity.onResume", "hácácá");
		registerReceiver(receiver, new IntentFilter("hu.hl.cellinfo_200819.update"));
	}
	protected void onPause() {
		super.onPause();
		Log.d("×MainActivity.onPause", "hácácá");
		unregisterReceiver(receiver);
	}
	public void onClick(View view) throws IOException {
		switch (view.getId()) {
			case R.id.btn_reset: {
				database.execSQL("DELETE FROM t_samples;");
				database.execSQL("DELETE FROM t_sessions;");
				database.execSQL("INSERT INTO t_sessions(id, selected) VALUES(0, 1);");
				break;
			}
			case R.id.btn_export: {
				StringBuilder str_csv= new StringBuilder();
				Cursor c= database.rawQuery("SELECT * FROM t_samples;", null);
				c.moveToFirst();
				IntStream.range(0, c.getColumnCount()).forEach(cn -> {
					str_csv.append(c.getColumnName(cn)+((cn<c.getColumnCount()-1) ? ";" : "\n"));
				});
				IntStream.range(0, c.getCount()).forEach(rn -> {
					IntStream.range(0, c.getColumnCount()).forEach(cn -> {
						str_csv.append(c.getString(cn)+((cn<c.getColumnCount()-1) ? ";" : (rn<c.getCount()) ? "\n" : ""));
					});
					c.moveToNext();
				});
				File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
				if (!exportDir.exists()) {
					exportDir.mkdirs();
				}
				String fn= exportDir.getAbsolutePath()+"/cellinfo_200827_database.csv";
				Log.d("×MainActivity.onClick", "büffesztettkötés: "+fn);
				OutputStreamWriter osw= new OutputStreamWriter(new FileOutputStream(fn, false));
				osw.write(str_csv.toString().replace(".", ","));
				osw.close();
				break;
			}
			case R.id.btn_record: {
				if (btn_record.isChecked()) {
					startForegroundService(serviceintent);
				} else {
					stopService(serviceintent);
				}
				break;
			}
			case R.id.btn_new_session: {
				database.execSQL("UPDATE t_sessions SET selected= 0;");
				database.execSQL("INSERT INTO t_sessions(id, selected) VALUES((SELECT COUNT(*) FROM t_sessions), 1);");
				break;
			}
			case R.id.btn_prev_session: {
				database.execSQL("UPDATE t_sessions SET selected= CASE WHEN id=(SELECT id FROM t_sessions WHERE selected=1)-1 OR selected=1 AND id=(SELECT MIN(id) FROM t_sessions) THEN 1 ELSE 0 END;");
				break;
			}
			case R.id.btn_next_session: {
				database.execSQL("UPDATE t_sessions SET selected= CASE WHEN id-1=(SELECT id FROM t_sessions WHERE selected=1) OR selected=1 AND id=(SELECT MAX(id) FROM t_sessions) THEN 1 ELSE 0 END;");
				break;
			}
		}
		onUpdate();
	}
	public void onUpdate() {
		Cursor c= database.rawQuery("SELECT * FROM t_sessions WHERE selected=1;", null);
	//	id INTEGER, start LONG, stop LONG, selected INTEGER
		c.moveToFirst();
		int session= c.getInt(c.getColumnIndex("id"));
		tv_session_id.setText(String.valueOf(session));
		long session_begin= c.getLong(c.getColumnIndex("start"));
		long session_end= c.getLong(c.getColumnIndex("stop"));
		tv_session_begin.setText((session_begin==0) ? "" : simpledateformat.format(new Date(session_begin)));
		tv_session_end.setText((session_end==0) ? "" : simpledateformat.format(new Date(session_end)));
		c.close();
		String str_sql_0=
			((btn_grp_type.isChecked()) ? "type," : "")+
			((btn_grp_enb.isChecked()) ? "enb," : "")+
			((btn_grp_lcid.isChecked()) ? "lcid,": "")+
			((btn_grp_dbm.isChecked()) ? "dbm," : "")+
			((btn_grp_ta.isChecked()) ? "ta," : "");
		String str_sql_1= (str_sql_0.equals("")) ? "" : "GROUP BY "+str_sql_0.replaceFirst(".$"," ");
		String str_sql_2=
			((btn_ord_type.isChecked()) ? "type ASC," : "")+
			((btn_ord_enb.isChecked()) ? "enb ASC," : "")+
			((btn_ord_lcid.isChecked()) ? "lcid ASC," : "")+
			((btn_ord_dbm.isChecked()) ? "dbm DESC," : "")+
			((btn_ord_ta.isChecked()) ? "ta ASC," : "")+
			((btn_ord_cnt.isChecked()) ? "cnt DESC," : "");
		String str_sql_3= "SELECT "+str_sql_0+"COUNT(*) AS cnt, MAX(tmstmp) AS mtmstmp FROM t_samples WHERE session="+String.valueOf(session)+" "+str_sql_1+"ORDER BY "+str_sql_2+"mtmstmp DESC;";
		Log.d("×MainActivity.onUpdate", "hácácá:"+str_sql_3);
		c= database.rawQuery(str_sql_3, null);
		c.moveToFirst();
		int rn= 0;
		while (!c.isAfterLast()) {
			LinearLayout row;
			TextView tv_type;
			TextView tv_enb;
			TextView tv_lcid;
			TextView tv_dbm;
			TextView tv_ta;
			TextView tv_cnt;
			if (rn==tbl_body.getChildCount()) {
				tbl_body.addView(row= new LinearLayout(getApplicationContext()));
				row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				row.setOrientation(LinearLayout.HORIZONTAL);
				Space spc;
				row.addView(spc= new Space(getApplicationContext(), null, 0, R.style.BodyText));
				row.addView(tv_type= new TextView(getApplicationContext(), null, 0, R.style.BodyText));
				row.addView(tv_enb= new TextView(getApplicationContext(), null, 0, R.style.BodyText));
				row.addView(tv_lcid= new TextView(getApplicationContext(), null, 0, R.style.BodyText));
				row.addView(tv_dbm= new TextView(getApplicationContext(), null, 0, R.style.BodyText));
				row.addView(tv_ta= new TextView(getApplicationContext(), null, 0, R.style.BodyText));
				row.addView(tv_cnt= new TextView(getApplicationContext(), null, 0, R.style.BodyText));
				LinearLayout.LayoutParams pr= new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				pr.weight= 1;
				spc.setLayoutParams(pr);
				tv_type.setLayoutParams(pr);
				tv_enb.setLayoutParams(pr);
				tv_lcid.setLayoutParams(pr);
				tv_dbm.setLayoutParams(pr);
				tv_ta.setLayoutParams(pr);
				tv_cnt.setLayoutParams(pr);
			} else {
				row= (LinearLayout) tbl_body.getChildAt(rn);
				tv_type= (TextView) row.getChildAt(1);
				tv_enb= (TextView) row.getChildAt(2);
				tv_lcid= (TextView) row.getChildAt(3);
				tv_dbm= (TextView) row.getChildAt(4);
				tv_ta= (TextView) row.getChildAt(5);
				tv_cnt= (TextView) row.getChildAt(6);
			}
			String s= ((c.getColumnIndex("type")<0) ? " " : c.getString(c.getColumnIndex("type")));
			tv_type.setText(s);
			switch (s) {
				case "gsm":
					tv_type.setTextColor(Color.RED);
					break;
				case "lte":
					tv_type.setTextColor(Color.CYAN);
					break;
				case "wcdma":
					tv_type.setTextColor(Color.GREEN);
					break;
				default:
					tv_type.setTextColor(Color.WHITE);
					break;
			}
			tv_enb.setText(((c.getColumnIndex("enb")<0) ? " " : c.getString(c.getColumnIndex("enb"))));
			tv_lcid.setText(((c.getColumnIndex("lcid")<0) ? " " : c.getString(c.getColumnIndex("lcid"))));
			tv_dbm.setText(((c.getColumnIndex("dbm")<0) ? " " : c.getString(c.getColumnIndex("dbm"))));
			if (-1<c.getColumnIndex("dbm")) {
				if (Integer.valueOf(c.getString(c.getColumnIndex("dbm")))<=-105) {
					tv_dbm.setBackground(getDrawable(R.drawable.border_red));
				} else if (Integer.valueOf(c.getString(c.getColumnIndex("dbm")))<=-85) {
					tv_dbm.setBackground(getDrawable(R.drawable.border_yellow));
				} else {
					tv_dbm.setBackground(getDrawable(R.drawable.border_green));
				}
			} else {
				tv_dbm.setBackground(getDrawable(R.drawable.border_default));
			}
			tv_ta.setText(((c.getColumnIndex("ta")<0) ? " " :  c.getString(c.getColumnIndex("ta"))));
			tv_cnt.setText(c.getString(c.getColumnIndex("cnt")));
			c.moveToNext();
			rn++;
		}
		while (rn<tbl_body.getChildCount()) {
			tbl_body.removeView(tbl_body.getChildAt(rn));
		}
	}
}
