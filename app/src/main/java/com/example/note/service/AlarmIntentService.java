package com.example.note.service;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.example.note.MainActivity;
import com.example.note.NoteActivity;
import com.example.note.R;
import com.example.note.application.MyApplication;
import com.example.note.broadcast.NotificationButtonReceiver;
import com.example.note.db.Note;
import com.example.note.db.Todo;
import com.example.note.util.NoteDbManager;
import com.example.note.util.TodoDbManager;
import com.example.note.util.TtsUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class AlarmIntentService extends IntentService {

    private static final String TAG = "AlarmIntentService";
    private static int notificationId = 0;
    private Note note;
    private String type;

    public AlarmIntentService() {
        super("AlarmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        type = intent.getStringExtra("type");
        int id = intent.getIntExtra("note", 0);
        String str;
        PendingIntent pi = null;
        //str = intent.getStringExtra("note");
        if ("todo".equals(type)) {
            TodoDbManager todoDbManager = TodoDbManager.getInstance();
            Todo tempTodo = new Todo();
            tempTodo.setTimeRemind(-1);
            tempTodo.setStatus(1);
            todoDbManager.updateTodo(id, tempTodo);

            str = todoDbManager.getTodoContentById(id);
        } else {
            NoteDbManager dbManager = NoteDbManager.getInstance();
            //取消提醒
            Note tempNote = new Note();
            tempNote.setTimeRemind(-1);
            tempNote.setStatus(1);
            dbManager.updateNote(id, tempNote);

            note = dbManager.getNoteById(id);
            //正则表达式去掉地址
            str = note.getContent().replaceAll("<img src='(.*?)'/>","").replaceAll("<voice src='(.*?)'/>","");

            Intent intent1 = new Intent(this, NoteActivity.class);
            intent1.putExtra("noteId", id);
            //第二个参数一定要是唯一的，比如不同的ID之类的，(如果系统需要多个定时器的话)。
            pi = PendingIntent.getActivity(this, note.getId(), intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        Log.d(TAG, "onStartCommand: " + intent.getStringExtra("note"));

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

        //第一个参数一定要是唯一的，比如不同的ID之类的，(如果系统需要多个通知的话)。
        manager.notify(notificationId++, notification);

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
        ttsUtil.speakText("您设置的提醒时间到了，具体内容为" + str);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        Activity activity = MyApplication.getInstance().getCurrentActivity();
        if (activity instanceof MainActivity) {
            //更新数据，singleTop模式
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if ("note".equals(type) && activity instanceof NoteActivity) {  //处于后台时，即使当前是这个活动也会返回false
            //更新数据，singleTop模式
            Intent intent = new Intent(this, NoteActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("noteId", note.getId());
            intent.putExtra("serviceFlag",1);
            startActivity(intent);
        }
        Log.d(TAG, "onDestroy: " + (activity instanceof NoteActivity));
        super.onDestroy();
    }
}
