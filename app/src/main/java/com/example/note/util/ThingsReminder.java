package com.example.note.util;

import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ThingsReminder {
    private static String calenderEventURL = null;
    static {
        if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
            calenderEventURL = "content://com.android.calendar/events";
        } else {
            calenderEventURL = "content://calendar/events";
        }
    }

    public static void OpenCalendar(Context context, String content) {
//        Calendar beginTime = Calendar.getInstance();//开始时间
//        beginTime.clear();
//        beginTime.set(2021,0,1,12,0); //2014年1月1日12点0分(注意：月份0-11，24小时制)
//        Calendar endTime = Calendar.getInstance();//结束时间
//        endTime.clear();
//        endTime.set(2021,1,1,13,30); //2014年2月1日13点30分(注意：月份0-11，24小时制)
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(Uri.parse(calenderEventURL))
                .putExtra("beginTime", System.currentTimeMillis())
                .putExtra("endTime", System.currentTimeMillis() + 24*60*60*1000)
                .putExtra("title", content)
                .putExtra("description", content);
        context.startActivity(intent);
    }
}
