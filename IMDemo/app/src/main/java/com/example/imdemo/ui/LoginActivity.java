package com.example.imdemo.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.imdemo.R;
import com.example.imdemo.db.DbManager;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;

public class LoginActivity extends BaseActivity implements View.OnClickListener {

    protected EditText usernaemLogin;
    protected EditText userpsdLogin;
    protected Button btnRegister;
    protected Button btnLogin;
    protected String userName;
    protected String userPsd;
    protected ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_login);
        initView();

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_register) {
            turnToActivity(this, RegisterActivity.class);
        } else if (view.getId() == R.id.btn_login) {
            if (TextUtils.isEmpty(userName))
                Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show();
            else if (TextUtils.isEmpty(userPsd))
                Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show();
            else {
                showDialog();
                DbManager.getInstance(this).closeDB();

                // todo 与服务器通信,获取到服务器返回的数据再加载下面这个方法
                EMClient.getInstance().login(userName, userPsd, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        pd.dismiss();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                EMClient.getInstance().chatManager().loadAllConversations();
                                EMClient.getInstance().groupManager().loadAllGroups();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent intent = new Intent();
                                        intent.setClass(LoginActivity.this,MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                            }
                        }).start();
                    }

                    @Override
                    public void onError(int i, final String s) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.dismiss();
                                Log.e("loginError", s);
                                Toast.makeText(LoginActivity.this, "登陆失败，请重试", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onProgress(int i, String s) {

                    }
                });
                /*CustomerHelper.getCustomerhelper().logIn(userName, userPsd, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        pd.dismiss();
                        EMClient.getInstance().chatManager().loadAllConversations();
                        EMClient.getInstance().groupManager().loadAllGroups();
                        Intent intent = new Intent();
                        intent.setClass(LoginActivity.this,MainActivity.class);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(int i, String s) {

                        pd.dismiss();
                        Log.e("loginError",s);
                        Toast.makeText(LoginActivity.this, "登陆失败，请重试", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(int i, String s) {

                    }
                });*/
                /*new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            pd.dismiss();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();*/
            }
        }
    }

    private void showDialog() {
        pd = new ProgressDialog(this);
        pd.setCanceledOnTouchOutside(false);
        pd.setTitle("登陆中...");
        pd.show();
    }
// 跳转到别的activity
    private void turnToActivity(Context mContext, Class<?> cls) {
        Intent intent = new Intent();
        intent.setClass(mContext, cls);
        startActivity(intent);
    }

    private void initView() {
        usernaemLogin = (EditText) findViewById(R.id.usernaem_login);
        userpsdLogin = (EditText) findViewById(R.id.userpsd_login);
        btnRegister = (Button) findViewById(R.id.btn_register);
        btnRegister.setOnClickListener(LoginActivity.this);
        btnLogin = (Button) findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(LoginActivity.this);
        usernaemLogin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                    userpsdLogin.setText("");
            }

            @Override
            public void afterTextChanged(Editable s) {
                userName = s.toString().trim();
            }
        });
        userpsdLogin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                userPsd = s.toString().trim();
            }
        });
    }
}
