package com.iflytek.voicedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

/**
 * 自定义语音 demo
 */
public class MyIatDemoActivity extends Activity {
    TextView tv_dialog;
    TextView tv_dialog2;
    TextView tv_show;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_iatdemo);
        tv_show = findViewById(R.id.tv_show);
        tv_dialog2 = findViewById(R.id.tv_dialog2);
        tv_dialog = findViewById(R.id.tv_dialog);
        tv_dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //所有功能都集成都SpeekDialog中
                SpeekDialog speekDialog = new SpeekDialog(MyIatDemoActivity.this, new SpeekDialog.SpeekCallBack() {
                    @Override
                    public void onResultBack(String result) {
                        tv_show.setText(result);
                    }
                },false);
                speekDialog.show();
            }
        });
        tv_dialog2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //所有功能都集成都SpeekDialog中
                SpeekDialog speekDialog = new SpeekDialog(MyIatDemoActivity.this, new SpeekDialog.SpeekCallBack() {
                    @Override
                    public void onResultBack(String result) {
                        tv_show.setText(result);
                    }
                },false,true);
                speekDialog.show();
            }
        });
    }
}
