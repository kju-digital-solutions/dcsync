package at.kju.datacollector.service;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import at.kju.datacollector.Constants;
import at.kju.datacollector.client.Progress;
import at.kju.datacollector.client.SyncSettings;
import at.kju.datacollector.storage.LocalStorageSyncManager;

/**
 * Created by Leo on 28.01.2016.
 */
public class SyncService extends Service {
    public final static String TAG = "DCSyncService";

    public static void Schedule(Context context) {
        Intent i = new Intent(context, SyncService.class);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi); // cancel any existing alarms
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_DAY,
                AlarmManager.INTERVAL_DAY, pi);

    }


    //private PowerManager.WakeLock mWakeLock;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This is where we initialize. We call this when onStart/onStartCommand is
     * called by the system. We won't do anything with the intent here, and you
     * probably won't, either.
     */
    private void handleIntent(Intent intent) {
        // obtain the wake lock
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        //mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.getUpdateIntent(getApplicationContext()));
        //mWakeLock.acquire();

        // check the global background data setting
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean connected = false;
        try {
            connected = cm.getActiveNetworkInfo().isConnected();
        }
        catch( Exception ex) {
        }

        if (!connected) {
            Progress p = new Progress(getApplicationContext());
            p.setFailed(new RuntimeException("No Internet Connection"));
            //todo: reschedule
            stopSelf();
            return;
        }

        // do the actual work, in a separate thread
        new SyncTask().execute();
    }

    private class SyncTask extends AsyncTask<Void, Void, Void> {
        /**
         * This is where YOU do YOUR work. There's nothing for me to write here
         * you have to fill this in. Make your HTTP request(s) or whatever it is
         * you have to do to get your updates in here, because this is run in a
         * separate thread
         */
        @Override
        protected Void doInBackground(Void... params) {
            Progress p = new Progress(getApplicationContext());
            try {
                LocalStorageSyncManager lssm = new LocalStorageSyncManager(getApplicationContext());
                SyncSettings s = lssm.getSyncSettings();

                String authtoken = s.getToken();
                if( authtoken != null) {
                    authtoken = SyncFunctions.authenticate(s.getUsername(), s.getPasswordHash(), s, getApplicationContext());
                    if( authtoken == null )
                        p.setFailed(new RuntimeException("No Auth Token"));
                }
                SyncFunctions.sync( s.getUsername(), authtoken, lssm.getAppVersionString(), lssm.getUpSyncDocs(), s, lssm.getFileStorageLocation(), p, lssm);
                p.setCompleted();

            } catch (final AuthenticatorException e) {
                Log.e(TAG, "AuthenticatorException", e);
                p.setFailed(e);
            } catch (final OperationCanceledException e) {
                Log.e(TAG, "OperationCanceledExcetpion", e);
                p.setFailed(e);
            } catch (final IOException e) {
                Log.e(TAG, "IOException", e);
                p.setFailed(e);
            } catch (final AuthenticationException e) {
                Log.e(TAG, "AuthenticationException", e);
                p.setFailed(e);
            } catch (final ParseException e) {
                Log.e(TAG, "ParseException", e);
                p.setFailed(e);
            } catch (final JSONException e) {
                Log.e(TAG, "JSONException", e);
                p.setFailed(e);
            } catch (final RemoteException e) {
                Log.e(TAG, "RemoteException", e);
                p.setFailed(e);
            }
            catch( Exception e) {
                p.setFailed(e);
                Log.e(TAG, "Exception", e);
            }
            return null;
        }

        /**
         * In here you should interpret whatever you fetched in doInBackground
         * and push any notifications you need to the status bar, using the
         * NotificationManager. I will not cover this here, go check the docs on
         * NotificationManager.
         *
         * What you HAVE to do is call stopSelf() after you've pushed your
         * notification(s). This will:
         * 1) Kill the service so it doesn't waste precious resources
         * 2) Call onDestroy() which will release the wake lock, so the device
         *    can go to sleep again and save precious battery.
         */
        @Override
        protected void onPostExecute(Void result) {
            // handle your data
            stopSelf();
        }
    }


    /**
     * This is called on 2.0+ (API level 5 or higher). Returning
     * START_NOT_STICKY tells the system to not restart the service if it is
     * killed because of poor resource (memory/cpu) conditions.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_NOT_STICKY;
    }

    /**
     * In onDestroy() we release our wake lock. This ensures that whenever the
     * Service stops (killed for resources, stopSelf() called, etc.), the wake
     * lock will be released.
     */
    public void onDestroy() {
        //mWakeLock.release();
        super.onDestroy();
    }
    public static void fireEvent(Context ctx, JSONObject eventData) {
        Intent i = new Intent(Constants.getUpdateIntent(ctx));
        i.putExtra(Constants.getExtraEvent(ctx), eventData.toString());
        ctx.sendBroadcast(i);
    }
}
