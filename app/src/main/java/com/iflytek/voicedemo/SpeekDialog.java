package com.iflytek.voicedemo;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.speech.util.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;


/**
 * 语音弹窗
 * 使用讯飞语音
 */

public class SpeekDialog extends Dialog {

    private Context context;
    private SpeekCallBack callBack;
    private boolean isOnlyNum;//true--只支持数字和下划线 false=支持汉字
    private boolean isStartSpeek;//是否开始、重新开始 说话
    private boolean isiflyetUI;//是否显示 讯飞的UI 此处只能设置false

    //======================全局控件======================
    private EditText tv_Result;
    private TextView tv_control, tv_sure;
    private ImageView tv_cancel, img_change;

    //======================讯飞语音设置======================
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private String mEngineType;  // 引擎类型
    private SharedPreferences mSharedPreferences;
    private SpeechRecognizer mIat; // 语音听写对象
    private RecognizerDialog mIatDialog; // 语音听写UI (SpeekDialog不使用)
    int ret = 0; //不显示弹窗的参数返回
    private boolean mTranslateEnable = false;//是否要转换
    private Toast mToast;

    /**
     * @param context
     * @param callBack
     * @param isOnlyNum //true--只支持数字和下划线 false=支持汉字
     */
    public SpeekDialog(Context context, SpeekCallBack callBack, boolean isOnlyNum) {
        this(context, callBack, isOnlyNum, false);

        this.context = context;
        this.callBack = callBack;
        this.isOnlyNum = isOnlyNum;
    }

    /**
     * @param context
     * @param callBack
     * @param isOnlyNum//true--只支持数字和下划线 false=支持汉字
     * @param isiflyetUI//是否显示           讯飞的UI(是--显示讯飞的UI,否--显示自定义UI)
     */
    public SpeekDialog(Context context, SpeekCallBack callBack, boolean isOnlyNum, boolean isiflyetUI) {
        super(context, R.style.SpeekDialog);
        this.context = context;
        this.callBack = callBack;
        this.isOnlyNum = isOnlyNum;
        this.isiflyetUI = false;
        initSpeek();
        init();
    }


    public void setSpeekCallBack(SpeekCallBack callback) {
        this.callBack = callback;
    }

    public interface SpeekCallBack {
        void onResultBack(String result);

    }


    /**
     * 初始化 讯飞语音对象
     */
    private void initSpeek() {
        mToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
        // 引擎类型
        mEngineType = SpeechConstant.TYPE_CLOUD;//连接网络类型
        if (isiflyetUI) {
            // 初始化识别无UI识别对象
            // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
            mIat = SpeechRecognizer.createRecognizer(context, new InitListener() {
                @Override
                public void onInit(int code) {
                    if (code != ErrorCode.SUCCESS) {
                        showTip("初始化失败，错误码：" + code);
                    }
                }
            });
        } else {
            // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
            // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
            mIatDialog = new RecognizerDialog(context, new InitListener() {
                @Override
                public void onInit(int code) {
                    if (code != ErrorCode.SUCCESS) {
                        showTip("初始化失败，错误码：" + code);
                    }
                }
            });
        }

        mSharedPreferences = context.getSharedPreferences(context.getPackageName(), Activity.MODE_PRIVATE);

        //初始化完成后， 设置参数
        setParam();
    }

    /**
     * 布局控制赋值
     */
    private void init() {

        //布局设置
        View dialogView = null;
        dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_speek, null);
        //
        tv_Result = dialogView.findViewById(R.id.tv_Result);
        tv_control = dialogView.findViewById(R.id.tv_control);
        tv_sure = dialogView.findViewById(R.id.tv_sure);
        tv_cancel = dialogView.findViewById(R.id.tv_cancel);
        img_change = dialogView.findViewById(R.id.img_change);
        //录音动画

        //赋值
        tv_control.setText(context.getResources().getString(R.string.recorder_Listening));
        tv_Result.setText("");

