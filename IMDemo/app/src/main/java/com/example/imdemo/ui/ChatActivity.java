package com.example.imdemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;

import com.example.imdemo.R;
import com.hyphenate.easeui.ui.EaseChatFragment;
import com.hyphenate.util.EasyUtils;

import java.io.File;

/**
 * Created by SH on 2016/12/29.
 */

public class ChatActivity extends BaseActivity {

    protected FrameLayout frame;
    private EaseChatFragment mChatFragment;
    String toChatUserName;
    public static ChatActivity activityInstance;
    private File newImgFile;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        super.setContentView(R.layout.activity_chat);
        activityInstance = this;
        initView();
        toChatUserName = getIntent().getExtras().getString("userId");
        mChatFragment = ChatFragment.newInstance();
        if(arg0 != null){
            mChatFragment.cameraFile = (File) arg0.get("camerafile");
        }
        mChatFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().replace(R.id.frame, mChatFragment).commit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        newImgFile = mChatFragment.cameraFile;
        outState.putSerializable("camerafile",newImgFile);
        newImgFile = null;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mChatFragment.cameraFile = (File) savedInstanceState.getSerializable("camerafile");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityInstance = null;
    }

    private void initView() {
        frame = (FrameLayout) findViewById(R.id.frame);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String username = intent.getStringExtra("userId");
        if (toChatUserName.equals(username))
            super.onNewIntent(intent);
        else {
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        mChatFragment.onBackPressed();
        if (EasyUtils.isSingleActivity(this)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    public String getToChatUserName(){
        return toChatUserName;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    }
}
