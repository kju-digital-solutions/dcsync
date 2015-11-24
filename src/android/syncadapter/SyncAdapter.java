/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package at.kju.datacollector.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import at.kju.datacollector.Constants;
import at.kju.datacollector.client.DCDocument;
import at.kju.datacollector.client.NetworkUtilities;
import at.kju.datacollector.client.Progress;
import at.kju.datacollector.client.SyncSettings;
import at.kju.datacollector.storage.DCDataHelper;
import at.kju.datacollector.storage.LocalStorageSyncManager;

/**
 * SyncAdapter implementation 
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private static final String TAG = "SyncAdapter";

	private final AccountManager mAccountManager;
	private final Context mContext;

	private Date mLastUpdated;
	LocalStorageSyncManager lssm =null;
	private long syncInterval = 120;

	private int mPrevProgressPercent = -1;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		mAccountManager = AccountManager.get(context);
		lssm = new LocalStorageSyncManager(mContext);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		String authtoken = null;
		Progress p = new Progress(mContext);
		try {

			// use the account manager to request the credentials
			authtoken = mAccountManager.blockingGetAuthToken(account, Constants.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);


			SyncSettings s = lssm.getSyncSettings();
			while( NetworkUtilities.sync(account, authtoken, lssm.getAppVersionString(), lssm.getUpSyncDocs(), s, lssm.getFileStorageLocation(), p, lssm) );
			p.setCompleted();
			return;

		} catch (final AuthenticatorException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "AuthenticatorException", e);
			p.setFailed(e);
		} catch (final OperationCanceledException e) {
			Log.e(TAG, "OperationCanceledExcetpion", e);
			p.setFailed(e);
		} catch (final IOException e) {
			Log.e(TAG, "IOException", e);
			syncResult.stats.numIoExceptions++;
			p.setFailed(e);
		} catch (final AuthenticationException e) {
			mAccountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE, authtoken);
			syncResult.stats.numAuthExceptions++;
			Log.e(TAG, "AuthenticationException", e);
			p.setFailed(e);
		} catch (final ParseException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "ParseException", e);
			p.setFailed(e);
		} catch (final JSONException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "JSONException", e);
			p.setFailed(e);
		} catch (final RemoteException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "RemoteException", e);
			p.setFailed(e);
		}
		catch( Exception e) {
			syncResult.stats.numParseExceptions++;
			p.setFailed(e);
			Log.e(TAG, "Exception", e);
		}
		return;
	}


}
