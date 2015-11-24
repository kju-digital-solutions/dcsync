package at.kju.datacollector.client;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class DCDocument {

	private String cid;
	private long creationDate;
	private String creatorDuid;
	private String modifiedDuid;
	private long modifiedDate;
	private String  creatorUser;
	private long serverModified;
	private String modifiedUser;
	private String document;
	private String files;
	private String path;
	private boolean deleted;
	private boolean syncNoMedia;
	private boolean local;

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

	public String getCid() {
		return cid;
	}

	public String getCreatorUser() {
		return creatorUser;
	}

	public void setCreatorUser(String creatorUser) {
		this.creatorUser = creatorUser;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public String getCreatorDuid() {
		return creatorDuid;
	}

	public void setCreatorDuid(String creatorDuid) {
		this.creatorDuid = creatorDuid;
	}

	public String getModifiedDuid() {
		return modifiedDuid;
	}

	public void setModifiedDuid(String modifiedDuid) {
		this.modifiedDuid = modifiedDuid;
	}

	public long getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(long modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public long getServerModified() {
		return serverModified;
	}

	public void setServerModified(long serverModified) {
		this.serverModified = serverModified;
	}

	public String getModifiedUser() {
		return modifiedUser;
	}

	public void setModifiedUser(String modifiedUser) {
		this.modifiedUser = modifiedUser;
	}

	public String getDocument() {
		return document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public String getFiles() {
		return files;
	}

	public void setFiles(String files) {
		this.files = files;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isSyncNoMedia() {
		return syncNoMedia;
	}

	public void setSyncNoMedia(boolean syncNoMedia) {
		this.syncNoMedia = syncNoMedia;
	}

	public DCDocument(String cid, String creatorDuid, String modifiedDuid, long creationDate, long modifiedDate, long serverModified, String creatordUser, String modifiedUser, String document, String files, String path, boolean deleted, boolean syncNoMedia, boolean local) {
		this.cid = cid;
		this.creationDate = creationDate;
		this.creatorDuid = creatorDuid;
		this.modifiedDuid = modifiedDuid;
		this.modifiedDate = modifiedDate;
		this.serverModified = serverModified;
		this.modifiedUser = modifiedUser;
		this.creatorUser= creatorUser;
		this.document = document;
		this.files = files;
		this.path = path;
		this.deleted = deleted;
		this.syncNoMedia = syncNoMedia;
		this.local = local;
	}
	public DCDocument() {

	}
	public static DCDocument fromJSON(JSONObject json) {
		try {
			return new DCDocument(json.getString("cid"),
					json.optString("creator_duid"),
					json.optString("modified_duid"),
					json.optLong("creation_date"),
					json.optLong("modified_date"),
					json.optLong("server_modified"),
					json.optString("creator_user"),
					json.optString("modified_user"),
					json.optString("document"),
					json.optString("files"),
					json.optString("path"),
					json.optBoolean("deleted", false),
					json.optBoolean("sync_nomedia", false),
					json.optBoolean("local", false)
					);
		} catch (final Exception ex) {
			Log.i("DCDocument", "Error parsing JSON user object" + ex.toString());
		}
		return null; 
	}
	public JSONObject toJSON() throws JSONException {
		JSONObject ob = new JSONObject();
		ob.put("cid", getCid());
		ob.put("creator_duid", getCreatorDuid());
		ob.put("modified_duid", getModifiedDuid());
		ob.put("creation_date", getCreationDate());
		ob.put("modified_date", getModifiedDate());
		ob.put("server_modified", getServerModified());
		ob.put("creator_user", getCreatorUser());
		ob.put("modified_user", getModifiedUser());
		ob.put("document", getDocument());
		ob.put("files", getFiles());
		ob.put("path", getPath());
		ob.put("deleted",isDeleted());
		ob.put("sync_nomedia", isSyncNoMedia());
		ob.put("local", isLocal());

	    return ob;
	}	
}
