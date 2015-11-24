package at.kju.datacollector.storage;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.UUID;

import at.kju.datacollector.Constants;

public class DCDataHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "dcsync.db";
	private static final int DATABASE_VERSION = 1;
	public static final String TAG = "DCDataHelper";

	
	// Table name
	public static final String DATA_TABLE = "T_DOCUMENTS";
	public static final String SETTINGS_TABLE = "T_SETTINGS";

	// Columns
	public static final String CREATION_DATE = "creationdate";
	public static final String MODIFIED_DATE = "lastmodifieddate";
	public static final String CID = "cid";
	public static final String CREATOR_DUID = "creator_duid";
	public static final String CREATOR_USER = "creator_user";
	public static final String MODIFIED_DUID = "modified_duid";
	public static final String MODIFIED_USER = "modified_user";
	public static final String PATH ="path";
	public static final String DOCUMENT = "document";
	public static final String FILES = "files";
	public static final String SYNC_STATE = "syncstate";
	public static final String SERVER_MODIFIED = "server_modified";
	public static final String LOCAL = "localonly";

	public static final String ID = "_ID";

	public static final String URL = "url";
	public static final String DEVICE_UUID ="device_uuid";
	public static final String JSONDATA ="jsondata";

	public static final int SYNC_STATE_NEW = 0;
	public static final int SYNC_STATE_MODIFIED = 1;
	public static final int SYNC_STATE_SYNCED = 2;
	public static final int SYNC_STATE_DELETED = 3;

	private Context ctx = null;
	public DCDataHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		ctx = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "create table " + DATA_TABLE + "( " + BaseColumns._ID + " integer primary key autoincrement, "
				+ CID + " text not null, "
				+ CREATION_DATE + " integer not null, "
				+ MODIFIED_DATE + " integer not null, "
				+ SERVER_MODIFIED + " integer null, "
				+ LOCAL + " integer null, "
				+ CREATOR_USER + " text not null, "
				+ MODIFIED_DUID + " text not null, "
				+ MODIFIED_USER + " text not null, "
				+ PATH + " text not null, "
				+ CREATOR_DUID + " text not null, "
				+ SYNC_STATE + " int not null, "
				+ FILES + " text null, "
				+ DOCUMENT + " text null);";
		Log.d("EventsData", "onCreate DATA_TABLE: " + sql);
		db.execSQL(sql);
		sql = "create table " + SETTINGS_TABLE + "( " + BaseColumns._ID + " integer primary key autoincrement, " 
				+ DEVICE_UUID + " text not null, "
				+ JSONDATA + " text not null );";
		Log.d("EventsData", "onCreate SETTINGS: " + sql);
		db.execSQL(sql);

		//create unique device id
	    final TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
	    final String tmDevice, tmSerial, androidId;
	    tmDevice = "" + tm.getDeviceId();
	    tmSerial = "" + tm.getSimSerialNumber();
	    androidId = "" + android.provider.Settings.Secure.getString(ctx.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
	    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
    	db.execSQL("insert into " + SETTINGS_TABLE + "(" + DEVICE_UUID + "," + URL + "," + JSONDATA +  ") values('" + deviceUuid + "','" + Constants.SYNC_URL + "','')");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion >= newVersion)
			return;

		String sql = null;
		if (oldVersion == 1) {
			//updradeSql( db, "");
		}

	}

	private void upgradeSql(SQLiteDatabase db, String sql) {
		Log.d("EventsData", "onUpgrade	: " + sql);
		db.execSQL(sql);

	}
	public String getUniqueDeviceId() {
		SQLiteDatabase db = this.getReadableDatabase();
		try {
			return getUniqueDeviceId(db);
		}
		finally {
			db.close();
		}

   }
	public String getUniqueDeviceId(SQLiteDatabase db) {

	    Cursor c = null;
	    try {
		    c = db.query(SETTINGS_TABLE, new String[] {DEVICE_UUID}, null, null, null, null, null);
		    if( c!= null && c.moveToFirst()) {
		    	return c.getString(0);
		    }
		    return "UNKNOWN_DEVICE_ID";
	    }
	    catch(Exception e)  {
	    	Log.e(TAG, "getUniqueDeviceID failed: " + e.getMessage());
	    	return "UNKNOWN_DEVICE_ID";
	    }
	    finally {
	    	if( c!= null)
	    		c.close();
	    }
   }
	
}
