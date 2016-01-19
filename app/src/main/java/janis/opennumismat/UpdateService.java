package janis.opennumismat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class UpdateService extends Service {
    AlarmManager am;
    PendingIntent pIntent2;

    public UpdateService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent intent2 = new Intent(this, UpdateReceiver.class);
        intent2.setAction("action 2");
        pIntent2 = PendingIntent.getBroadcast(this, 0, intent2, 0);
        am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 15*60*1000, 8*60*60*1000, pIntent2);

        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        am.cancel(pIntent2);
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i("Test", "Service: onTaskRemoved");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw null;
    }
}
