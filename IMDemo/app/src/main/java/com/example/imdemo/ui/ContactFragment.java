package com.example.imdemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imdemo.R;
import com.example.imdemo.db.InviteMessageDao;
import com.example.imdemo.entities.Constant;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.easeui.model.EaseAtMessageHelper;
import com.hyphenate.easeui.ui.EaseConversationListFragment;
import com.hyphenate.util.NetUtils;

/**
 * Created by SH on 2016/12/28.
 */

public class ContactFragment extends EaseConversationListFragment {
    public static ContactFragment newInstance() {

        Bundle args = new Bundle();

        ContactFragment fragment = new ContactFragment();
        fragment.setArguments(args);
        return fragment;
    }

    TextView errorText;
    @Override
    protected void initView() {
        super.initView();
        LinearLayout linear = (LinearLayout) View.inflate(getContext(), R.layout.item_contect_error,null);
        errorItemContainer.addView(linear);
        errorText = (TextView) linear.findViewById(R.id.text_neterror);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        hideTitleBar();
    }

    @Override
    protected void onConnectionDisconnected() {
        super.onConnectionDisconnected();
        if (NetUtils.hasNetwork(getActivity())){
            errorText.setText("无法连接服务器");
        } else {
            errorText.setText("网络异常，请检查网络设置");
        }
    }

    @Override
    protected void setUpView() {
        super.setUpView();

        conversationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EMConversation item = conversationListView.getItem(position);
                String userName = item.getUserName();
                if(EMClient.getInstance().getCurrentUser().equals(userName)){
                    Toast.makeText(getContext(), "不能与自己对话", Toast.LENGTH_SHORT).show();
                }else{
                    Intent intent = new Intent(getActivity(),ChatActivity.class);
                    if(item.isGroup()){
                        if(item.getType() == EMConversation.EMConversationType.ChatRoom){
                            // it's group chat
                            intent.putExtra(Constant.EXTRA_CHAT_TYPE, Constant.CHATTYPE_CHATROOM);
                        }else{
                            intent.putExtra(Constant.EXTRA_CHAT_TYPE, Constant.CHATTYPE_GROUP);
                        }
                    }
                    intent.putExtra(Constant.EXTRA_USER_ID,userName);
                    startActivity(intent);
                    ((MainActivity)getActivity()).updateUnradDot(conversationList);
                }
            }
        });
    }



    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.em_delete_message, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    boolean deleteMessage = false;
    if (item.getItemId() == R.id.delete_message) {
        deleteMessage = true;
    } else if (item.getItemId() == R.id.delete_conversation) {
        deleteMessage = false;
    }
    EMConversation tobeDeleteCons = conversationListView.getItem(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
    if (tobeDeleteCons == null) {
        return true;
    }
    if(tobeDeleteCons.getType() == EMConversation.EMConversationType.GroupChat){
        EaseAtMessageHelper.get().removeAtMeGroup(tobeDeleteCons.getUserName());
    }
    try {
        // delete conversation
        EMClient.getInstance().chatManager().deleteConversation(tobeDeleteCons.getUserName(), deleteMessage);
        InviteMessageDao inviteMessgeDao = new InviteMessageDao(getActivity());
        inviteMessgeDao.deleteMessage(tobeDeleteCons.getUserName());
    } catch (Exception e) {
        e.printStackTrace();
    }
    refresh();

    // update unread count
    ((MainActivity) getActivity()).updateUnreadLabel();
    return true;
}
}
