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

package at.kju.datacollector.authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import at.kju.datacollector.Constants;
import at.kju.datacollector.client.NetworkUtilities;
import at.kju.datacollector.client.SyncSettings;
import at.kju.datacollector.storage.DCDataHelper;
import at.kju.datacollector.storage.LocalStorageSyncManager;

/**
 * This class is an implementation of AbstractAccountAuthenticator for
 * authenticating accounts in the at.sicom.datacollector domain.
 */
class Authenticator extends AbstractAccountAuthenticator {
	// Authentication Service context
	private final Context mContext;

	public Authenticator(Context context) {
		super(context);
		mContext = context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
		return null;
		/*
		if (options != null && options.containsKey(AccountManager.KEY_PASSWORD)) {
			final String password = options.getString(AccountManager.KEY_PASSWORD);
			final String accessToken = onlineConfirmPassword(account.name, password);
			final Bundle result = new Bundle();
			result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, accessToken!=null);
			return result;
		}
		// Launch AuthenticatorActivity to confirm credentials
		final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
		intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
		intent.putExtra(AuthenticatorActivity.PARAM_CONFIRMCREDENTIALS, true);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;*/
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle loginOptions) {
		final Bundle result = new Bundle();
		if (!authTokenType.equals(Constants.AUTHTOKEN_TYPE)) {
			result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
			return result;
		}
		final AccountManager am = AccountManager.get(mContext);
		final String password = am.getPassword(account);
		if (password != null) {
			final String accessToken= onlineConfirmPassword(account.name, password);
			if (accessToken != null) {
				result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
				result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
				result.putString(AccountManager.KEY_AUTHTOKEN, accessToken);
				return result;
			}
		}
		result.putString(AccountManager.KEY_ERROR_MESSAGE, "no valid access token");
		//todo: mark in sync settings for app co theck?
		return result;
		/*
		// the password was missing or incorrect, return an Intent to an
		// Activity that will prompt the user for the password.
		final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
		intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
		intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		result.putParcelable(AccountManager.KEY_INTENT, intent);
		return result;*/
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAuthTokenLabel(String authTokenType) {
		if (authTokenType.equals(Constants.AUTHTOKEN_TYPE)) {
			return "tokenLabel";
			//return mContext.getString(R.string.label);
		}
		return null;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) {
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	/**
	 * Validates user's password on the server
	 */
	private String onlineConfirmPassword(String username, String password) {
		SyncSettings syncSettings= null;
		try {
			syncSettings = new LocalStorageSyncManager(mContext).getSyncSettings();
		}
		catch( Exception ex) {
			Log.w("DC-AUTHENTICATOR", ex);
		}
		return NetworkUtilities.authenticate(username, password, syncSettings, null/* Handler */, null/* Context */);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle loginOptions) {

		//todo: what to do here?
		/*final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
		intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
		intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(AuthenticatorActivity.PARAM_CONFIRMCREDENTIALS, false);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);*/
		return null;


	}

}
