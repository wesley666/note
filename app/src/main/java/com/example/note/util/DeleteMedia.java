package com.example.note.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.MODE_PRIVATE;

public class DeleteMedia {

    public static void deleteMedia(Context context, String noteContent) {
        SharedPreferences cloudService = context.getSharedPreferences("CloudService", MODE_PRIVATE);
        String userName = cloudService.getString("UserName", "");
        String userPassword = cloudService.getString("UserPassword", "");
        SynWithWebDav synWithWebDav = new SynWithWebDav(userName, userPassword);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Pattern voice = Pattern.compile("<voice src='(.*?)'/>");
                Matcher mVoice = voice.matcher(noteContent);

                //find the voice
                while (mVoice.find()) {
                    String voiceName = mVoice.group(1);
                    File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES) + "/" + voiceName);
                    file.delete();
                    synWithWebDav.deleteFile(voiceName, "media");
                }

                Pattern img = Pattern.compile("<img src='(.*?)'/>");
                Matcher mImg = img.matcher(noteContent);

                //查找图片
                while (mImg.find()) {
                    String imgName = mImg.group(1);
                    File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + imgName);
                    file.delete();
                    synWithWebDav.deleteFile(imgName, "media");
                }
            }
        }).start();
    }
}
