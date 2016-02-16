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

package at.kju.datacollector.service;

import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import at.kju.datacollector.helpers.JsonHelper;
import at.kju.datacollector.client.DCDocument;
import at.kju.datacollector.helpers.MultipartUtility;
import at.kju.datacollector.client.Progress;
import at.kju.datacollector.client.SyncSettings;
import at.kju.datacollector.storage.LocalStorageSyncManager;

/**
 * Provides utility methods for communicating with the server.
 */
public class SyncFunctions {
	private static final String TAG = "SyncFunctions";
	public static final String AUTH_URI = "accesstoken";
	public static final String SYNC_URL_SUFFIX = "sync";
	private static final int MAX_BUFFER_SIZE = 20000;


	/**
	 * Executes the network requests on a separate thread.
	 *
	 * @param runnable
	 *            The runnable instance containing network mOperations to be
	 *            executed.
	 */
	public static Thread performOnBackgroundThread(final Runnable runnable) {
		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {

				}
			}
		};
		t.start();
		return t;
	}

	/**
	 * Connects to the Voiper server, authenticates the provided username and
	 * password.
	 * 
	 * @param username
	 *            The user's username
	 * @param password
	 *            The user's password
	 * @param handler
	 *            The hander instance from the calling UI thread.
	 * @param context
	 *            The context of the calling Activity.
	 * @return boolean The boolean result indicating whether the user was
	 *         successfully authenticated.
	 * @throws JSONException
	 * @throws ParseException
	 */
	public static String authenticate(String username, String password,SyncSettings s,  final Context context) throws ParseException  {
		HttpURLConnection conn=null;
		try {
			String syncUrl = s.getUrl();

			String urlParameters  = "?u=" + URLEncoder.encode(username, "UTF-8") + "&";
			urlParameters += "p=" + URLEncoder.encode(s.getPasswordHash(), "UTF-8") + "&";
			urlParameters += "d=" + URLEncoder.encode(s.getDuid(), "UTF-8") + "&";

			final URL url = new URL(syncUrl + (syncUrl.endsWith("/") ? "" : "/") + AUTH_URI + urlParameters);
			conn = (HttpURLConnection) url.openConnection();
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			int status = conn.getResponseCode();
			if (status == HttpURLConnection.HTTP_OK) {
				if (Log.isLoggable(TAG, Log.VERBOSE)) {
					Log.v(TAG, "Successful authentication");
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuffer responseBody = new StringBuffer();
				String line;
				while ((line = in.readLine()) != null)
					responseBody.append(line);
				in.close();
				String access_token =new JSONObject(responseBody.toString()).getString("access_token");
				return access_token;
			} else {
				if (Log.isLoggable(TAG, Log.VERBOSE)) {
					Log.v(TAG, "Error authenticating" + conn.getResponseMessage());
				}
				return null;
			}
		} catch (final IOException e) {
			if (Log.isLoggable(TAG, Log.VERBOSE)) {
				Log.v(TAG, "IOException when getting authtoken", e);
			}
			return null;
		} catch (final JSONException e) {
			if (Log.isLoggable(TAG, Log.VERBOSE)) {
				Log.v(TAG, "JSONException when getting authtoken", e);
			}
			return null;
		} finally {
			try {
				conn.disconnect();
			}
			catch( Exception ex) {

			}
			if (Log.isLoggable(TAG, Log.VERBOSE)) {
				Log.v(TAG, "getAuthtoken completing");
			}
		}

	}



	public static boolean sync(String username, String authtoken, String appVersion, List<DCDocument> fd, SyncSettings s, String fileRoot, Progress p, LocalStorageSyncManager lssm) {
		boolean moreToCome = true;
		String syncTS = s.getLastSyncTimestamp();
		int nFilesDone = 0;
		int nRecordsDone = 0;
		HttpURLConnection urlconn = null;
		boolean filesChanged = false;
		try {

			URL Url = new URL(s.getUrl() + (s.getUrl().endsWith("/") ? "" : "/") + SYNC_URL_SUFFIX);
			while(moreToCome) {
				moreToCome = false;
				urlconn = (HttpURLConnection) Url.openConnection();
				final MultipartUtility mphelper = new MultipartUtility(urlconn);

				urlconn.setRequestMethod("POST");
				mphelper.prepareConnection();
				mphelper.addFormField("u", username);
				mphelper.addFormField("t", authtoken);
				mphelper.addFormField("v", appVersion);
				mphelper.addFormField("duid", s.getDuid());
				mphelper.addFormField("locale", lssm.getContext().getResources().getConfiguration().locale.toString());
				mphelper.addFormField("params", (s.getParams() == null) ? null : s.getParams().toString());
				mphelper.addFormField("upload_only", "0");
				mphelper.addFormField("sync_timestamp", syncTS);

				JSONArray arr = new JSONArray();
				for (DCDocument f : fd) {
					arr.put(f.toJSON());
					JSONArray files = new JSONArray();
					if (f.getFiles() != null)
						files = f.getFiles();
					for (int i = 0; i < files.length(); i++) {
						String fil = files.getString(i);
						if (fil.trim().length() == 0)
							continue;
						File filLoc = new File(fileRoot + "/" + fil.trim());
						i++;
						mphelper.addFilePart("file_" + i, filLoc, "text/plain");
					}
					p.setFilesdone(++nFilesDone);
				}
				mphelper.addFormField("upload_documents", arr.toString());

				mphelper.finishMultipart();
				int statusCode = urlconn.getResponseCode();


				if (statusCode == HttpURLConnection.HTTP_OK) {
					// Succesfully connected to the server and authenticated.
					Log.d(TAG, "OK response");

					// write bytes to file
					byte[] buffer = new byte[MAX_BUFFER_SIZE];
					boolean jsonResponse = false;
					int recCount = 0;
					String upsyncError = null;
					InputStream is = urlconn.getInputStream();

					ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
					try {
						ZipEntry ze;
						while ((ze = zis.getNextEntry()) != null) {
							String filename = ze.getName();
							if (ze.isDirectory()) {
								new File(fileRoot + filename).mkdirs();
							} else if (ze.getName().toLowerCase().startsWith("files")) {
								filesChanged = true;
								File f = new File(fileRoot + "/"  + filename.substring(6).replace('\\','/'));
								f.getParentFile().mkdirs();
								FileOutputStream foss = new FileOutputStream (f, false);
								int count;
								while ((count = zis.read(buffer)) != -1) {
									foss.write(buffer, 0, count);
								}
								p.setFilesdone(++nFilesDone);
								ze.getTime();
								foss.close();
							} else if (filename.toLowerCase().endsWith("documents.json")) {
								//fix Kitkat, (does not work from input stream)
								File f = new File(fileRoot + "/tmpDocuments.json");
								f.getParentFile().mkdirs();
								FileOutputStream foss = new FileOutputStream (f, false);
								int count;
								while ((count = zis.read(buffer)) != -1) {
									foss.write(buffer, 0, count);
								}
								foss.close();
								InputStreamReader jis = new InputStreamReader(new FileInputStream(f.getPath()), "UTF-8");

								//now do the parsing
								//InputStreamReader jis = new InputStreamReader(zis);
								JsonReader reader = new JsonReader(jis);
								List<DCDocument> dcl = new ArrayList<DCDocument>();
								try {
									reader.beginArray();
									while (reader.hasNext()) {
										dcl.add(DCDocument.fromJSON(JsonHelper.handleObject(reader)));
										p.setRecordsdone(++nRecordsDone);
										if( dcl.size() > 50) {
											lssm.saveData(dcl, true, p);
											dcl = new ArrayList<DCDocument>();
										}

									}
									reader.endArray();
								}
								catch(Exception ex) {
									throw ex;
								}
								if( dcl.size() > 0) {
									lssm.saveData(dcl, true, p);
								}
								//and delete the temp-file
								f.delete();
							} else if (filename.toLowerCase().endsWith("sync.json")) {
								InputStreamReader jis = new InputStreamReader(zis, "UTF-8");
								JsonReader reader = new JsonReader(jis);
								JSONObject obj = JsonHelper.handleObject(reader);
								upsyncError = obj.optString("upsync_error");
								syncTS = obj.optString("sync_timestamp");

								if( p.getFilesmax() == 0 )
									p.setFilesmax(obj.optInt("files_total"));
								if( p.getRecordsmax() == 0)
									p.setRecordsmax(obj.optInt("documents_total"));

								moreToCome = !obj.optBoolean("sync_completed", false);
								if ((upsyncError == null || upsyncError.isEmpty()) && fd.size() > 0) {
									lssm.markAsSynced(fd);
								}
							}
						}
					} finally {
						zis.close();
					}

				} else {
					if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
						Log.e(TAG, "Authentication exception in syncing");
						throw new AuthenticationException();
					} else {
						Log.e(TAG, "Server error in sync: " + urlconn.getResponseMessage());
						throw new IOException();
					}
				}
			}
			s.setLastSyncTimestamp(syncTS);
			s.setLastSyncDate(new Date(lssm.getUTCDate()));
			s.setFilesChanged(filesChanged);
			lssm.setSyncSettings(s);
		} catch (Exception ex2) {
			throw new RuntimeException(ex2);
		}
		finally {
			try { urlconn.disconnect(); } catch (Exception e2) {};
		}
		return true;
	}

}
