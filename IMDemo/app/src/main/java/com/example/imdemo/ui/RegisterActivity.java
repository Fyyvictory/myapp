package com.example.imdemo.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.imdemo.R;
import com.example.imdemo.utils.PreferanceUtils;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

public class RegisterActivity extends BaseActivity implements View.OnClickListener {

    protected EditText usernameRegister;
    protected EditText psdRegister;
    protected EditText psdvalidateRegister;
    protected String userName_register;
    protected String userPwd_register;
    protected String pwdValidate;
    protected Button btnSaveregister;
    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_register);
        initView();

    }

    private void initView() {
        usernameRegister = (EditText) findViewById(R.id.username_register);
        psdRegister = (EditText) findViewById(R.id.psd_register);
        psdvalidateRegister = (EditText) findViewById(R.id.psdvalidate_register);
        btnSaveregister = (Button) findViewById(R.id.btn_saveregister);
        btnSaveregister.setOnClickListener(RegisterActivity.this);
        usernameRegister.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                userName_register = s.toString().trim();
            }
        });

        psdRegister.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                userPwd_register = s.toString().trim();
            }
        });

        psdvalidateRegister.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                pwdValidate = s.toString().trim();
            }
        });
    }

    /**
     * 保存button的点击事件
     * @param view
     */
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_saveregister) {
            if (TextUtils.isEmpty(userName_register))
                Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show();
            else if (TextUtils.isEmpty(userPwd_register))
                Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show();
            else if (!userPwd_register.equals(pwdValidate))
                Toast.makeText(this, "密码输入不一致", Toast.LENGTH_SHORT).show();
            else {
                showDialog();
                // todo 与服务器通信
                    new Thread(){
                        @Override
                        public void run() {
                            try {
                                EMClient.getInstance().createAccount(userName_register, userPwd_register);
                                runOnUiThread(new Runnable() {
                                    private PreferanceUtils preferanceUtils;

                                    @Override
                                    public void run() {
                                        pd.dismiss();
                                        preferanceUtils = PreferanceUtils.getInstance();
                                        preferanceUtils.setCurrentUserName(userName_register);
                                        Toast.makeText(RegisterActivity.this, "请登录", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                });
                            } catch (final HyphenateException e) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        if (!RegisterActivity.this.isFinishing())
                                            pd.dismiss();
                                        int errorCode = e.getErrorCode();
                                        if (errorCode == EMError.NETWORK_ERROR) {
                                            Toast.makeText(getApplicationContext(), "网络异常", Toast.LENGTH_SHORT).show();
                                            return;
                                        } else if (errorCode == EMError.USER_ALREADY_EXIST) {
                                            Toast.makeText(getApplicationContext(), "用户已存在", Toast.LENGTH_SHORT).show();
                                            return;
                                        } else if (errorCode == EMError.USER_AUTHENTICATION_FAILED) {
                                            Toast.makeText(getApplicationContext(), "没有注册权限", Toast.LENGTH_SHORT).show();
                                            return;
                                        } else if (errorCode == EMError.USER_ILLEGAL_ARGUMENT) {
                                            Toast.makeText(getApplicationContext(), "非法用户名", Toast.LENGTH_SHORT).show();
                                            return;
                                        } else {
                                            Toast.makeText(getApplicationContext(), "注册失败", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                    }
                                });
                            }
                        }

                    }.start();
                    //CustomerHelper.getCustomerhelper().Register(userName_register,userPwd_register);
            }
        }
    }

    /**
     * 显示加载的dialog
     */
    private void showDialog() {
        pd = new ProgressDialog(this);
        pd.setTitle("正在提交...");
        pd.setCanceledOnTouchOutside(false);
        pd.show();
    }
}
