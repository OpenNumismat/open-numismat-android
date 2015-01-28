package janis.opennumismat;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by v.ignatov on 21.01.2015.
 */
public class DownloadJsonTask extends AsyncTask<String, Void, JSONObject> {
    protected String url;

    private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(5000 /* milliseconds */);
        conn.setConnectTimeout(1000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }

    @Override
    protected JSONObject doInBackground(String... urls) {
        try {
            InputStream stream;
            url = urls[0];
            stream = downloadUrl(url);
            ByteArrayOutputStream outString = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int current;

            try{
                while((current = stream.read(buffer)) != -1) {
                    outString.write(buffer, 0, current);
                }
            } finally {
                stream.close();
            }

            String str = new String(outString.toByteArray());

            return new JSONObject(str);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
