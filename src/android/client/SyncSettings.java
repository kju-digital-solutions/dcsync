package at.kju.datacollector.client;

import android.util.Base64;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class SyncSettings {
	private Date lastSyncDate;
	private String lastSyncTimestamp;
	private String duid;
	private String username;
	private String passwordHash;
	private String locale;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String password) {
		this.passwordHash = password;
	}

	public void setPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(password.getBytes("UTF-8"));
		this.passwordHash = Base64.encodeToString(hash,Base64.DEFAULT);
	}
	private String url;
	private JSONObject eventFilter;
	private JSONObject params;
	private long interval;

	public SyncSettings(Date lastSyncDate, String lastSyncTimestamp, String duid, String locale, String url, JSONObject eventFilter, JSONObject params, int interval, String username, String passwordHash) {
		this.lastSyncDate = lastSyncDate;
		this.lastSyncTimestamp = lastSyncTimestamp;
		this.duid = duid;
		this.locale = locale;
		this.url = url;
		this.eventFilter = eventFilter;
		this.params = params;
		this.interval = interval;
		this.username = username;
		this.passwordHash = passwordHash;
	}
	public SyncSettings(JSONObject obj) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		this.lastSyncDate = new Date(obj.optLong("lastSyncDate", 0));
		this.lastSyncTimestamp = obj.optString("lastSyncTimestamp");
		this.duid = obj.optString("duid");
		this.locale = obj.optString("locale");
		this.url = obj.optString("url");
		this.eventFilter = obj.optJSONObject("eventFilter");
		this.params =obj.optJSONObject("params");
		this.interval = obj.optLong("interval");
		this.username = obj.optString("username");
		this.passwordHash = obj.optString("password_hash");
		if( obj.has("password")) {
			this.setPassword(obj.optString("password", ""));
		}
	}

	public SyncSettings() {

	}
	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("lastSyncDate", getLastSyncDate());
			obj.put("lastSyncTimestamp", getLastSyncTimestamp());
			obj.put("locale", getLocale() );
			obj.put("url", getUrl());
			obj.put("eventFilter", getEventFilter() );
			obj.put("params", getParams() );
			obj.put("interval", getInterval() );
			obj.put("username", getUsername() );
			obj.put("password_hash", getPasswordHash() );
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		return obj;
	}
	public Date getLastSyncDate() {
		return lastSyncDate;
	}

	public void setLastSyncDate(Date lastSyncDate) {
		this.lastSyncDate = lastSyncDate;
	}

	public String getLastSyncTimestamp() {
		return lastSyncTimestamp;
	}

	public void setLastSyncTimestamp(String lastSyncTimestamp) {
		this.lastSyncTimestamp = lastSyncTimestamp;
	}

	public String getDuid() {
		return duid;
	}

	public void setDuid(String duid) {
		this.duid = duid;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public JSONObject getEventFilter() {
		return eventFilter;
	}

	public void setEventFilter(JSONObject eventFilter) {
		this.eventFilter = eventFilter;
	}

	public JSONObject getParams() {
		return params;
	}

	public void setParams(JSONObject params) {
		this.params = params;
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}
}
