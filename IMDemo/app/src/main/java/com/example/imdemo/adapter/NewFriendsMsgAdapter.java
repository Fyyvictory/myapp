package com.example.imdemo.adapter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imdemo.R;
import com.example.imdemo.db.InviteMessageDao;
import com.example.imdemo.entities.InviteMessage;
import com.hyphenate.chat.EMClient;

import java.util.List;

/**
 * Created by SH on 2017/1/11.
 */

public class NewFriendsMsgAdapter extends ArrayAdapter<InviteMessage> {
    private Context mCon;
    private InviteMessageDao dao;

    public NewFriendsMsgAdapter(Context context, int resource, List<InviteMessage> list) {
        super(context, resource,list);
        mCon = context;
        dao = new InviteMessageDao(mCon);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = View.inflate(mCon, R.layout.item_newfriendlist, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String str1 = mCon.getResources().getString(R.string.Has_agreed_to_your_friend_request);
        String str2 = mCon.getResources().getString(R.string.agree);

        String str3 = mCon.getResources().getString(R.string.Request_to_add_you_as_a_friend);
        String str4 = mCon.getResources().getString(R.string.Apply_to_the_group_of);
        String str5 = mCon.getResources().getString(R.string.Has_agreed_to);
        String str6 = mCon.getResources().getString(R.string.Has_refused_to);

        String str7 = mCon.getResources().getString(R.string.refuse);
        String str8 = mCon.getResources().getString(R.string.invite_join_group);
        String str9 = mCon.getResources().getString(R.string.accept_join_group);
        String str10 = mCon.getResources().getString(R.string.refuse_join_group);

        final InviteMessage inviteMessage = getItem(position);
        if(inviteMessage != null){
            viewHolder.agree.setVisibility(View.INVISIBLE);
            if(inviteMessage.getGroupId() != null){
                viewHolder.llGroup.setVisibility(View.VISIBLE);
                viewHolder.tvGroupName.setText(inviteMessage.getGroupName());
            }else{
                viewHolder.llGroup.setVisibility(View.GONE);
            }
            viewHolder.message.setText(inviteMessage.getReason());
            viewHolder.tvName.setText(inviteMessage.getFrom());
            if(inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.BEAGREED){
                viewHolder.msgState.setVisibility(View.GONE);
                viewHolder.message.setText(str1);
            }else if(inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.BEINVITEED || inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.BEAPPLYED ||
                    inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.GROUPINVITATION){
                viewHolder.agree.setVisibility(View.VISIBLE);
                viewHolder.agree.setEnabled(true);
                viewHolder.agree.setBackgroundResource(android.R.drawable.btn_default);
                viewHolder.agree.setText(str2);

                viewHolder.userState.setVisibility(View.VISIBLE);
                viewHolder.userState.setEnabled(true);
                viewHolder.userState.setBackgroundResource(android.R.drawable.btn_default);
                viewHolder.userState.setText(str7);

                if(inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.BEINVITEED){
                    if(inviteMessage.getReason() == null){
                        viewHolder.message.setText(str3);
                    }
                }else if(inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.BEAPPLYED){
                    // 进群邀请
                    if(TextUtils.isEmpty(inviteMessage.getReason())){
                        viewHolder.message.setText(str4+inviteMessage.getGroupName());
                    }
                }else if(inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.GROUPINVITATION){
                    if(TextUtils.isEmpty(inviteMessage.getReason()))
                        viewHolder.message.setText(str8+inviteMessage.getGroupName());
                }

                // 添加点击事件
                final ViewHolder finalViewHolder = viewHolder;
                viewHolder.agree.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        acceptInvitation(finalViewHolder.agree, finalViewHolder.userState,inviteMessage);
                    }
                });

                viewHolder.userState.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        refuseInvitation(finalViewHolder.agree, finalViewHolder.userState,inviteMessage);
                    }
                });
            }else if(inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.AGREED){
                viewHolder.userState.setText(str5);
                viewHolder.userState.setBackgroundDrawable(null);
                viewHolder.userState.setEnabled(false);
            }else if(inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.GROUPINVITATION_ACCEPTED){
                String str = inviteMessage.getGroupInviter()+str9+inviteMessage.getGroupName();
                viewHolder.userState.setText(str);
                viewHolder.userState.setBackgroundDrawable(null);
                viewHolder.userState.setEnabled(false);
            }else if(inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.GROUPINVITATION_DECLINED){
                String str = inviteMessage.getGroupInviter()+str10+inviteMessage.getGroupName();
                viewHolder.userState.setText(str);
                viewHolder.userState.setBackgroundDrawable(null);
                viewHolder.userState.setEnabled(false);
            }
        }
        return convertView;
    }

    /**
     * 拒绝邀请
     * @param agree
     * @param userState
     * @param inviteMessage
     */
    private void refuseInvitation(final Button agree, final Button userState, final InviteMessage inviteMessage) {
        final ProgressDialog pd = new ProgressDialog(mCon);
        String str01 = mCon.getResources().getString(R.string.Are_refuse_with);
        final String str02 = mCon.getResources().getString(R.string.Has_refused_to);
        final String str03 = mCon.getResources().getString(R.string.Refuse_with_failure);
        pd.setMessage(str01);
        pd.setCanceledOnTouchOutside(false);
        pd.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                  try{
                      if (inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.BEINVITEED) {//decline the invitation
                          EMClient.getInstance().contactManager().declineInvitation(inviteMessage.getFrom());
                      } else if (inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.BEAPPLYED) { //decline application to join group
                          EMClient.getInstance().groupManager().declineApplication(inviteMessage.getFrom(), inviteMessage.getGroupId(), "");
                      } else if (inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.GROUPINVITATION) {
                          EMClient.getInstance().groupManager().declineInvitation(inviteMessage.getGroupId(), inviteMessage.getGroupInviter(), "");
                      }
                      inviteMessage.setStatus(InviteMessage.InviteMesageStatus.REFUSED);
                      // update database
                      ContentValues values = new ContentValues();
                      values.put(InviteMessageDao.COLUMN_NAME_STATUS, inviteMessage.getStatus().ordinal());
                      dao.updateMessage(inviteMessage.getId(), values);
                      ((Activity)mCon).runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              pd.dismiss();
                              userState.setText(str02);
                              userState.setBackgroundDrawable(null);
                              userState.setEnabled(false);
                              agree.setVisibility(View.GONE);
                          }
                      });
                  }catch (final Exception e){
                      ((Activity) mCon).runOnUiThread(new Runnable() {

                          @Override
                          public void run() {
                              pd.dismiss();
                              Toast.makeText(mCon, str03 + e.getMessage(), Toast.LENGTH_SHORT).show();
                          }
                      });
                  }
            }
        }).start();
    }

    /**
     * 接受邀请
     * @param agree
     * @param userState
     * @param inviteMessage
     */
    private void acceptInvitation(final Button agree, final Button userState, final InviteMessage inviteMessage) {
        final ProgressDialog pD = new ProgressDialog(mCon);
        String str01 = mCon.getResources().getString(R.string.Are_agree_with);
        final String str02 = mCon.getResources().getString(R.string.Has_agreed_to);
        final String str03 = mCon.getResources().getString(R.string.Agree_with_failure);
        pD.setMessage(str01);
        pD.setCanceledOnTouchOutside(false);
        pD.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.BEINVITEED){
                        EMClient.getInstance().contactManager().acceptInvitation(inviteMessage.getFrom());
                    }else if(inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.BEAPPLYED){
                        //accept application to join group
                        EMClient.getInstance().groupManager().acceptApplication(inviteMessage.getFrom(),inviteMessage.getGroupId());
                    }else if(inviteMessage.getStatus() == InviteMessage.InviteMesageStatus.GROUPINVITATION){
                        EMClient.getInstance().groupManager().acceptInvitation(inviteMessage.getGroupId(), inviteMessage.getGroupInviter());
                    }
                    inviteMessage.setStatus(InviteMessage.InviteMesageStatus.AGREED);
                    // update database
                    ContentValues values = new ContentValues();
                    values.put(InviteMessageDao.COLUMN_NAME_STATUS,inviteMessage.getStatus().ordinal());
                    dao.updateMessage(inviteMessage.getId(),values);
                    ((Activity)mCon).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pD.dismiss();
                            agree.setText(str02);
                            agree.setBackgroundDrawable(null);
                            agree.setEnabled(false);
                            userState.setVisibility(View.GONE);
                        }
                    });
                }catch (final Exception e){
                    ((Activity)mCon).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pD.dismiss();
                            Toast.makeText(mCon, str03+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    static class ViewHolder {
        protected ImageView ivAvstar;
        protected RelativeLayout avstarContainer;
        protected TextView tvName;
        protected ImageView msgState;
        protected TextView message;
        protected Button agree;
        protected Button userState;
        protected TextView tvGroupName;
        protected LinearLayout llGroup;

        ViewHolder(View rootView) {
            initView(rootView);
        }

        private void initView(View rootView) {
            ivAvstar = (ImageView) rootView.findViewById(R.id.iv_avstar);
            avstarContainer = (RelativeLayout) rootView.findViewById(R.id.avstar_container);
            tvName = (TextView) rootView.findViewById(R.id.tv_name);
            msgState = (ImageView) rootView.findViewById(R.id.msg_state);
            message = (TextView) rootView.findViewById(R.id.message);
            agree = (Button) rootView.findViewById(R.id.agree);
            userState = (Button) rootView.findViewById(R.id.user_state);
            tvGroupName = (TextView) rootView.findViewById(R.id.tv_groupName);
            llGroup = (LinearLayout) rootView.findViewById(R.id.ll_group);
        }
    }
}
