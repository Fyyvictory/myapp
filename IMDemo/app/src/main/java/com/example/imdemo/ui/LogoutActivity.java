package com.example.imdemo.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.imdemo.CustomerHelper;
import com.example.imdemo.R;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.EMNoActiveCallException;

public class LogoutActivity extends AppCompatActivity implements View.OnClickListener {

    protected Button btnLogout;
    protected LinearLayout activityLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_logout);
        initView();
        String user = EMClient.getInstance().getCurrentUser();
        String str = "退出("+user+")账户";
        btnLogout.setText(str);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_logout) {
            logout();
        }
    }

    void logout(){
        String areLogout = "注销账户";
        String logoutting = getResources().getString(R.string.Are_logged_out);
        /*try {
            EMClient.getInstance().callManager().endCall();
        } catch (EMNoActiveCallException e) {
            e.printStackTrace();
        }*/
        CustomerHelper.getCustomerhelper().endCall();
        new AlertDialog.Builder(this).setMessage(areLogout)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, int which) {
                                EMClient.getInstance().logout(true, new EMCallBack() {
                                    @Override
                                    public void onSuccess() {
                                        CustomerHelper.getCustomerhelper().reset();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dialog.dismiss();
                                                startActivity(new Intent(LogoutActivity.this,LoginActivity.class));
                                                finish();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(int i, String s) {
                                        CustomerHelper.getCustomerhelper().reset();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dialog.dismiss();
                                                Toast.makeText(LogoutActivity.this, "操作失败，请重试", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onProgress(int i, String s) {

                                    }
                                });
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
    }

    private void initView() {
        btnLogout = (Button) findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(LogoutActivity.this);
        activityLogout = (LinearLayout) findViewById(R.id.activity_logout);
    }
}
