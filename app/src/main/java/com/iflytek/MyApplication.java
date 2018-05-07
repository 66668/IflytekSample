package com.iflytek;

import android.app.Application;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

/**
 * Created by sfs-sjy on 2018/2/11.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //讯飞语音初始化
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=5a8041fa");
    }
}
