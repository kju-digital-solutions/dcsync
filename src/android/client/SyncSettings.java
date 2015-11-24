package at.kju.datacollector.client;

import org.json.JSONObject;

import java.util.Date;

public class SyncSettings {
	private Date lastSyncDate;
	private String lastSyncTimestamp;
	private String duid;
	private String locale;
	private String url;
	private JSONObject eventFilter;
	private JSONObject params;
	private long interval;

	public SyncSettings(Date lastSyncDate, String lastSyncTimestamp, String duid, String locale, String url, JSONObject eventFilter, JSONObject params, int interval) {
		this.lastSyncDate = lastSyncDate;
		this.lastSyncTimestamp = lastSyncTimestamp;
		this.duid = duid;
		this.locale = locale;
		this.url = url;
		this.eventFilter = eventFilter;
		this.params = params;
		this.interval = interval;
	}
	public SyncSettings(JSONObject obj) {
		this.lastSyncDate = new Date(obj.optLong("lastSyncDate", 0));
		this.lastSyncTimestamp = obj.optString("lastSyncTimestamp");
		this.duid = obj.optString("duid");
		this.locale = obj.optString("locale");
		this.url = obj.optString("url");
		this.eventFilter = obj.optJSONObject("eventFilter");
		this.params =obj.optJSONObject("params");
		this.interval = obj.optLong("interval");
	}

	public SyncSettings() {

	}
	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("lastSyncDate", getLastSyncDate());
			obj.put("lastSyncTimestamp", getLastSyncTimestamp());
			obj.put("locale", getLocale() );
			obj.put("url", getUrl() );
			obj.put("eventFilter", getEventFilter() );
			obj.put("params", getParams() );
			obj.put("interval", getInterval() );
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
