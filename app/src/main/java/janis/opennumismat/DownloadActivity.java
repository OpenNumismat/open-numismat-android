package janis.opennumismat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by v.ignatov on 22.10.2014.
 */
public class DownloadActivity extends AppCompatActivity {
    private static final Integer LIST_VERSION = 1;
    private static final String LIST_URL = "https://raw.githubusercontent.com/OpenNumismat/catalogues-mobile/master/list.json";
    public static final String TARGET_DIR = "OpenNumismat";

    private ArrayAdapter adapter;
    private SharedPreferences pref;

    private static class DownloadEntry {
        private final String title;
        private final String date;
        private final String size;
        private final String file_name;
        private final String url;
        public File file;

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

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        adapter = null;

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        new DownloadListTask().execute(LIST_URL);
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
        }
        else
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

    private class DownloadListTask extends DownloadJsonTask {
        @Override
        protected void onPostExecute(JSONObject json) {
            adapter = null;
            if (json == null) {
                Toast toast = Toast.makeText(
                        DownloadActivity.this, getString(R.string.could_not_download_list) + '\n' + url, Toast.LENGTH_LONG
                );
                toast.show();

                return;
            }

            List entries = new ArrayList();
            try {
                if (json.getInt("version") != LIST_VERSION)
                    return;

                String density = pref.getString("density", "XHDPI");

                JSONArray cats = json.getJSONArray("catalogues");
                for (int i = 0; i < cats.length(); i++) {
                    JSONObject cat = cats.getJSONObject(i);

                    DownloadEntry entry = new DownloadEntry(cat.getString("title"),
                            cat.getString("date"), cat.getJSONObject("size").getString(density),
                            cat.getString("file"), cat.getJSONObject("url").getString(density));
                    entries.add(entry);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            setContentView(R.layout.activity_download);
            adapter = new DownloadListAdapter(DownloadActivity.this, entries);

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
            if (result != null && !result.isEmpty()) {
                Uri uri = Uri.fromFile(new File(result));
                setResult(RESULT_OK, new Intent().setData(uri));
            }
            else {
                pd.cancel();

                Toast toast;
                if (result.isEmpty()) {
                    toast = Toast.makeText(
                            DownloadActivity.this, getString(R.string.could_not_create_file) + '\n' + entry.getFile().getPath(), Toast.LENGTH_LONG
                    );
                }
                else {
                    toast = Toast.makeText(
                            DownloadActivity.this, getString(R.string.could_not_download_file) + '\n' + entry.getUrl(), Toast.LENGTH_LONG
                    );
                }
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

                int count;
                int total = 0;
                int progress = 0;

                pd.setMax(lenghtOfFile);
                h.sendEmptyMessage(progress);
                while ((count=is.read(data)) != -1)
                {
                    total += count;
                    int temp_progress = total*100/lenghtOfFile;
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

            }catch(FileNotFoundException e){
                e.printStackTrace();
                return "";
            }catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }
    }
}
