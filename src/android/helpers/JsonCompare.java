package at.kju.datacollector.helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public final class JsonCompare {
    private JsonCompare() {
    }

    /**
     * Compares JSON string provided to the expected JSON string using provided comparator, and returns the results of
     */
    public static boolean compare(Object qbe, Object actual)
            throws JSONException {
        //handle nulls
        if( qbe == null) {
            return actual == null;
        }
        if( actual == null)
            return false;

        if( qbe.getClass() != actual.getClass())
            return false;
        //handle Json Object
        if ((qbe instanceof JSONObject) && (actual instanceof JSONObject)) {
            return compareJSONObject((JSONObject) qbe, (JSONObject) actual);
        }
        //handle JSon 'Array
        else if ((qbe instanceof JSONArray) && (actual instanceof JSONArray)) {
            return compareJSONArray((JSONArray) qbe, (JSONArray) actual);
        }
        else return qbe.equals(actual);

    }

    public static boolean compareJSONObject(JSONObject qbe, JSONObject actual)
            throws JSONException {
        Iterator<String> keysIterator = qbe.keys();
        while (keysIterator.hasNext())
        {
            String keyStr = (String)keysIterator.next();
            Object  qbeVal = qbe.get(keyStr);
            Object actualVal = actual.opt(keyStr);
            if( !compare(qbeVal, actualVal) )
                return false;
        }
        return true;
    }
    public static boolean compareJSONArray(JSONArray qbe, JSONArray actual)
            throws JSONException {
        if( qbe.length() != actual.length())
            return false;
        for( int i = 0; i< qbe.length();i++) {
            if( !compare(qbe.get(i), actual.get(i) ))
                return false;
        }
        return true;
    }

}