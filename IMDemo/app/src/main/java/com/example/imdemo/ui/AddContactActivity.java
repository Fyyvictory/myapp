package com.example.imdemo.ui;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imdemo.CustomerHelper;
import com.example.imdemo.R;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.widget.EaseAlertDialog;
import com.hyphenate.exceptions.HyphenateException;

import static android.R.color.darker_gray;

public class AddContactActivity extends BaseActivity implements View.OnClickListener {

    protected TextView addListFriends;
    protected Button search;
    protected EditText editNote;
    protected ImageView avatar;
    protected TextView name;
    protected Button indicator;
    protected String toAddUserName;
    protected RelativeLayout title;
    protected RelativeLayout toAddUserLayout;
    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_add_contact);
        initView();
    }

    private void initView() {
        addListFriends = (TextView) findViewById(R.id.add_list_friends);
        search = (Button) findViewById(R.id.search);
        search.setOnClickListener(AddContactActivity.this);
        editNote = (EditText) findViewById(R.id.edit_note);
        //avatar = (ImageView) findViewById(R.id.avatar);
        name = (TextView) findViewById(R.id.name);
        indicator = (Button) findViewById(R.id.indicator);
        indicator.setOnClickListener(AddContactActivity.this);
        title = (RelativeLayout) findViewById(R.id.title);
        avatar = (ImageView) findViewById(R.id.avatar);
        toAddUserLayout = (RelativeLayout) findViewById(R.id.ll_user);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.search) {
            searchContact();
        } else if (view.getId() == R.id.indicator) {
            addContact();
        }
    }

    public void searchContact() {
        toAddUserName = editNote.getText().toString().trim();
        if (TextUtils.isEmpty(toAddUserName)) {
            new EaseAlertDialog(this, "请输入要查找的用户名").show();
            return;
        }
        // todo 可以查找服务器中的联系人

        toAddUserLayout.setVisibility(View.VISIBLE);
        name.setText(toAddUserName);
    }

    public void addContact(){
        if(EMClient.getInstance().getCurrentUser().equals(toAddUserName)){
            new EaseAlertDialog(this,"不能添加自己为好友").show();
            return;
        }
        if(CustomerHelper.getCustomerhelper().getContactList().containsKey(toAddUserName)){
            new EaseAlertDialog(this,"你们已经是好友了").show();
            return;
        }

        pd = new ProgressDialog(this);
        pd.setMessage("正在发送请求...");
        pd.setCanceledOnTouchOutside(false);
        pd.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String s= "加个好友吧";
                try {
                    EMClient.getInstance().contactManager().addContact(toAddUserName,s);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.dismiss();
                            String ss ="请求已发送，等待验证...";
                            indicator.setBackgroundResource(android.R.color.darker_gray);
                            indicator.setClickable(false);
                            Toast.makeText(getApplicationContext(),ss , Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (HyphenateException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.dismiss();
                            String s2 = "请求发送失败，请重试";
                            Toast.makeText(getApplicationContext(), s2, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void back(View view) {
        super.back(view);
    }
}
