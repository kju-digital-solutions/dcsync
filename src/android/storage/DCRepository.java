package at.kju.datacollector.storage;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import at.kju.datacollector.Constants;
import at.kju.datacollector.client.DCDocument;
import at.kju.datacollector.client.SyncSettings;
import at.kju.datacollector.helpers.JsonCompare;

/**
 * Created by lw on 19.11.2015.
 */
public class DCRepository {
    private Context mCtx;

    public DCRepository(Context ctx) {
        mCtx = ctx;
    }
    public SyncSettings getSyncSettings() throws RemoteException, Exception{
        ContentProviderClient cp = mCtx.getContentResolver().acquireContentProviderClient(Constants.getContentAuthority(mCtx));
        Cursor c = null;
        try {
            c = cp.query(DCContentProvider._settingsUri, new String[] { DCDataHelper.JSONDATA, DCDataHelper.DEVICE_UUID}, null, null, null);

            if( ! c.moveToFirst())
                throw new Exception( "No Settings found!");
            JSONObject obj = new JSONObject(c.getString(0));
            obj.put("duid", c.getString(1));

            SyncSettings s = new SyncSettings(obj);
            c.close(); c=null;
            return s;
        } finally {
            if( c!= null)
                c.close();
            cp.release();
        }
    }
    public void setSyncSettings(SyncSettings settings) throws Exception {
        ContentProviderClient cp = mCtx.getContentResolver().acquireContentProviderClient(Constants.getContentAuthority(mCtx));
        Cursor c = null;
        try {
            c = cp.query(DCContentProvider._settingsUri, new String[] {DCDataHelper.JSONDATA}, null, null, null);
            if( ! c.moveToFirst())
                throw new RuntimeException("No Settings present!");

            ContentValues cv = new ContentValues();
            cv.put(DCDataHelper.JSONDATA, settings.toJSON().toString());
            int i = cp.update(DCContentProvider._settingsUri, cv, null, null);
            if( i == 0) throw new Exception( "not found");
        } finally {
            if( c!= null)
                c.close();
            cp.release();
        }
    }

    public List<DCDocument> searchDCDocuments(JSONObject documentFilter, String where, String[] whereParams, boolean exactMatch, int startRow, int maxResults ) throws RemoteException {
        ContentProviderClient cp = mCtx.getContentResolver().acquireContentProviderClient(Constants.getContentAuthority(mCtx));
        List<DCDocument> retList = new ArrayList<DCDocument>();
        Cursor c = null;
        try {
            List<String> params = new ArrayList<String>();
            for( String p : whereParams) {
                params.add(p);
            }
            List<Pattern> patterns = new ArrayList<Pattern>();


            c = cp.query(DCContentProvider._documentsUri, new String[] {DCDataHelper.CID, DCDataHelper.CREATOR_DUID, DCDataHelper.MODIFIED_DUID, DCDataHelper.CREATION_DATE, DCDataHelper.MODIFIED_DATE, DCDataHelper.SERVER_MODIFIED,DCDataHelper.CREATOR_USER, DCDataHelper.MODIFIED_USER, DCDataHelper.DOCUMENT, DCDataHelper.FILES, DCDataHelper.PATH, DCDataHelper.SYNC_STATE, DCDataHelper.LOCAL},
                    where , params.toArray(new String[params.size()]), null);
            int nRow = -1;
            for( c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                DCDocument fd = new DCDocument(c.getString(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4), c.getInt(5),  c.getString(6), c.getString(7), c.getString(8), c.getString(9), c.getString(10),c.getInt(11) == DCDataHelper.SYNC_STATE_DELETED, false, c.getInt(12)==1);

                if( documentFilter != null && !JsonCompare.compare(documentFilter,fd.getDocument() ))
                   continue;
                nRow++;
                if( startRow > nRow)
                    continue;
                retList.add(fd);

                if(retList.size() >= maxResults)
                    break;
            }
            c.close();

        } catch (JSONException e) {
            e.printStackTrace();
        } finally        {
            if( c!=null)
                c.close();
            cp.release();
        }
        return retList;
    }

    public List<DCDocument> countDCDocuments(JSONObject documentFilter, String where, String[] whereParams, boolean exactMatch, int startRow, int maxResults ) throws RemoteException{
        ContentProviderClient cp = mCtx.getContentResolver().acquireContentProviderClient(Constants.getContentAuthority(mCtx));
        List<DCDocument> retList = new ArrayList<DCDocument>();

        Cursor c = null;
        try {
            List<String> params = new ArrayList<String>();
            for( String p : whereParams) {
                params.add(p);
            }

            c = cp.query(DCContentProvider._documentsUri, new String[] {DCDataHelper.CID, DCDataHelper.CREATOR_DUID, DCDataHelper.MODIFIED_DUID, DCDataHelper.CREATION_DATE, DCDataHelper.MODIFIED_DATE, DCDataHelper.SERVER_MODIFIED,DCDataHelper.CREATOR_USER, DCDataHelper.MODIFIED_USER, DCDataHelper.DOCUMENT, DCDataHelper.FILES, DCDataHelper.PATH, DCDataHelper.SYNC_STATE},
                    where ,params.toArray(new String[params.size()]), null);
            int nRow = -1;
            for( c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                DCDocument fd = new DCDocument(c.getString(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4), c.getInt(5),  c.getString(6), c.getString(7), c.getString(8), c.getString(9), c.getString(10),c.getInt(11) == DCDataHelper.SYNC_STATE_DELETED, false, c.getInt(12)==1);
                if( documentFilter != null && !JsonCompare.compare(documentFilter,fd.getDocument() ))
                    continue;

                nRow++;
                if( startRow > nRow)
                    continue;
                retList.add(fd);

                if(retList.size() >= maxResults)
                    break;
            }
            c.close();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally
        {
            if( c!=null)
                c.close();
            cp.release();
        }
        return retList;
    }

    public boolean setSetting(String field, String value) throws Exception {
        ContentProviderClient cp = mCtx.getContentResolver().acquireContentProviderClient(Constants.getContentAuthority(mCtx));
        Cursor c = null;
        try {
            c = cp.query(DCContentProvider._settingsUri, new String[] {field}, null, null, null);
            if( ! c.moveToFirst())
                throw new Exception("No Settings present!");

            if( ! c.getString(0).equals(value)) {
                ContentValues cv = new ContentValues();
                cv.put(field, value);
                cp.update(DCContentProvider._settingsUri, cv, null, null);
                return true;
            }
            return false;
        } finally {
            if( c!= null)
                c.close();
            cp.release();
        }
    }


    public String getSetting(String field) throws Exception{
        ContentProviderClient cp = mCtx.getContentResolver().acquireContentProviderClient(Constants.getContentAuthority(mCtx));
        Cursor c = null;
        try {

            c = cp.query(DCContentProvider._settingsUri, new String[]{field}, null, null, null);
            if( ! c.moveToFirst())
                throw new Exception( "GetSetting: no settings  present");
            return c.getString(0);
        } finally {
            if( c!= null)
                c.close();
            cp.release();
        }
    }


    public int getDataCount(String query, String[] args) {
        SQLiteDatabase db=null;
        Cursor c = null;
        try {
            DCDataHelper dh = new DCDataHelper(mCtx);
            db = dh.getReadableDatabase();
            c = db.rawQuery(query, args);
            if( c!= null && c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            if( c!= null)
                c.close();
            if( db != null)
                db.close();
        }
        return 0;
    }


}