        img_change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tv_control.getText().toString().contains("正在听")) {
                    return;
                }
                startUseSpeek();
            }
        });

        tv_control.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tv_control.getText().toString().contains("正在听")) {
                    return;
                }
                startUseSpeek();
            }
        });

        //确定按钮
        tv_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callBack.onResultBack(tv_Result.getText().toString().trim());
                tv_sure.setClickable(false);
                dismiss();
            }
        });

        //取消按钮
        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        setContentView(dialogView);
        setCanceledOnTouchOutside(true);
        getWindow().setGravity(Gravity.CENTER);

        //创建dialog，直接执行语音,
        startUseSpeek();

    }

    /**
     * 运行 语音
     */
    private void startUseSpeek() {
        //清空
        tv_Result.setText("");
        mIatResults.clear();
        isStartSpeek = true;

        if (isiflyetUI) {//显示讯飞弹窗

            mIatDialog.setListener(new RecognizerDialogListener() {
                @Override
                public void onResult(RecognizerResult recognizerResult, boolean b) {
                    tv_control.setText(context.getResources().getString(R.string.recorder_restart));
                    isStartSpeek = false;
                    if (mTranslateEnable) {
                        printTransResult(recognizerResult);
                    } else {
                        printResult(recognizerResult);
                    }
                }

                @Override
                public void onError(SpeechError speechError) {
                    tv_control.setText(context.getResources().getString(R.string.recorder_restart));
                    isStartSpeek = false;
                    if (mTranslateEnable && speechError.getErrorCode() == 14002) {
                        showTip(speechError.getPlainDescription(true) + "\n请确认是否已开通翻译功能");
                    } else {
                        showTip(speechError.getPlainDescription(true));
                    }
                }
            });
            mIatDialog.show();
        } else {
            // 不显示听写对话框
            ret = mIat.startListening(new RecognizerListener() {
                @Override
                public void onVolumeChanged(int i, byte[] bytes) {
                    //音量大小(0--13级),可以控制图片变化，做一些动画效果
                    if (isStartSpeek) {
                        showSpeekAnim(i);
                    } else {
                        img_change.setBackground(ContextCompat.getDrawable(context, R.mipmap.recorder_player2));
                    }

                }

                @Override
                public void onBeginOfSpeech() {
                    tv_control.setText(context.getResources().getString(R.string.recorder_Listening));
                }

                @Override
                public void onEndOfSpeech() {
                    tv_control.setText(context.getResources().getString(R.string.recorder_restart));
                    isStartSpeek = false;
                }

                @Override
                public void onResult(RecognizerResult recognizerResult, boolean b) {
                    tv_control.setText(context.getResources().getString(R.string.recorder_restart));
                    isStartSpeek = false;
                    if (mTranslateEnable) {
                        printTransResult(recognizerResult);
                    } else {
                        printResult(recognizerResult);
                    }
                }

                @Override
                public void onError(SpeechError speechError) {
                    tv_control.setText(context.getResources().getString(R.string.recorder_restart));
                    isStartSpeek = false;
                    // Tips：
                    // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
                    // 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
                    if (mTranslateEnable && speechError.getErrorCode() == 14002) {
                        showTip(speechError.getPlainDescription(true) + "\n请确认是否已开通翻译功能");
                    } else {
                        showTip(speechError.getPlainDescription(true));
                    }
                }

                @Override
                public void onEvent(int i, int i1, int i2, Bundle bundle) {

                }
            });

            if (ret != ErrorCode.SUCCESS) {
                showTip("听写失败,错误码：" + ret);
            }
        }
    }

    /**
     * 根据音量显示图片，形成动画效果
     *
     * @param i
     */
    private void showSpeekAnim(int i) {
        if (i == 0 || i == 1 || i == 2) {
            img_change.setBackground(ContextCompat.getDrawable(context, R.mipmap.recorder_player2));//1
        } else if (i == 3 || i == 4) {
            img_change.setBackground(ContextCompat.getDrawable(context, R.mipmap.recorder_player3));//2
        } else if (i == 5 || i == 6) {
            img_change.setBackground(ContextCompat.getDrawable(context, R.mipmap.recorder_player4));//3
        } else if (i == 7 || i == 8) {
            img_change.setBackground(ContextCompat.getDrawable(context, R.mipmap.recorder_player5));//4
        } else if (i == 9 || i == 10) {
            img_change.setBackground(ContextCompat.getDrawable(context, R.mipmap.recorder_player6));//5
        } else if (i == 9 || i == 10) {
            img_change.setBackground(ContextCompat.getDrawable(context, R.mipmap.recorder_player7));//6
        } else if (i > 11) {
            img_change.setBackground(ContextCompat.getDrawable(context, R.mipmap.recorder_player8));//7
        } else {
            img_change.setBackground(ContextCompat.getDrawable(context, R.mipmap.recorder_player2)); //1
        }
    }

    /**
     * 参数设置
     *
     * @return
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎类型 --连接网络类型
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        //字音转换
        this.mTranslateEnable = mSharedPreferences.getBoolean("translate", false);
        if (mTranslateEnable) {
            mIat.setParameter(SpeechConstant.ASR_SCH, "1");
            mIat.setParameter(SpeechConstant.ADD_CAP, "translate");
            mIat.setParameter(SpeechConstant.TRS_SRC, "its");
        }
        //
        String lag = mSharedPreferences.getString("iat_language_preference", "mandarin");

        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
            mIat.setParameter(SpeechConstant.ACCENT, null);

            if (mTranslateEnable) {
                mIat.setParameter(SpeechConstant.ORI_LANG, "en");
                mIat.setParameter(SpeechConstant.TRANS_LANG, "cn");
            }
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);

            if (mTranslateEnable) {
                mIat.setParameter(SpeechConstant.ORI_LANG, "cn");
                mIat.setParameter(SpeechConstant.TRANS_LANG, "en");
            }
        }

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS,
                mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS,
                mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT,
                mSharedPreferences.getString("iat_punc_preference", "0"));

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH,
                Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }

    /**
     * 转换--语音结果处理
     *
     * @param results
     */
    private void printTransResult(RecognizerResult results) {
        String trans = JsonParser.parseTransResult(results.getResultString(), "dst");
        String oris = JsonParser.parseTransResult(results.getResultString(), "src");

        if (TextUtils.isEmpty(trans) || TextUtils.isEmpty(oris)) {
            Log.e("SJY", "解析结果失败，请确认是否已开通翻译功能。");
        } else {
            //  "原始语言:\n"+oris+"\n目标语言:\n"+trans
            filterAndShow(trans);
        }
    }

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        filterAndShow(resultBuffer.toString());

    }

    /**
     * TODO 效果不好，未限制
     * 对识别结果进一步筛选
     *
     * @param result
     */
    private void filterAndShow(String result) {
        if (isOnlyNum) { //筛选出数字
            if (result.toString().matches("^[A-Za-z0-9]+$")) {
                tv_Result.setText(result.toString());
            } else {
                tv_Result.setText("");
                showTip("语音不符合要求，请重置！");
            }

        } else {
            tv_Result.setText(result.toString());

        }
    }


    //重写，关闭资源
    @Override
    public void dismiss() {
        super.dismiss();
        if (null != mIat) {
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
    }

    @Override
    public void show() {
        super.show();
        WindowManager.LayoutParams lp = this.getWindow().getAttributes();

        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        this.getWindow().setAttributes(lp);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            dismiss();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }
}
