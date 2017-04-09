package com.example.imdemo.db;

import android.content.ContentValues;
import android.content.Context;

import com.example.imdemo.entities.InviteMessage;

import java.util.List;

/**
 * Created by SH on 2016/12/29.
 */

public class InviteMessageDao {
    static final String TABLE_NAME = "new_friends_msgs";
    static final String COLUMN_NAME_ID = "id";
    static final String COLUMN_NAME_FROM = "username";
    static final String COLUMN_NAME_GROUP_ID = "groupid";
    static final String COLUMN_NAME_GROUP_Name = "groupname";

    static final String COLUMN_NAME_TIME = "time";
    static final String COLUMN_NAME_REASON = "reason";
    public static final String COLUMN_NAME_STATUS = "status";
    static final String COLUMN_NAME_ISINVITEFROMME = "isInviteFromMe";
    static final String COLUMN_NAME_GROUPINVITER = "groupinviter";

    static final String COLUMN_NAME_UNREAD_MSG_COUNT = "unreadMsgCount";
    Context mCon;

    public InviteMessageDao(Context mCon){this.mCon = mCon;}

    /**
     * save message
     * @param message
     * @return  return cursor of the message
     */
    public Integer saveMessage(InviteMessage message){
        return DbManager.getInstance(mCon).saveMessage(message);
    }

    /**
     * update message
     * @param msgId
     * @param values
     */
    public void updateMessage(int msgId,ContentValues values){
        DbManager.getInstance(mCon).updateMessage(msgId, values);
    }

    /**
     * get messges
     * @return
     */
    public List<InviteMessage> getMessagesList(){
        return DbManager.getInstance(mCon).getMessagesList();
    }

    public void deleteMessage(String from){
        DbManager.getInstance(mCon).deleteMessage(from);
    }

    public int getUnreadMessagesCount(){
        return DbManager.getInstance(mCon).getUnreadNotifyCount();
    }

    public void saveUnreadMessageCount(int count){
        DbManager.getInstance(mCon).setUnreadNotifyCount(count);
    }
}
