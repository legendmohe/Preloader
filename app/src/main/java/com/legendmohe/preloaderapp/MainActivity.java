package com.legendmohe.preloaderapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.legendmohe.preloader.PreloadTask;
import com.legendmohe.preloader.Preloader;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreloadTask<String> task = new PreloadTask<String>() {

            @Override
            public void run(Preloader.Result<String> result) throws Exception {
                Log.d(TAG, "run: start");
                try {
                    Thread.sleep(5*1000);
                    result.get(1000);
                    result.set("hello world");
                } catch (InterruptedException e) {
                    Log.d(TAG, "run: e=" + e);
                    throw e;
                }
                Log.d(TAG, "run: end");
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "onCancel() called");
            }
        };

        int taskId = Preloader.start(task);

        Intent intent = new Intent(this, SubActivity.class);
        intent.putExtra("preload_task_id", taskId);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
