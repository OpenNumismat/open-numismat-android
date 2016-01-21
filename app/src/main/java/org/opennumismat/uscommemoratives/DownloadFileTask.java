package org.opennumismat.uscommemoratives;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by v.ignatov on 20.01.2016.
 */
public class DownloadFileTask extends AsyncTask<Void, Integer, Integer> {
    public DownloadEntry entry;
    private Context context;
    private ProgressDialog pd;
    private int lenghtOfFile;

    public DownloadFileTask(Context context, DownloadEntry entry) {
        this.context = context;
        this.entry = entry;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        pd = new ProgressDialog(context);
        pd.setMessage(context.getString(R.string.downloading));
        // меняем стиль на индикатор
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        // включаем анимацию ожидания
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();
    }

    @Override
    protected Integer doInBackground(Void... params) {
        return downloadData();
    }

    @Override
    protected void onPostExecute(Integer result) {
        pd.dismiss();

        if (result != 0) {
            String text = "";

            if (result == R.string.could_not_create_file) {
                text = context.getString(R.string.could_not_create_file) + '\n' + entry.file().getPath();
            }
            else if (result == R.string.could_not_download_file) {
                text = context.getString(R.string.could_not_download_file) + '\n' + entry.url();
            }

            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        }
    }

    private Integer downloadData(){
        try{
            URL url  = new URL(entry.url());
            URLConnection connection = url.openConnection();
            connection.connect();
            lenghtOfFile = connection.getContentLength();

            InputStream is = url.openStream();
            FileOutputStream fos = new FileOutputStream(entry.file());

            int count;
            int total = 0;
            byte data[] = new byte[1024];

            while ((count=is.read(data)) != -1)
            {
                total += count;
                publishProgress(total);

                fos.write(data, 0, count);
            }

            fos.close();
            is.close();
        }catch(FileNotFoundException e){
            e.printStackTrace();
            return R.string.could_not_create_file;
        }catch(Exception e){
            e.printStackTrace();
            return R.string.could_not_download_file;
        }

        return 0;
    }

    protected void onProgressUpdate(Integer... progress) {
        pd.setMax(lenghtOfFile);
        pd.setIndeterminate(false);
        pd.setProgress(progress[0]);
    }
}
