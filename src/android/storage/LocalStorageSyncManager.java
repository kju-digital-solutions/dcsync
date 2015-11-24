package at.kju.datacollector.storage;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.PeriodicSync;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import at.kju.datacollector.Constants;
import at.kju.datacollector.client.DCDocument;
import at.kju.datacollector.client.Progress;
import at.kju.datacollector.client.SyncSettings;
import at.kju.datacollector.syncadapter.SyncAdapter;

/**
 * @author Leo
 *
 */
public class LocalStorageSyncManager {
	private DCRepository  repo;
	private Context mCtx;
	private static final String TAG = "LocalStorageSyncManager";


	
	public LocalStorageSyncManager(Context ctx) {
		mCtx = ctx;
		repo = new DCRepository(ctx);
	}



	public String getAppVersionString() throws PackageManager.NameNotFoundException {
		PackageManager pm = mCtx.getPackageManager();
		return pm.getApplicationLabel(pm.getApplicationInfo(mCtx.getPackageName(), 0)) + ";" + pm.getPackageInfo(mCtx.getPackageName(), 0).versionCode;
	}

	private boolean checkStorage() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		return mExternalStorageWriteable;
	}


	public SyncSettings getSyncSettings() throws Exception{
		return repo.getSyncSettings();
	}

	public void setSyncSettings(SyncSettings settings) throws  Exception{
		SyncSettings settingsOld = repo.getSyncSettings();
		repo.setSyncSettings(settings);

		ensureAccount();

		if( settingsOld.getInterval() != settings.getInterval()) {
			syncIntervalChanged(settings);
		}
	}
	public void ensureAccount() {
		AccountManager am = AccountManager.get(mCtx);
		Account[] accounts = am.getAccountsByType(Constants.ACCOUNT_TYPE);

		if( accounts.length == 0) {
			//todo: async?
			am.addAccount(Constants.ACCOUNT_TYPE, Constants.AUTHTOKEN_TYPE, null, null, null, null, null);
		}

	}
	public void syncIntervalChanged(SyncSettings settings) {

		AccountManager am = AccountManager.get(mCtx);
		Account[] accounts = am.getAccountsByType(Constants.ACCOUNT_TYPE);
		Account account;

		if (accounts.length != 0) {
			account = accounts[0];

			List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(account, Constants.CONTENT_AUTHORITY);
			for( PeriodicSync sync :syncs) {
				ContentResolver.removePeriodicSync(account,sync.authority, sync.extras);
			}
			long interval = settings.getInterval();
			if( interval > 0) {
				Bundle params = new Bundle();
				params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false);
				params.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false);
				params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
				ContentResolver.addPeriodicSync(account,  Constants.CONTENT_AUTHORITY, params, interval/60 );
			}
		}
	}
	public Uri  getCommonDir() {
		File[] files =  new File(mCtx.getExternalFilesDir(null), Constants.COMMON_DIR).listFiles();
		if( !checkStorage() || files == null || files.length == 0)
			return Uri.parse(Constants.HTML_ROOT );
		else 
			return Uri.fromFile(new File(mCtx.getExternalFilesDir(null), Constants.COMMON_DIR + "/"));
	}
	public String getFileStorageLocation() {
		if( checkStorage() )
			return mCtx.getExternalFilesDir(null).getAbsolutePath();
		else
			return null;
	}

	public List<DCDocument> getUpSyncDocs() throws RemoteException {
		return repo.searchDCDocuments(DCDataHelper.SYNC_STATE + " in (?,?,?)" ,
				new String[]{ String.valueOf(DCDataHelper.SYNC_STATE_MODIFIED), String.valueOf(DCDataHelper.SYNC_STATE_DELETED),String.valueOf(DCDataHelper.SYNC_STATE_NEW)}
				,null, false, 0, 1000);
	}

	public void markAsSynced(List<DCDocument> docs) throws RemoteException, OperationApplicationException {
		ContentProviderClient cp = mCtx.getContentResolver().acquireContentProviderClient(Constants.CONTENT_AUTHORITY);
		ContentValues cv = new ContentValues();
		cv.put(DCDataHelper.SYNC_STATE, DCDataHelper.SYNC_STATE_SYNCED);
		ArrayList<ContentProviderOperation> updates = new ArrayList<ContentProviderOperation>();

		for( DCDocument fd : docs) {
			updates.add(ContentProviderOperation.newUpdate(DCContentProvider.DOCUMENTS_URI).withSelection(DCDataHelper.CID + "=?", new String[]{fd.getCid()}).withValues(cv).build());
		}
		cp.applyBatch(updates);
	}


	public void saveData(List<DCDocument> dcDocs, boolean fromServer, Progress p ) throws Exception {
		Cursor c = null;
		List<DCDocument> fdlist = new ArrayList<DCDocument>(); //copy list
		fdlist.addAll(dcDocs);
		
		ContentProviderClient cp = mCtx.getContentResolver().acquireContentProviderClient(Constants.CONTENT_AUTHORITY);
		String deviceUUID = repo.getSetting(DCDataHelper.DEVICE_UUID);
		HashMap<String, Integer> existing = new HashMap<String, Integer>();
		ArrayList<ContentProviderOperation> todelete = new ArrayList<ContentProviderOperation>();
		try {
			//remember deletes
			ArrayList<String>  whereArgs = new ArrayList<String>(100);
			for( int j = fdlist.size()-1; j >=0; j--) {
				if( fdlist.get(j).isDeleted()) {
					todelete.add( ContentProviderOperation.newDelete(DCContentProvider.DOCUMENTS_URI).withSelection(DCDataHelper.CID + "=?", new String[]{fdlist.get(j).getCid()}).build());
					fdlist.remove(j);
				}
			}
			
			int i = 0;
			String where = DCDataHelper.CID + " in(";
			for( DCDocument fd:fdlist) {
				where += "?,";
				whereArgs.add(fd.getCid());
				if( i % 100 == 0 || i == fdlist.size()-1) {
					String[] args = new String[whereArgs.size()];
					whereArgs.toArray(args);
					c =cp.query(DCContentProvider.DOCUMENTS_URI, new String [] {DCDataHelper.CID}, where.substring(0,where.length()-1) + ")", args, null);
					for( c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
						existing.put(c.getString(0), 1);
					}
					c.close();
					c = null;
					where = DCDataHelper.CID + " in(";
					whereArgs = new ArrayList<String>(100);
				}
				i++;
			}
			i=0;
			ArrayList<ContentProviderOperation> updates = new ArrayList<ContentProviderOperation>();
			ArrayList<ContentValues> insertValues = new ArrayList<ContentValues>();

			for( DCDocument fd:fdlist) {
				ContentValues cv = new ContentValues();
				cv.put(DCDataHelper.DOCUMENT, fd.getDocument());
				cv.put(DCDataHelper.PATH, fd.getPath());
				cv.put(DCDataHelper.LOCAL, fd.isLocal() ? 1 : 0);
				cv.put(DCDataHelper.FILES, fd.getFiles());
				if( fromServer) {
					cv.put(DCDataHelper.MODIFIED_DATE, fd.getModifiedDate());
					cv.put(DCDataHelper.SERVER_MODIFIED, fd.getServerModified());
					cv.put(DCDataHelper.CREATION_DATE, fd.getCreationDate());
				}
				else {
					cv.put(DCDataHelper.MODIFIED_DATE, new Date().getTime());
				}
				if( !existing.containsKey(fd.getCid())) {
					cv.put(DCDataHelper.CID, fd.getCid());
					cv.put(DCDataHelper.CREATION_DATE, fromServer ? fd.getCreationDate() : new Date().getTime());
					cv.put(DCDataHelper.CREATOR_DUID, fromServer ? fd.getCreatorDuid() :  deviceUUID);
					cv.put(DCDataHelper.SYNC_STATE, fromServer ? DCDataHelper.SYNC_STATE_SYNCED : DCDataHelper.SYNC_STATE_NEW);
					insertValues.add(cv);
				}
				else {
					cv.put(DCDataHelper.SYNC_STATE, (fromServer && !fd.isLocal()) ? DCDataHelper.SYNC_STATE_SYNCED :  DCDataHelper.SYNC_STATE_MODIFIED  );
					updates.add( ContentProviderOperation.newUpdate(DCContentProvider.DOCUMENTS_URI).withSelection(DCDataHelper.CID + "=?", new String[]{fd.getCid()}).withValues(cv).build());
				}
				i++;
				if( p!= null)
					p.setRecordsdone(p.getRecordsdone() + 1);
				
			}
			//update
			if( updates.size() > 0) {
				cp.applyBatch(updates);
			}
			//delete
			if( todelete.size() > 0) {
				cp.applyBatch(todelete);
			}
			//insert
			if( insertValues.size() > 0) {
				ContentValues[] iv = new ContentValues[insertValues.size()];
				cp.bulkInsert(DCContentProvider.DOCUMENTS_URI, insertValues.toArray(iv));
			}
		}
		catch(Exception ex) {
			throw ex;
		} finally {
			if( c!= null)
				c.close();
			cp.release();
		}		
	}
	
	public DCDocument getData(String uuid) throws Exception {
		List<DCDocument> retlist = repo.searchDCDocuments(DCDataHelper.CID + "=?", new String[] {uuid},null, false, 0, 1 );
		if( retlist == null || retlist.size() == 0)
			return null;
		return retlist.get(0);
	}
	public List<DCDocument>  searchDCDocuments(String where, String[] whereParams, HashMap<String, String> documentSearchmap, boolean exactMatch, int startRow, int maxResults ) throws RemoteException{
		return repo.searchDCDocuments(where, whereParams, documentSearchmap, exactMatch, startRow, maxResults);

	}

	public int getDataCountUnsynced(String category) {
		final String QUERY = "select count(*) from " + DCDataHelper.DATA_TABLE + " where " +DCDataHelper.PATH + " =? and (" + DCDataHelper.SYNC_STATE + "=? or " + DCDataHelper.SYNC_STATE + "=?)";
		return repo.getDataCount(QUERY, new String[]{category, String.valueOf(DCDataHelper.SYNC_STATE_NEW), String.valueOf(DCDataHelper.SYNC_STATE_MODIFIED)});
	}
	public int getDataCountSynced(String category) {
		final String QUERY = "select count(*) from " + DCDataHelper.DATA_TABLE + " where " +DCDataHelper.PATH + " =? and (" + DCDataHelper.SYNC_STATE + "=?)";
		return repo.getDataCount(QUERY, new String[]{category, String.valueOf(DCDataHelper.SYNC_STATE_SYNCED)});
	}

	public Context getContext() {
		return mCtx;
	}

	/*public String storePicture(String path, String filename, int imgSizeX, int imgSizeY, Integer resizeFlags, InputStream is) throws IOException {
		String relPath =  path + "/" + filename;
		String fullFile = getFileStorageLocation() + "/" +  relPath;
		File file = new File(fullFile);
		new File(file.getParent()).mkdirs();


		OutputStream os = new FileOutputStream(file);
		byte[] buffer = new byte[100000];
		BufferedInputStream bufferedInput = new BufferedInputStream(is,100000);

		int bytesRead = 0;
		while ((bytesRead = bufferedInput.read(buffer)) != -1)
			os.write(buffer, 0, bytesRead);
		bufferedInput.close();
		os.close();

		ImageHelper.resizeImage(file, file, imgSizeX, imgSizeY);

		return relPath;
	}*/

}