package com.tts.example.opencvtest;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class TestActivity extends AppCompatActivity {
    Thread mWorkerThread;
    TextView txt;
    Handler handler;
    ProgressDialog progressDialog;

    private final static int MSG_ASYNC_START = 0;
    private final static int MSG_ASYNC_END = 1;

    private Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            long currentTime = System.currentTimeMillis();
            switch (msg.what) {
                case MSG_ASYNC_START:
                    txt.append("started long running at: " + currentTime + '\n');
                    progressDialog.setMessage("Running async task");
                    progressDialog.setTitle("Working");
                    progressDialog.show();
                    return true;
                case MSG_ASYNC_END:
                    txt.append("ended long running task at: " + currentTime + '\n');
                    progressDialog.dismiss();
                    return true;
                default:
                    return false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        handler = new Handler(callback);
        progressDialog = new ProgressDialog(this);
        Button btn = (Button) findViewById(R.id.long_running_btn);
        txt = (TextView) findViewById(R.id.main_text);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWorkerThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        longRunningTask(6000);
                    }
                });
                mWorkerThread.start();
            }
        });
    }

    private void longRunningTask(long taskDuration) {
        long startTime = System.currentTimeMillis();
        handler.sendEmptyMessage(MSG_ASYNC_START);

        long currentTime = startTime;
        do {
            try {
                Thread.sleep(taskDuration);
            } catch (InterruptedException e) {
                // do nothing
            }
            currentTime = System.currentTimeMillis();
        }
        while (currentTime + startTime < taskDuration);

        handler.sendEmptyMessage(MSG_ASYNC_END);
    }
}
