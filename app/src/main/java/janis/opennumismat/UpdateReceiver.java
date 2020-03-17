package janis.opennumismat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UpdateReceiver extends BroadcastReceiver {
    private Context context;

    public UpdateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected())
            new DownloadListTask().execute(MainActivity.UPDATE_URL);
    }


    private class DownloadListTask extends DownloadJsonTask {
        private final Integer LIST_VERSION = 1;

        @Override
        protected void onPostExecute(JSONObject json) {
            Log.i("UpdateReceiver", "onPostExecute");
            if (json == null)
                return;

            try {
                if (json.getInt("version") != LIST_VERSION)
                    return;

                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                String path = pref.getString(MainActivity.PREF_LAST_PATH, "");
                if (path.isEmpty())
                    return;

                SQLiteDatabase database;
                database = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);

                String catalog = "";
                Cursor cursor = database.rawQuery(
                        "SELECT value FROM settings WHERE title='File'",
                        new String[]{});
                if(cursor.moveToFirst()) {
                    catalog = cursor.getString(0);
                }

                Log.i("UpdateReceiver", catalog);
                JSONArray cats = json.getJSONArray("catalogues");
                for (int i = 0; i < cats.length(); i++) {
                    JSONObject cat = cats.getJSONObject(i);
                    String file = cat.getString("file");
                    if (file.equals(catalog)) {
                        JSONArray upds = cat.getJSONArray("updates");
                        for (int j = 0; j < upds.length(); j++) {
                            JSONObject upd = upds.getJSONObject(j);

                            Log.i("UpdateReceiver", upd.getString("title"));
                            if (!checkUpdate(database, upd.getString("title"))) {
                                sendNotif();
                            }
                        }
                    }
                }

                database.close();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private boolean checkUpdate(SQLiteDatabase database, String title) {
            String sql = "SELECT value FROM updates" +
                    " WHERE title=?";
            Cursor cursor = database.rawQuery(sql, new String[]{title});
            return cursor.moveToFirst();
        }
    }

    private void sendNotif() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(context.getString(R.string.available_update));

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);
        mBuilder.setContentIntent(pIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        mNotificationManager.cancelAll();
        // mId allows you to update the notification later on.
        int mId = 1;
        mNotificationManager.notify(mId, mBuilder.build());
    }
}
