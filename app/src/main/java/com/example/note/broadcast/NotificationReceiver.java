package com.example.note.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.note.NoteActivity;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        //throw new UnsupportedOperationException("Not yet implemented");
        Log.d(TAG, "onReceive: ");
        int noteId = intent.getIntExtra("note", 0);
        //NoteDbManager dbManager = NoteDbManager.getInstance();
        String type = intent.getStringExtra("type");
        Intent intent1;
        if ("note".equals(type)) {
            Log.d(TAG, "onReceive: yes");
            intent1 = new Intent(context, NoteActivity.class);
            //intent1.setClassName("com.example.note","com.example.note.NoteActivity");
            //intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent1.putExtra("noteId",noteId);
            context.startActivity(intent1);
        }
    }
}
