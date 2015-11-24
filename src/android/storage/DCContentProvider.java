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
	private static final UriMatcher sURIMatcher = buildUriMatcher();

	private static final String[] TABLE_MAP = new String[] {  DCDataHelper.DATA_TABLE, DCDataHelper.DATA_TABLE,
			DCDataHelper.DATA_TABLE, DCDataHelper.SETTINGS_TABLE };
	private static final String[] SUFFIX_MAP = new String[] {  "dcdocuments", "document", "document", "settings" };
	private static final String[] TYPE_MAP = new String[] { 
			"vnd.android.cursor.dir/" + Constants.CONTENT_AUTHORITY + ".dcdocuments",
			"vnd.android.cursor.dir/" + Constants.CONTENT_AUTHORITY + ".document",
			"vnd.android.cursor.item/" + Constants.CONTENT_AUTHORITY + ".document",
			"vnd.android.cursor.item/" + Constants.CONTENT_AUTHORITY + ".settings"
			};

	public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.CONTENT_AUTHORITY + "/");
	public static final Uri DOCUMENTS_URI = Uri.parse("content://" + Constants.CONTENT_AUTHORITY + "/" +  SUFFIX_MAP[DOCUMENTS] );
	public static final Uri SETTINGS_URI = Uri.parse("content://" + Constants.CONTENT_AUTHORITY + "/" +  SUFFIX_MAP[SETTINGS] );

	/**
	 * Builds up a UriMatcher for search suggestion and shortcut refresh
	 * queries.
	 */
	private static UriMatcher buildUriMatcher() {
		UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		// to get definitions...
		matcher.addURI(Constants.CONTENT_AUTHORITY, "forms", DC_DOCUMENTS);
		matcher.addURI(Constants.CONTENT_AUTHORITY, "formdata", DOCUMENTS);
		matcher.addURI(Constants.CONTENT_AUTHORITY, "formdata/#", DOCUMENT);
		matcher.addURI(Constants.CONTENT_AUTHORITY, "settings", SETTINGS);
		return matcher;
	}

	@Override
	public boolean onCreate() {
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
		int typeconst = sURIMatcher.match(uri);
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
		int typeconst = sURIMatcher.match(uri);
		if (typeconst == -1)
			throw new IllegalArgumentException("Unknown Uri: " + uri);
		return TYPE_MAP[typeconst];

	}

	// Other required implementations...

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int typeconst = sURIMatcher.match(uri);
		long rowID = db.insert(TABLE_MAP[typeconst], "", values);
		// ---if added successfully---
		if (rowID > 0) {
			Uri _uri = ContentUris.withAppendedId(Uri.withAppendedPath(CONTENT_URI, SUFFIX_MAP[typeconst]), rowID);
			getContext().getContentResolver().notifyChange(_uri, null,false);
			return _uri;
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int typeconst = sURIMatcher.match(uri);
		int count = db.delete(TABLE_MAP[typeconst], selection, selectionArgs);
		if( count > 0)
			getContext().getContentResolver().notifyChange(uri, null,false);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int typeconst = sURIMatcher.match(uri);
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
			int typeconst = sURIMatcher.match(uri);
			for(ContentValues cv : values) {
				db.insert(TABLE_MAP[typeconst], "", cv);
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
