package com.example.imdemo.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.example.imdemo.R;
import com.example.imdemo.adapter.NewFriendsMsgAdapter;
import com.example.imdemo.db.InviteMessageDao;
import com.example.imdemo.entities.InviteMessage;

import java.util.List;

public class NotificationMsgActivity extends BaseActivity {

    protected RelativeLayout title;
    protected ListView newfriendList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_notifity);
        initView();
        InviteMessageDao messageDao = new InviteMessageDao(this);
        List<InviteMessage> messagesList = messageDao.getMessagesList();
        NewFriendsMsgAdapter adapter = new NewFriendsMsgAdapter(this,1,messagesList);
        newfriendList.setAdapter(adapter);
        messageDao.saveUnreadMessageCount(0);

    }

    private void initView() {
        title = (RelativeLayout) findViewById(R.id.title);
        newfriendList = (ListView) findViewById(R.id.newfriend_list);
    }

    public void back(View view){
        finish();
    }
}
