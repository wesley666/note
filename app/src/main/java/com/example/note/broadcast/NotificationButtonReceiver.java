package com.example.note.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.note.util.TtsUtil;

public class NotificationButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationButton";
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        //throw new UnsupportedOperationException("Not yet implemented");
        Log.d(TAG, "onReceive: ");
        TtsUtil ttsUtil = TtsUtil.getInstance(context);
        ttsUtil.stop();
    }
}
