package com.example.imdemo;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.easeui.controller.EaseUI;

/**
 * Created by SH on 2016/12/27.
 */

public class MyApp extends Application {

    private static EaseUI easeUI;

    @Override
    public void onCreate() {
        super.onCreate();
        initEaseUI();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * 初始化easeUI
     */
    private synchronized void initEaseUI() {
            CustomerHelper.getCustomerhelper().init(this);
    }
}
