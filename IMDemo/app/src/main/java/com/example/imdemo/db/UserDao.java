package com.example.imdemo.db;

import android.content.Context;

import com.hyphenate.easeui.domain.EaseUser;

import java.util.List;
import java.util.Map;

/**
 * Created by SH on 2016/12/29.
 */

public class UserDao {
    public static final String TABLE_NAME = "uers";
    public static final String COLUMN_NAME_ID = "username";
    public static final String COLUMN_NAME_NICK = "nick";
    public static final String COLUMN_NAME_AVATAR = "avatar";

    public static final String PREF_TABLE_NAME = "pref";
    public static final String COLUMN_NAME_DISABLED_GROUPS = "disabled_groups";
    public static final String COLUMN_NAME_DISABLED_IDS = "disabled_ids";
    Context mCon;

    public UserDao(Context mCon){this.mCon = mCon;}

    public void saveContactList(List<EaseUser> list){
        DbManager.getInstance(mCon).saveContactList(list);
    }

    public Map<String, EaseUser> getContectList(){
        return DbManager.getInstance(mCon).getContectList();
    }

    public void deleteContact(String userName){
        DbManager.getInstance(mCon).deleteContact(userName);
    }

    public void saveContact(EaseUser mUser){
        DbManager.getInstance(mCon).saveContact(mUser);
    }

    public void setDisabledGroups(List<String> groups){
        DbManager.getInstance(mCon).setDisabledGroups(groups);
    }

    public List<String>  getDisabledGroups(){
        return DbManager.getInstance(mCon).getDisabledGroups();
    }

    public void setDisabledIds(List<String> ids){
        DbManager.getInstance(mCon).setDisabledIds(ids);
    }

    public List<String> getDisabledIds(){
        return DbManager.getInstance(mCon).getDisabledIds();
    }
}
