package com.example.note.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.example.note.util.TtsUtil;


public class AlarmService extends Service {
    private static final String TAG = "AlarmService";
    private TtsUtil ttsUtil;

    public AlarmService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        ttsUtil = TtsUtil.getInstance(this);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Long date = intent.getLongExtra("date",0);//接收从Activity中传递来的值。
        Intent i = new Intent(this, AlarmIntentService.class);
        int id = intent.getIntExtra("note", 0);
        i.putExtra("note", id);
        i.putExtra("type", intent.getStringExtra("type"));


        //第二个参数一定要是唯一的，比如不同的ID之类的，(如果系统需要多个定时器的话)。这里因为有可能是todo id，所以可能产生冲突,所以以一百万为基准
        //将查询得到的note的ID值作为第二个参数,
        PendingIntent pendingIntent;
        if ("todo".equals(intent.getStringExtra("type"))) {
            pendingIntent = PendingIntent.getService(this, 1000000 + id, i, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getService(this, id, i, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        if ("cancel".equals(intent.getStringExtra("cancel"))) {
            try {
                //assert alarmManager != null;
                alarmManager.cancel(pendingIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, date, pendingIntent);
        }

        Log.d(TAG, "onStartCommand: " + intent.getIntExtra("note",0) +alarmManager);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ttsUtil.destroy();
        //在Service结束后关闭AlarmManager
//        Log.d(TAG, "onDestroy: ");
//        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
//        Intent i = new Intent(this, AlarmReceiver.class);
//        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
//        manager.cancel(pi);

    }
}
