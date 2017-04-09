package com.example.imdemo.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.imdemo.CustomerHelper;
import com.example.imdemo.R;
import com.example.imdemo.db.InviteMessageDao;
import com.example.imdemo.widget.ContectItemView;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.ui.EaseContactListFragment;
import com.hyphenate.util.EMLog;

import java.util.Hashtable;
import java.util.Map;

/**
 * Created by SH on 2016/12/28.
 */

public class ConnectFragment extends EaseContactListFragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    protected ContectItemView TextViewapplyer;
    protected LinearLayout linear_header;
    private InviteMessageDao inviteMessgeDao;
    private View loadingView;
    private ContactSyncListener contactSyncListener;

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    private Delegate delegate;

    public static ConnectFragment newInstance() {

        Bundle args = new Bundle();

        ConnectFragment fragment = new ConnectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("InflateParams")
    @Override
    protected void initView() {
        super.initView();
        View view = View.inflate(getActivity(), R.layout.conn_fragment_header, null);
        TextViewapplyer = (ContectItemView) view.findViewById(R.id.applyer_text);
        TextViewapplyer.setOnClickListener(ConnectFragment.this);
        ContectItemView text_group = (ContectItemView) view.findViewById(R.id.group_text);
        text_group.setOnClickListener(ConnectFragment.this);
        //linear_header = (LinearLayout) view.findViewById(R.id.linear_conn_header);
        listView.addHeaderView(view);
        loadingView = LayoutInflater.from(getActivity()).inflate(R.layout.loading_data, null);
        contentContainer.addView(loadingView);
        listView.setOnItemClickListener(this);
        refresh();
        //  设置联系人数据
        /*Map<String, EaseUser> map = CustomerHelper.getCustomerhelper().getContactList();
        if (map instanceof Hashtable<?, ?>) {
            map = (Map<String, EaseUser>) ((Hashtable<String, EaseUser>) map).clone();
        }
        setContactsMap(map);*/
    }

    @Override
    protected void setUpView() {

        //  设置联系人数据
        Map<String, EaseUser> map = CustomerHelper.getCustomerhelper().getContactList();
        if (map instanceof Hashtable<?, ?>) {
            map = (Map<String, EaseUser>) ((Hashtable<String, EaseUser>) map).clone();
        }
        setContactsMap(map);
        super.setUpView();

        contactSyncListener = new ContactSyncListener();
        CustomerHelper.getCustomerhelper().addSyncContactListener(contactSyncListener);

       /* contactInfoSyncListener = new ContactListFragment.ContactInfoSyncListener();
        CustomerHelper.getCustomerhelper().getUserProfileManager().addSyncContactInfoListener(contactInfoSyncListener);
*/
        if (CustomerHelper.getCustomerhelper().isContactsSyncedWithServer()) {
            loadingView.setVisibility(View.GONE);
        } else if (CustomerHelper.getCustomerhelper().isSyncingContactsWithServer()) {
            loadingView.setVisibility(View.VISIBLE);
        }
    }

    class ContactSyncListener implements CustomerHelper.DataSyncListener {

        @Override
        public void onSyncComplete(final boolean success) {

            EMLog.d("ContactSyncListener", "on contact list sync success:" + success);
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (success) {
                                loadingView.setVisibility(View.GONE);
                                refresh();
                            } else {
                                String s1 = getResources().getString(R.string.get_failed_please_check);
                                Toast.makeText(getActivity(), s1, Toast.LENGTH_LONG).show();
                                loadingView.setVisibility(View.GONE);
                            }
                        }

                    });
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.applyer_text) {
            delegate.hideUnreadDot();

            //done 跳转到申请和通知的页面
            startActivity(new Intent(getActivity(), NotificationMsgActivity.class));
        } else if (view.getId() == R.id.group_text) {

            // done 跳转到群组的页面
            startActivity(new Intent(getActivity(), GroupsActivity.class));
        }
    }

    @Override
    public void refresh() {
        Map<String, EaseUser> m = CustomerHelper.getCustomerhelper().getContactList();
        if (m instanceof Hashtable<?, ?>) {
            //noinspection unchecked
            m = (Map<String, EaseUser>) ((Hashtable<String, EaseUser>) m).clone();
        }
        setContactsMap(m);
        super.refresh();
        if (inviteMessgeDao == null) {
            inviteMessgeDao = new InviteMessageDao(getActivity());
        }
        if (inviteMessgeDao.getUnreadMessagesCount() > 0) {
            TextViewapplyer.showUnreadMsgView();
        } else {
            TextViewapplyer.hideUnreadMsgView();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // done 点击进入聊天界面
        EaseUser user = (EaseUser) listView.getItemAtPosition(position);
        if (user != null) {
            String username = user.getUsername();
            startActivity(new Intent(getActivity(), ChatActivity.class).putExtra("userId", username));
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        hideTitleBar();
    }

    public interface Delegate{
        void hideUnreadDot();
    }
}
