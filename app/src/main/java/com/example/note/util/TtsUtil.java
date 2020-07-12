package com.example.note.util;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TtsUtil {
    private Context context;


    private static final String TAG = "SpeechUtils";
    private static TtsUtil singleton;

    public TextToSpeech textToSpeech; // TTS对象

    public static TtsUtil getInstance(Context context) {
        if (singleton == null) {
            synchronized (TtsUtil.class) {
                if (singleton == null) {
                    singleton = new TtsUtil(context);
                }
            }
        }
        return singleton;
    }

    private TtsUtil(Context context) {
        this.context = context;
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    int error = textToSpeech.setLanguage(Locale.CHINA);
                    if (error == TextToSpeech.LANG_MISSING_DATA || error == TextToSpeech.LANG_NOT_SUPPORTED) {
                        //mSpeech.setSpeechRate(1.0f)
                        Log.d(TAG, "onInit: \"设置中文语音失败\"");
                    } else {
                        Log.d(TAG, "onInit: \"初始化成功\"");
                    }
                    //textToSpeech.setPitch(1.0f);// 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
                    //textToSpeech.setSpeechRate(1.0f);
                }
                Log.d(TAG, "onInit: \"初始化\""+i);
            }
        });
    }

    public void speakText(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text,
                    TextToSpeech.QUEUE_ADD, null,"unique");
        }

    }

    public void destroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            singleton = null;
        }
    }

    public  void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }


}
