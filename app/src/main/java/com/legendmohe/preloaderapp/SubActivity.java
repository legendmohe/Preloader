package com.legendmohe.preloaderapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.legendmohe.preloader.PreloadException;
import com.legendmohe.preloader.Preloader;


public class SubActivity extends AppCompatActivity {
    private static final String TAG = "SubActivity";

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        mTextView = findViewById(R.id.textView);

        // get result
        int preloadTaskId = getIntent().getIntExtra("preload_task_id", -1);
        if (preloadTaskId > 0) {
            Preloader.Result<String> preloadResult = Preloader.getResult(preloadTaskId);
            if (preloadResult != null) {
                // sync get
                try {
                    String content = preloadResult.get(1000);
                    mTextView.setText(content);
                    Log.d(TAG, "sync onResult() called with: content = [" + content + "]");
                } catch (PreloadException e) {
                    mTextView.setText("e=" + e.getMessage());
                    Log.d(TAG, "sync onResult() called with: e = [" + e + "]");
                }
                // async get
                preloadResult.get(new Preloader.ResultListener<String>() {
                    @Override
                    public void onResult(String result, PreloadException e) {
                        if (e == null) {
                            mTextView.setText(result);
                        } else {
                            mTextView.setText("e=" + e.getMessage());
                        }
                        Log.d(TAG, "async onResult() called with: result = [" + result + "], e = [" + e + "]");
                    }
                });
            }
            // cancel task
//            Preloader.cancel(preloadTaskId);
//            Preloader.cancelAll();
        }
    }
}
