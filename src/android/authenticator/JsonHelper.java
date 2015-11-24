package at.kju.datacollector.authenticator;

import android.util.JsonReader;
import android.util.JsonToken;

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
            JsonToken token = reader.peek();
            if (token.equals(JsonToken.END_OBJECT)) {
                reader.endObject();
                return obj;
            } else
                handleNonArrayToken(reader, token, null, obj);
        }
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
        while (true) {
            JsonToken token = reader.peek();
            if (token.equals(JsonToken.END_ARRAY)) {
                reader.endArray();
                break;
            } else if (token.equals(JsonToken.BEGIN_OBJECT)) {
                arr.put(handleObject(reader));
            } else if (token.equals(JsonToken.END_OBJECT)) {
                reader.endObject();
            } else
                handleNonArrayToken(reader, token, arr, null);
        }
        return arr;
    }

    /**
     * Handle non array non object tokens
     *
     * @param reader
     * @param token
     * @throws IOException
     */
    public static void handleNonArrayToken(JsonReader reader, JsonToken token, JSONArray arr, JSONObject obj) throws IOException, JSONException
    {
        String name = null;
        if (token.equals(JsonToken.NAME))
            name = reader.nextName();

        token = reader.peek();
        if (token.equals(JsonToken.STRING)) {
            if( arr != null)
                arr.put(reader.nextString());
            else
                obj.put(name, reader.nextString());
        }
        else if (token.equals(JsonToken.NUMBER)) {
            if( arr != null)
                arr.put(reader.nextLong());
            else
                obj.put(name, reader.nextLong());
        }
        else if (token.equals(JsonToken.BOOLEAN)) {
            if( arr != null)
                arr.put(reader.nextBoolean());
            else
                obj.put(name, reader.nextBoolean());
        }
        else
            reader.skipValue();
    }
}



