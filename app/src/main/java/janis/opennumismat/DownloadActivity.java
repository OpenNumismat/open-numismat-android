package janis.opennumismat;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by v.ignatov on 22.10.2014.
 */
public class DownloadActivity extends Activity {
    private static final String XML_LIST_URL = "https://open-numismat-mobile.googlecode.com/files/collections.xml";

    private static class DownloadEntry {
        private final String title;
        private final String date;
        private final String size;
        private final String file;
        private final String url;

        private DownloadEntry(String title, String date, String size, String file, String url) {
            this.title = title;
            this.date = date;
            this.size = size;
            this.file = file;
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return file + ", " + size + ", " + date;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        new DownloadListTask().execute(XML_LIST_URL);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private List loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        List entries = null;

        try {
            //this will be used in reading the data from the internet
            stream = downloadUrl(XML_LIST_URL);

            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(stream, null);
            parser.nextTag();

            entries = readFeed(parser);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        return entries;
    }

    private static final String ns = null;
    private List readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List entries = new ArrayList();

        parser.require(XmlPullParser.START_TAG, ns, "collections");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("collection")) {
                DownloadEntry entry = readEntry(parser);
                if (entry != null)
                    entries.add(entry);
            } else {
                skip(parser);
            }
        }
        return entries;
    }
    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    private DownloadEntry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "collection");
        String title = null;
        String date = null;
        String size = null;
        String file = null;
        String url = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
                title = readText(parser, "title");
            } else if (name.equals("date")) {
                date = readText(parser, "date");
            } else if (name.equals("size")) {
                size = readText(parser, "size");
            } else if (name.equals("file")) {
                file = readText(parser, "file");
            } else if (name.equals("url")) {
                url = readText(parser, "url");
            } else {
                skip(parser);
            }
        }

        if (title != null)
            return new DownloadEntry(title, date, size, file, url);
        else
            return null;
    }
    // For the tags title and summary, extracts their text values.
    private String readText(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        String result = "";

        parser.require(XmlPullParser.START_TAG, ns, tag);
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, ns, tag);

        return result;
    }
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
    private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }

    private class DownloadListAdapter extends ArrayAdapter<DownloadEntry> {
        private final Context context;
        private final List values;

        public DownloadListAdapter(Context context, List values) {
            super(context, R.layout.download_item, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.download_item, null);
            }

            DownloadEntry entry = (DownloadEntry)values.get(position);

            TextView title = (TextView) convertView.findViewById(R.id.title);
            title.setText(entry.getTitle());
            TextView description = (TextView) convertView.findViewById(R.id.description);
            description.setText(entry.getDescription());

            return convertView;
        }
    }

    private class DownloadListTask extends AsyncTask<String, Void, List> {
        @Override
        protected List doInBackground(String... urls) {
            try {
                return loadXmlFromNetwork(urls[0]);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(List result) {
            if (result != null) {
                setContentView(R.layout.activity_download);
                ListView lView = (ListView) findViewById(R.id.download_list);
                lView.setAdapter(new DownloadListAdapter(DownloadActivity.this, result));
            }
            else {
                Toast toast = Toast.makeText(
                        DownloadActivity.this, getString(R.string.could_not_download_list) + '\n' + XML_LIST_URL, Toast.LENGTH_LONG
                );
                toast.show();
            }
        }
    }
}
