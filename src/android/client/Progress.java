package at.kju.datacollector.client;

import android.accounts.AuthenticatorException;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import at.kju.datacollector.syncadapter.SyncService;

/**
 * Created by lw on 23.11.2015.
 */
public class Progress {
    private final Context ctx;
    private int recordsmax;
    private int filesdone;
    private int filesmax;
    private int recordsdone;
    private int percent;
    private String currentStep;

    public void setRecordsmax(int recordsmax) {
        this.recordsmax = recordsmax;
        setPercent(-1);
    }

    public void setFilesdone(int filesdone) {
        this.filesdone = filesdone;
        setPercent(-1);
    }

    public void setFilesmax(int filesmax) {
        this.filesmax = filesmax;
        setPercent(-1);
    }

    public void setRecordsdone(int recordsdone) {
        this.recordsdone = recordsdone;
        setPercent(-1);
    }

    public String getCurrentStep() {

        return currentStep;
    }

    public int getRecordsmax() {
        return recordsmax;
    }

    public int getFilesdone() {
        return filesdone;
    }

    public int getFilesmax() {
        return filesmax;
    }

    public int getRecordsdone() {
        return recordsdone;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        int oldperc = percent;
        if(percent== -1) {
            this.percent = (recordsmax + filesmax ) > 0 ? ((recordsdone +  filesdone)/ (filesmax + recordsmax)) : 0;
        }
        else {
            this.percent = percent;
        }
        if( oldperc != percent)
            notifyProgress();
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
        notifyProgress();
    }

    private void notifyProgress() {
        JSONObject obj = this.toJSON();
        try {
            obj.put("eventType", "onSyncProgress");
        }
        catch( JSONException ex) {
        }
        SyncService.fireEvent(ctx, obj);
    }

    public Progress(Context ctx) {
        this.ctx = ctx;
    }

    public Progress(Context ctx, int percent, String currentStep, int filesdone, int filesmax, int recordsdone, int recordsmax) {
        this.ctx = ctx;
        this.percent = percent;
        this.currentStep = currentStep;
        this.filesdone = filesdone;
        this.filesmax = filesmax;
        this.recordsdone = recordsdone;
        this.recordsmax = recordsmax;
    }

    public JSONObject toJSON()  {
        try {
            JSONObject obj = new JSONObject();
            obj.put("percent", percent);
            obj.put("current_step", currentStep);
            return obj;
        }
        catch( JSONException ex) {
            return new JSONObject();
        }
    }


    public void setFailed(Exception e) {
        JSONObject obj = this.toJSON();
        try {
            obj.put("eventType", "onSyncFailed");
            obj.put("exception", e.toString());
        }
        catch( JSONException ex) {
        }
        SyncService.fireEvent(ctx, obj);
    }


    public void setCompleted() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("eventType", "onSyncCompleted");
        }
        catch( JSONException ex) {
        }
        SyncService.fireEvent(ctx, obj);
    }
}
