package com.example.note.application;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.note.MainActivity;

import org.litepal.LitePal;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static MyApplication mInstance;
    private Activity mActivity;
    private Context context;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();
        context = getApplicationContext();
        mInstance = this;
        LitePal.initialize(getApplicationContext());
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                // 此处记录最后的activity

                Log.d(TAG, "onActivityCreated: " );
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                Log.d(TAG, "onActivityStarted: " );
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                mActivity = activity;
                Log.d(TAG, "onActivityResumed: "+ (activity instanceof MainActivity));
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                mActivity = null;
                Log.d(TAG, "onActivityPaused: ");
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                Log.d(TAG, "onActivityStopped: ");
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                Log.d(TAG, "onActivitySaveInstanceState: ");
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                Log.d(TAG, "onActivityDestroyed: ");
            }
        });
    }

    public Activity getCurrentActivity() {
        return mActivity;
    }

    public Context getContext() {
        return context;
    }

    public static MyApplication getInstance() {
        return  mInstance;
    }

}
