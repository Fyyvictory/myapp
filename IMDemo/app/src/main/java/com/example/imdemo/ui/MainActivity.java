package com.example.imdemo.ui;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imdemo.CustomerHelper;
import com.example.imdemo.R;
import com.example.imdemo.db.InviteMessageDao;
import com.example.imdemo.db.UserDao;
import com.example.imdemo.entities.Constant;
import com.example.imdemo.runtimepermissions.PermissionManager;
import com.example.imdemo.runtimepermissions.PermissionsResultAction;
import com.hyphenate.EMContactListener;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.util.NetUtils;

import java.util.List;

public class MainActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener, View.OnClickListener {

    protected RadioButton radioContact;
    protected RadioButton radioConnect;
    protected FrameLayout frame;
    protected RadioGroup radiogroup;
    protected ImageButton imgAdd;
    protected TextView unreadAddressLable;
    protected TextView unreadLabel;
    protected ImageButton btnLogout;
    private FragmentManager fragmentManager;
    private ContactFragment contactFra;
    private ConnectFragment connectFra;
    protected EMMessageListener messageListener;
    protected LocalBroadcastManager broadcastManager;
    protected BroadcastReceiver broadcastReceiver;

    /**
     * check if current user account was remove
     *//*
    public boolean getCurrentAccountRemoved() {
        return isCurrentAccountRemoved;
    }
*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
        super.setContentView(R.layout.activity_main);
        requestPermissions();
        initView();

        inviteMessgeDao = new InviteMessageDao(this);
        UserDao userDao = new UserDao(this);
        EMClient.getInstance().chatManager().loadAllConversations();
        EMClient.getInstance().groupManager().loadAllGroups();

        //register broadcast receiver to receive the change of group from DemoHelper
        registerBroadcastReceiver();
        EMClient.getInstance().contactManager().setContactListener(new MyContactListener());
    }

    private void initView() {
        radioContact = (RadioButton) findViewById(R.id.radio_contact);
        radioConnect = (RadioButton) findViewById(R.id.radio_connect);
        frame = (FrameLayout) findViewById(R.id.frame);
        radiogroup = (RadioGroup) findViewById(R.id.radiogroup);
        initFragment();
        radiogroup.setOnCheckedChangeListener(this);
        radioContact.setChecked(true);
        imgAdd = (ImageButton) findViewById(R.id.img_add);
        imgAdd.setOnClickListener(MainActivity.this);
        unreadAddressLable = (TextView) findViewById(R.id.unread_address_number);
        unreadLabel = (TextView) findViewById(R.id.unread_msg_number);
        btnLogout = (ImageButton) findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(MainActivity.this);
    }

    private void initFragment() {
        fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        contactFra = ContactFragment.newInstance();
        fragmentTransaction.add(R.id.frame, contactFra);
        connectFra = ConnectFragment.newInstance();
        fragmentTransaction.add(R.id.frame, connectFra);
        fragmentTransaction.show(ContactFragment.newInstance());
        fragmentTransaction.commit();
        connectFra.setDelegate(new ConnectFragment.Delegate() {
            @Override
            public void hideUnreadDot() {
                unreadAddressLable.setVisibility(View.GONE);
            }
        });
    }

    private void hideAllFragment(FragmentTransaction fragmentTransaction) {
        fragmentTransaction.hide(connectFra).hide(contactFra);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceiver();
    }

    private void registerBroadcastReceiver() {
        broadcastManager = LocalBroadcastManager.getInstance(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_CONTACT_CHANAGED);
        intentFilter.addAction(Constant.ACTION_GROUP_CHANAGED);
        //intentFilter.addAction(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION);
        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                updateUnreadLabel();
                updateUnreadAddressLable();
                if (radioContact.isChecked()) {
                    // refresh conversation list
                    if (contactFra != null) {
                        contactFra.refresh();
                    }
                } else if (radioConnect.isChecked()) {
                    if (connectFra != null) {
                        connectFra.refresh();
                    }
                }
                String action = intent.getAction();
                if (action.equals(Constant.ACTION_GROUP_CHANAGED)) {
                    if (EaseCommonUtils.getTopActivity(MainActivity.this).equals(GroupsActivity.class.getName())) {
                        GroupsActivity.instance.onResume();
                    }
                }
                //red packet code : 处理红包回执透传消息
                /*if (action.equals(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION)){
                    if (conversationListFragment != null){
                        conversationListFragment.refresh();
                    }
                }*/
                //end of red packet code
            }
        };
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        hideAllFragment(fragmentTransaction);
        if (checkedId == R.id.radio_connect)
            fragmentTransaction.show(connectFra);
        else if (checkedId == R.id.radio_contact)
            fragmentTransaction.show(contactFra);
        else
            Toast.makeText(this, "异常错误", Toast.LENGTH_SHORT).show();
        fragmentTransaction.commit();
        messageListener = new EMMessageListener() {

            @Override
            public void onMessageReceived(List<EMMessage> messages) {
                // notify new message
                for (EMMessage message : messages) {
                    CustomerHelper.getCustomerhelper().getNotifier().onNewMsg(message);
                }
                refreshUIWithMessage();
            }

            @Override
            public void onCmdMessageReceived(List<EMMessage> messages) {
               /* //red packet code : 处理红包回执透传消息
                for (EMMessage message : messages) {
                    EMCmdMessageBody cmdMsgBody = (EMCmdMessageBody) message.getBody();
                    final String action = cmdMsgBody.action();//获取自定义action
                    if (action.equals(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION)) {
                        RedPacketUtil.receiveRedPacketAckMessage(message);
                    }
                }
                //end of red packet code
                refreshUIWithMessage();*/
            }

            @Override
            public void onMessageReadAckReceived(List<EMMessage> messages) {
            }

            @Override
            public void onMessageDeliveryAckReceived(List<EMMessage> message) {
            }

            @Override
            public void onMessageChanged(EMMessage message, Object change) {
            }
        };
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.img_add) {
            //done 跳转到添加新朋友的界面
            if (NetUtils.hasNetwork(this)) {
                startActivity(new Intent(this, AddContactActivity.class));
            }
        } else if (view.getId() == R.id.btn_logout) {
            startActivity(new Intent(this,LogoutActivity.class));
        }
    }

    @Override
    public void back(View view) {
        super.back(view);
    }

    public class MyContactListener implements EMContactListener {
        @Override
        public void onContactAdded(String username) {
        }

        @Override
        public void onContactDeleted(final String username) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (ChatActivity.activityInstance != null && ChatActivity.activityInstance.toChatUserName != null &&
                            username.equals(ChatActivity.activityInstance.toChatUserName)) {
                        String st10 = getResources().getString(R.string.have_you_removed);
                        Toast.makeText(MainActivity.this, ChatActivity.activityInstance.getToChatUserName() + st10, Toast.LENGTH_LONG)
                                .show();
                        ChatActivity.activityInstance.finish();
                    }
                }
            });
        }

        @Override
        public void onContactInvited(String username, String reason) {
        }

        @Override
        public void onContactAgreed(String username) {
        }

        @Override
        public void onContactRefused(String username) {
        }
    }

    private void unregisterBroadcastReceiver() {
        broadcastManager.unregisterReceiver(broadcastReceiver);
    }

    @TargetApi(23)
    private void requestPermissions() {
        PermissionManager.getInstance().requestAllManifestPermissionsIfNecessary(this, new PermissionsResultAction() {
            @Override
            public void onGranted() {
//				Toast.makeText(MainActivity.this, "All permissions have been granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDenied(String permission) {
                Toast.makeText(MainActivity.this, "Permission " + permission + " has been denied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        CustomerHelper sdkHelper = CustomerHelper.getCustomerhelper();
        sdkHelper.pushActivity(this);
        EMClient.getInstance().chatManager().addMessageListener(messageListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EMClient.getInstance().chatManager().removeMessageListener(messageListener);
        CustomerHelper sdkHelper = CustomerHelper.getCustomerhelper();
        sdkHelper.popActivity(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            //return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * update unread message count
     */
    public void updateUnreadLabel() {
        int count = getUnreadMsgCountTotal();
        if (count > 0) {
            unreadLabel.setText(String.valueOf(count));
            unreadLabel.setVisibility(View.VISIBLE);
        } else {
            unreadLabel.setVisibility(View.INVISIBLE);
        }
    }

    public void updateUnradDot(List<EMConversation> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getUnreadMsgCount() > 0) {
                unreadLabel.setVisibility(View.VISIBLE);
                return;
            }
        }
        unreadLabel.setVisibility(View.GONE);
    }

    /**
     * update the total unread count
     */
    public void updateUnreadAddressLable() {
        runOnUiThread(new Runnable() {
            public void run() {
                int count = getUnreadAddressCountTotal();
                if (count > 0) {
                    unreadAddressLable.setVisibility(View.VISIBLE);
                } else {
                    unreadAddressLable.setVisibility(View.INVISIBLE);
                }
            }
        });

    }

    private InviteMessageDao inviteMessgeDao;

    /**
     * get unread event notification count, including application, accepted, etc
     *
     * @return
     */
    public int getUnreadAddressCountTotal() {
        int unreadAddressCountTotal = 0;
        unreadAddressCountTotal = inviteMessgeDao.getUnreadMessagesCount();
        return unreadAddressCountTotal;
    }

    /**
     * get unread message count
     *
     * @return
     */
    public int getUnreadMsgCountTotal() {
        int unreadMsgCountTotal = 0;
        int chatroomUnreadMsgCount = 0;
        unreadMsgCountTotal = EMClient.getInstance().chatManager().getUnreadMsgsCount();
        for (EMConversation conversation : EMClient.getInstance().chatManager().getAllConversations().values()) {
            if (conversation.getType() == EMConversation.EMConversationType.ChatRoom)
                chatroomUnreadMsgCount = chatroomUnreadMsgCount + conversation.getUnreadMsgCount();
        }
        return unreadMsgCountTotal - chatroomUnreadMsgCount;
    }

    private void refreshUIWithMessage() {
        runOnUiThread(new Runnable() {
            public void run() {
                // refresh unread count
                updateUnreadLabel();
                if (radioContact.isChecked()) {
                    // refresh conversation list
                    if (contactFra != null) {
                        contactFra.refresh();
                    }
                }
            }
        });
    }
}
