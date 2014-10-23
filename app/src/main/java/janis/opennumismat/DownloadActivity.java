package janis.opennumismat;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by v.ignatov on 22.10.2014.
 */
public class DownloadActivity extends Activity {
    private static final String XML_LIST_URL = "https://open-numismat-mobile.googlecode.com/files/collections.xml";
    private static final String TARGET_DIR = "OpenNumismat";

    private ArrayAdapter adapter;

    private static class DownloadEntry {
        private final String title;
        private final String date;
        private final String size;
        private final String file_name;
        private final String url;
        public File file;

        final boolean[] answer = new boolean[1];

        private DownloadEntry(String title, String date, String size, String file, String url) {
            this.title = title;
            this.date = date;
            this.size = size;
            this.file_name = file;
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public File getFile() {
            return file;
        }

        public String getDescription() {
            return file_name + ", " + size + ", " + date;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        adapter = null;

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

        @Override
        public DownloadEntry getItem(int position) {
            return (DownloadEntry)values.get(position);
        }
    }

    private static DownloadEntry entry;
    public void prepareFile(DownloadEntry entry) {
        File targetDirectory = new File(Environment.getExternalStorageDirectory() + File.separator + TARGET_DIR);
        if(!targetDirectory.exists()){
            targetDirectory.mkdir();
        }

        entry.file = new File(targetDirectory, entry.file_name);

        if (entry.file.exists()) {
            this.entry = entry;

            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setMessage(R.string.replace);
            ad.setPositiveButton(R.string.overwrite, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    startDownload(DownloadActivity.entry);
                }
            });
            ad.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    dialog.dismiss();
                }
            });
            ad.setCancelable(true);
            ad.show();

            return;
        }

        startDownload(entry);
    }

    ProgressDialog pd;
    Handler h;
    private void startDownload(DownloadEntry entry) {
        pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.downloading));
        // меняем стиль на индикатор
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        // включаем анимацию ожидания
        pd.setIndeterminate(true);
        pd.show();
        h = new Handler() {
            public void handleMessage(Message msg) {
                // выключаем анимацию ожидания
                pd.setIndeterminate(false);
                if (msg.what < pd.getMax()) {
                    pd.setProgress(msg.what);
                } else {
                    pd.dismiss();
                }
            }
        };

        new DownloadFileTask().execute(entry);
    }

    private class DownloadListTask extends AsyncTask<String, Void, List> {
        private String url;

        @Override
        protected List doInBackground(String... urls) {
            url = urls[0];
            try {
                return loadXmlFromNetwork(url);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(List result) {
            setContentView(R.layout.activity_download);
            if (result != null) {
                adapter = new DownloadListAdapter(DownloadActivity.this, result);
            }
            else {
                Toast toast = Toast.makeText(
                        DownloadActivity.this, getString(R.string.could_not_download_list) + '\n' + url, Toast.LENGTH_LONG
                );
                toast.show();

                adapter = null;
            }

            ListView lView = (ListView) findViewById(R.id.download_list);
            lView.setAdapter(adapter);
            lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1,
                                        int pos, long id
                ) {
                    if (adapter != null) {
                        DownloadEntry entry = (DownloadEntry) adapter.getItem(pos);
                        prepareFile(entry);
                    }
                }
            });
        }
    }

    private class DownloadFileTask extends AsyncTask<DownloadEntry, Void, String> {
        private DownloadEntry entry;

        @Override
        protected String doInBackground(DownloadEntry... entries) {
            entry = entries[0];
            return downloadData();
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Uri uri = Uri.fromFile(new File(result));
                setResult(RESULT_OK, new Intent().setData(uri));
            }
            else {
                Toast toast = Toast.makeText(
                        DownloadActivity.this, getString(R.string.could_not_download_file) + '\n' + entry.getUrl(), Toast.LENGTH_LONG
                );
                toast.show();

                setResult(RESULT_CANCELED);
            }

            finish();
        }

        private String downloadData(){
            try{
                URL url  = new URL(entry.getUrl());
                URLConnection connection = url.openConnection();
                connection.connect();

                int lenghtOfFile = connection.getContentLength();

                InputStream is = url.openStream();

                FileOutputStream fos = new FileOutputStream(entry.getFile());

                byte data[] = new byte[1024];

                int count = 0;
                int total = 0;
                int progress = 0;

                pd.setMax(lenghtOfFile);
                h.sendEmptyMessage(progress);
                while ((count=is.read(data)) != -1)
                {
                    total += count;
                    int temp_progress = (int)total*100/lenghtOfFile;
                    if (temp_progress != progress) {
                        progress = temp_progress;
                        h.sendEmptyMessage(total);
                    }

                    fos.write(data, 0, count);
                }
                h.sendEmptyMessage(lenghtOfFile);

                is.close();
                fos.close();

                return entry.getFile().getPath();

            }catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }
    }
}
