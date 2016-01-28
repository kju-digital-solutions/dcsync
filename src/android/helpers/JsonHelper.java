package at.kju.datacollector.helpers;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.MalformedJsonException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by lw on 19.11.2015.
 */
public class JsonHelper {

    /**
     * Handle an Object. Consume the first token which is BEGIN_OBJECT. Within
     * the Object there could be array or non array tokens. We write handler
     * methods for both. Noe the peek() method. It is used to find out the type
     * of the next token without actually consuming it.
     *
     * @param reader
     * @throws IOException
     */
    public static JSONObject handleObject(JsonReader reader) throws IOException, JSONException
    {
        JSONObject obj = new JSONObject();
        reader.beginObject();
        while (reader.hasNext()) {
            handleToken(reader, null, obj);
        }
        reader.endObject();
        return obj;
    }

    /**
     * Handle a json array. The first token would be JsonToken.BEGIN_ARRAY.
     * Arrays may contain objects or primitives.
     *
     * @param reader
     * @throws IOException
     */
    public static JSONArray handleArray(JsonReader reader) throws IOException, JSONException
    {
        JSONArray arr = new JSONArray();
        reader.beginArray();
        while (reader.hasNext()) {
            handleToken(reader, arr, null);
        }
        reader.endArray();
        return arr;
    }

    /**
     * Handle member tokens
     *
     * @param reader
     * @throws IOException
     */
    public static void handleToken(JsonReader reader,  JSONArray arr, JSONObject obj) throws IOException, JSONException
    {
        JsonToken token = reader.peek();
        String name = null;
        if (token.equals(JsonToken.NAME)) {
            name = reader.nextName();
            token = reader.peek();
        }

        if( name == null && obj!=null)
            throw new MalformedJsonException("property value without name");
        if( name != null && obj==null)
            throw new MalformedJsonException("property value with name in array");

        if (token.equals(JsonToken.STRING)) {
            if( arr != null)
                arr.put(reader.nextString());
            else
                obj.put(name, reader.nextString());
        }
        else if (token.equals(JsonToken.NUMBER)) {
            if( arr != null)
                arr.put(reader.nextDouble());
            else
                obj.put(name, reader.nextDouble());
        }
        else if (token.equals(JsonToken.NULL)) {
            reader.nextNull();
            if( arr != null)
                arr.put(null);
            else
                obj.put(name, null);
        }
        else if (token.equals(JsonToken.BOOLEAN)) {
            if( arr != null)
                arr.put(reader.nextBoolean());
            else
                obj.put(name, reader.nextBoolean());
        }
        else if (token.equals(JsonToken.BEGIN_ARRAY)) {
            if( arr != null)
                arr.put(handleArray(reader));
            else
                obj.put(name, handleArray(reader));
        }
        else if (token.equals(JsonToken.BEGIN_OBJECT)) {
            if( arr != null)
                arr.put(handleObject(reader));
            else
                obj.put(name, handleObject(reader));
        }
        else
            throw new MalformedJsonException("cannot parse " + token.name());
    }
}



