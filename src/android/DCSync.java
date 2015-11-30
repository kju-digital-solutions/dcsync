package at.kju.datacollector;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import at.kju.datacollector.client.DCDocument;
import at.kju.datacollector.client.SyncSettings;
import at.kju.datacollector.storage.LocalStorageSyncManager;
import at.kju.datacollector.syncadapter.SyncService;

/**
 * Created by lw on 16.11.2015.
 */
public class DCSync extends CordovaPlugin {

    private static final String LOG_TAG = "DCSync";
    private CallbackContext callbackContext;
    private BroadcastReceiver receiver;
    private LocalStorageSyncManager lssm;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if ("registerCallback".equals(action)) {
            if (this.callbackContext != null) {
                callbackContext.error("already bound");
                return true;
            }
            this.callbackContext = callbackContext;

            IntentFilter intentFilter = new IntentFilter(SyncService.UPDATE_INTENT);
            if (this.receiver == null) {
                this.receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        sendCallback(intent, true);
                    }
                };
                webView.getContext().registerReceiver(this.receiver, intentFilter);
            }
        }
        else if ("getLastSync".equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        JSONObject obj = new JSONObject();
                        SyncSettings s = lssm.getSyncSettings();
                        obj.put("syncDate", s.getLastSyncDate().getTime());
                        callbackContext.success(obj);
                    } catch (Exception ex) {
                        callbackContext.error(ex.toString());
                    }
                }
            });
        }
        else if ("getDocumentCount".equals(action)) {
            final String path = args.getString(0);
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        JSONObject obj = new JSONObject();
                        long unsynced = lssm.getDataCountUnsynced(path);
                        long synced = lssm.getDataCountSynced(path);
                        obj.put("count", unsynced + synced);
                        obj.put("unsynced", unsynced);
                        callbackContext.success(obj);
                    } catch (Exception ex) {
                        callbackContext.error(ex.toString());
                    }
                }
            });
        }
        else if ("getContentRootUri".equals(action)) {
            final String path = args.getString(0);
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        callbackContext.success(lssm.getFileStorageLocation());
                    } catch (Exception ex) {
                        callbackContext.error(ex.toString());
                    }
                }
            });
        }
        else if ("newDocumentCid".equals(action)) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        callbackContext.success(UUID.randomUUID().toString());
                    } catch (Exception ex) {
                        callbackContext.error(ex.toString());
                    }
                }
            });
        }
        else if ("saveDocument".equals(action)) {
            final DCDocument dcd = new DCDocument();
            dcd.setCid(args.getString(0));
            dcd.setPath(args.getString(1));
            dcd.setDocument(args.getJSONObject(2));
            dcd.setFiles(args.getJSONArray(3).join(";"));
            dcd.setLocal( args.getBoolean(4));


            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        SyncSettings settings = lssm.getSyncSettings();
                        if( dcd.getCreatorUser() == null ||  dcd.getCreatorUser().isEmpty() )
                            dcd.setCreatorUser(settings.getUsername());
                        if( dcd.getCreatorDuid() == null ||  dcd.getCreatorDuid().isEmpty())
                            dcd.setCreatorDuid(settings.getDuid());
                        if( dcd.getCreationDate() == 0)
                            dcd.setCreationDate(lssm.getUTCDate());
                        dcd.setModifiedUser(settings.getUsername());
                        dcd.setModifiedDuid(settings.getDuid());
                        dcd.setModifiedDate(lssm.getUTCDate());

                        ArrayList list = new ArrayList<DCDocument>();
                        list.add(dcd);
                        int count = lssm.saveData(list, false, null);
                        if( count != 1)
                            throw new Exception("Document could not be saved, maybe not all fields provided?");
                        callbackContext.success(lssm.getData(dcd.getCid()).toJSON());
                    } catch (Exception ex) {
                        callbackContext.error(ex.toString());
                    }
                }
            });
        }
        else if ("deleteDocument".equals(action)) {
            final String cid = args.getString(0);
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        final DCDocument dcd = lssm.getData(cid);
                        if( dcd == null) {
                            callbackContext.error("Document not found");
                            return;
                        }
                        dcd.setDeleted(true);
                        ArrayList list = new ArrayList<DCDocument>();
                        list.add(dcd);
                        lssm.saveData(list, false,  null);
                        callbackContext.success();
                    } catch (Exception ex) {
                        callbackContext.error(ex.toString());
                    }
                }
            });
        }
        else if ("setSyncOptions".equals(action)) {

            final JSONObject options = args.getJSONObject(0);

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        final SyncSettings s = lssm.getSyncSettings();
                        if( options.has("url"))
                            s.setUrl(options.getString("url"));
                        if( options.has("locale"))
                            s.setLocale(options.getString("locale"));
                        if( options.has("interval"))
                            s.setInterval(options.getLong("interval"));
                        if( options.has("username"))
                            s.setUsername(options.getString("username"));
                        if( options.has("password"))
                            s.setPassword(options.getString("password"));
                        if( options.has("params"))
                            s.setParams(options.getJSONObject("params"));
                        if( options.has("event_filter"))
                            s.setEventFilter(options.getJSONObject("event_filter"));
                        lssm.setSyncSettings(s);
                        callbackContext.success();
                    } catch (Exception ex) {
                        callbackContext.error(ex.toString());
                    }
                }
            });
        }
        else if ("searchDocuments".equals(action)) {

            final JSONObject documentFilter = args.getJSONObject(0);
            final JSONObject options = args.getJSONObject(1);

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        String where = "";
                        ArrayList<String> params = new ArrayList<String>();
                        HashMap<String, String> fields = new HashMap<String, String>();

                        if( options.has("path")) {
                            where += "path=? ";
                            params.add(options.getString("path"));
                        }
                        Iterator<String> it = documentFilter.keys();
                        while ( it.hasNext()) {
                            String field =  it.next();
                            fields.put( field, documentFilter.getString(field));
                        }
                        List<DCDocument> list = lssm.searchDCDocuments(where,params.toArray(new String[params.size()]),fields, options.optBoolean("exactMatch", false), options.optInt("skipResults", 0), options.optInt("maxResults", 100));
                        JSONArray arr = new JSONArray();
                        for( DCDocument dc : list) {
                            arr.put(dc.toJSON());
                        }
                        callbackContext.success(arr);
                    } catch (Exception ex) {
                        callbackContext.error(ex.toString());
                    }
                }
            });
        }
        else if ("performSync".equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        AccountManager am = AccountManager.get(webView.getContext());
                        Account[] accounts = am.getAccountsByType(Constants.ACCOUNT_TYPE);
                        Account account = null;
                        if (accounts.length != 0) {
                            account = accounts[0];
                        }
                        Bundle b = new Bundle();
                        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                        am.invalidateAuthToken(Constants.ACCOUNT_TYPE, AccountManager.get(webView.getContext()).peekAuthToken(account, Constants.AUTHTOKEN_TYPE));
                        ContentResolver.requestSync(account, Constants.CONTENT_AUTHORITY, b);

                        callbackContext.success();
                    } catch (Exception ex) {
                        callbackContext.error(ex.toString());
                    }
                }
            });
        }
            return true;

    }

    private void sendCallback(Intent intent, boolean keepCallback) {
        JSONObject obj = new JSONObject();
        try {
            obj = new JSONObject(intent.getStringExtra(SyncService.EXTRA_EVENT));
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        if (this.callbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
            result.setKeepCallback(keepCallback);
            this.callbackContext.sendPluginResult(result);
        }
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        lssm = new LocalStorageSyncManager(webView.getContext());
    }
}
