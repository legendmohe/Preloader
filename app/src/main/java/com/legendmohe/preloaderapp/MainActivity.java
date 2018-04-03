package com.legendmohe.preloaderapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
            public void run(final Preloader.Result<String> result) throws Exception {
                FakeRestfulApi.request("hello world", new ResponseListener() {
                    @Override
                    public void onSuccess(String response) {
                        result.set(response);
                    }

                    @Override
                    public void onFail(int resCode) {
                        result.error(resCode, null);
                    }
                });
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "onCancel() called");
                FakeRestfulApi.cancel();
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

    private static class FakeRestfulApi {

        private static Thread gThread;

        static void request(final String param, final ResponseListener listener) {
            gThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5 * 1000);
                        if (listener != null) {
                            listener.onSuccess(param);
                        }
                    } catch (Exception e) {
                        listener.onFail(404);
                    }
                }
            });
            gThread.start();
        }

        static void cancel() {
            if (gThread != null) {
                gThread.interrupt();
                gThread = null;
            }
        }
    }

    private interface ResponseListener {
        void onSuccess(String response);

        void onFail(int resCode);
    }
}
