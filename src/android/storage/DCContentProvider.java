package at.kju.datacollector.storage;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import at.kju.datacollector.Constants;

public class DCContentProvider extends ContentProvider {
	String TAG = "DCContentProvider";

	private DCDataHelper dbhelper;
	private SQLiteDatabase db;

	// UriMatcher stuff
	private static final int DC_DOCUMENTS = 0;
	private static final int DOCUMENTS = 1;
	private static final int DOCUMENT = 2;
	private static final int SETTINGS = 3;

	private static final String[] TABLE_MAP = new String[] {  DCDataHelper.DATA_TABLE, DCDataHelper.DATA_TABLE,
			DCDataHelper.DATA_TABLE, DCDataHelper.SETTINGS_TABLE };
	private static final String[] SUFFIX_MAP = new String[] {  "dcdocuments", "document", "document", "settings" };

	private static String[] _typeMap = null;

	public static Uri _contentUri =null;
	public static Uri _documentsUri =null;
	public static Uri _settingsUri =null;

	private static UriMatcher _uRIMatcher = null;


	@Override
	public boolean onCreate() {
		String authority= Constants.getContentAuthority(getContext());
		if( _typeMap == null) {
			_typeMap = new String[] {
					"vnd.android.cursor.dir/" + authority + ".dcdocuments",
					"vnd.android.cursor.dir/" + authority+ ".document",
					"vnd.android.cursor.item/" + authority+ ".document",
					"vnd.android.cursor.item/" + authority+ ".settings"
			};
		}
		if(_contentUri ==null)
			_contentUri = Uri.parse("content://" + authority +  "/");
		if(_documentsUri ==null)
			_documentsUri = Uri.parse("content://" +authority + "/" +  SUFFIX_MAP[DOCUMENTS] );
		if(_settingsUri ==null)
			_settingsUri = Uri.parse("content://" + authority + "/" +  SUFFIX_MAP[SETTINGS] );

		if( _uRIMatcher==null) {
			UriMatcher m = new UriMatcher(UriMatcher.NO_MATCH);
			// to get definitions...
			m.addURI(authority, "dcdocuments", DC_DOCUMENTS);
			m.addURI(authority, "document", DOCUMENTS);
			m.addURI(authority, "document/#", DOCUMENT);
			m.addURI(authority, "settings", SETTINGS);
			_uRIMatcher=m;
		}
		dbhelper = new DCDataHelper(getContext());
		db = dbhelper.getWritableDatabase();
		return (db != null);
	}

	/**
	 * Handles all the dictionary searches and suggestion queries from the
	 * Search Manager. When requesting a specific word, the uri alone is
	 * required. When searching all of the dictionary for matches, the
	 * selectionArgs argument must carry the search query as the first element.
	 * All other arguments are ignored.
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		// Use the UriMatcher to see what kind of query we have and format the
		// db query accordingly
		int typeconst = _uRIMatcher.match(uri);
		if (typeconst == -1)
			throw new IllegalArgumentException("Unknown Uri: " + uri);
		Cursor c = db.query(TABLE_MAP[typeconst], projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/**
	 * This method is required in order to query the supported types. It's also
	 * useful in our own query() method to determine the type of Uri received.
	 */
	@Override
	public String getType(Uri uri) {
		int typeconst = _uRIMatcher.match(uri);
		if (typeconst == -1)
			throw new IllegalArgumentException("Unknown Uri: " + uri);
		return _typeMap[typeconst];

	}

	// Other required implementations...

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int typeconst = _uRIMatcher.match(uri);
		long rowID = db.insert(TABLE_MAP[typeconst], "", values);
		// ---if added successfully---
		if (rowID > 0) {
			Uri _uri = ContentUris.withAppendedId(Uri.withAppendedPath(_contentUri, SUFFIX_MAP[typeconst]), rowID);
			getContext().getContentResolver().notifyChange(_uri, null,false);
			return _uri;
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int typeconst = _uRIMatcher.match(uri);
		int count = db.delete(TABLE_MAP[typeconst], selection, selectionArgs);
		if( count > 0)
			getContext().getContentResolver().notifyChange(uri, null,false);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int typeconst = _uRIMatcher.match(uri);
		int count = db.update(TABLE_MAP[typeconst], values, selection, selectionArgs);
		// ---if added successfully---
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null,false);
		}
		return count;
	}
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		db.beginTransaction();  
		try {
			int typeconst = _uRIMatcher.match(uri);
			for(ContentValues cv : values) {
				db.insertOrThrow(TABLE_MAP[typeconst], "", cv);
			}
			db.setTransactionSuccessful();
			getContext().getContentResolver().notifyChange(uri, null,false);
			return values.length;
		}
		finally {
			db.endTransaction();
		}
	}
}
