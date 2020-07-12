package com.example.note.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.example.note.NoteActivity;
import com.example.note.R;
import com.example.note.broadcast.NotificationButtonReceiver;
import com.example.note.util.NoteDbManager;
import com.example.note.util.TtsUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TtsService extends Service {//implements TextToSpeech.OnInitListener {

    private static final String TAG = "TtsService";
    private String str;
    private TextToSpeech mTts;

    public TtsService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
//        mTts = new TextToSpeech(this, this);
//        mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
//            @Override
//            public void onStart(String utteranceId) {
//
//            }
//
//            @Override
//            public void onDone(String utteranceId) {
//                Log.d(TAG, "onDone: ");
//                stopSelf();
//            }
//
//            @Override
//            public void onError(String utteranceId) {
//
//            }
//        });
//        mTts.setPitch(1.0f);// 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
//        mTts.setSpeechRate(1.0f);
//        str ="turn left please ";
        Log.d(TAG, "oncreate_service");

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int noteId = intent.getIntExtra("note", 0);
        NoteDbManager dbManager = NoteDbManager.getInstance();
        //str = intent.getStringExtra("note");
        str = dbManager.getNoteById(noteId).getContent();

        Log.d(TAG, "onStartCommand: " + intent.getStringExtra("note"));

        String type = intent.getStringExtra("type");
        Intent intent1 = new Intent(this, NoteActivity.class);
        //intent1.putExtra("noteId",noteId);
        //intent1.putExtra("type", intent.getStringExtra("type"));
        if ("note".equals(type)) {
            intent1 = new Intent(this, NoteActivity.class);
            intent1.putExtra("noteId",noteId);
        }
        PendingIntent pi = PendingIntent.getActivity(this,0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification);
        remoteViews.setTextViewText(R.id.notification_textView, str);
        Intent buttonIntent = new Intent(this, NotificationButtonReceiver.class);
        PendingIntent buttonPi = PendingIntent.getBroadcast(this, 0, buttonIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.notification_button, buttonPi);

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String time = sdf.format(date);
        remoteViews.setTextViewText(R.id.notification_date_textView, time);

        //高版本需要渠道
        if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            //只在Android O之上需要渠道，这里的第一个参数要和下面的channelId一样
            NotificationChannel notificationChannel = new NotificationChannel("1","name",NotificationManager.IMPORTANCE_HIGH);
            //如果这里用IMPORTANCE_NOENE就需要在系统的设置里面开启渠道，通知才能正常弹出
            manager.createNotificationChannel(notificationChannel);
        }
//这里的第二个参数要和上面的第一个参数一样
        Notification notification = new NotificationCompat.Builder(this,"1")
                .setContentTitle("提醒")
                .setContentText(str)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi)
                //.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCustomBigContentView(remoteViews)
               // .setContent(remoteViews)
                .build();
        manager.notify(1, notification);

        TtsUtil ttsUtil = TtsUtil.getInstance(this);
        ttsUtil.textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d(TAG, "onStart: ");
            }

            @Override
            public void onDone(String utteranceId) {
                Log.d(TAG, "onDone: ");
            }

            @Override
            public void onError(String utteranceId) {
                Log.d(TAG, "onError: ");
            }

            @Override
            public void onError(String utteranceId, int errorCode) {
                Log.d(TAG, "onError: ");
                super.onError(utteranceId, errorCode);
            }

            @Override
            public void onStop(String utteranceId, boolean interrupted) {
                Log.d(TAG, "onStop: ");
                super.onStop(utteranceId, interrupted);
            }
        });
        ttsUtil.speakText(str);
        //ttsUtil.speakText("kkkkk");
        //speakText(str);

        return super.onStartCommand(intent, flags, startId);
    }

//    @Override
//    public void onInit(int status) {
//        Log.v(TAG, "oninit");
//        if (status == TextToSpeech.SUCCESS) {
//            int result = mTts.setLanguage(Locale.CHINESE);
//            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                Log.v(TAG, "Language is not available.");
//            } else {
//                Log.d(TAG, "onInit: \"初始化成功\"");
//
//                //可确保引擎初始化成功了再执行
//                speakText(str);
//            }
//        } else {
//            Log.v(TAG, "Could not initialize TextToSpeech.");
//        }
//    }

    private void speakText(String str) {
        mTts.speak(str, TextToSpeech.QUEUE_FLUSH, null,"unique");
    }
}
