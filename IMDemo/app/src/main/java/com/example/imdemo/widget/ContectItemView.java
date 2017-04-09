package com.example.imdemo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.imdemo.R;

/**
 * Created by SH on 2017/1/16.
 */

public class ContectItemView extends LinearLayout {
    private TextView unreadMsgView;
    public ContectItemView(Context context) {
        super(context);
    }

    public ContectItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    private void init(Context context, AttributeSet attrs){
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ContectItemView);
        String name = ta.getString(R.styleable.ContectItemView_contactItemName);
        Drawable image = ta.getDrawable(R.styleable.ContectItemView_contactItemImage);
        ta.recycle();

        LayoutInflater.from(context).inflate(R.layout.contectitemview, this);
        ImageView avatar = (ImageView) findViewById(R.id.avatar);
        unreadMsgView = (TextView) findViewById(R.id.unread_msg_number);
        TextView nameView = (TextView) findViewById(R.id.name);
        if(image != null){
            avatar.setImageDrawable(image);
        }
        nameView.setText(name);
    }

    public void setUnreadCount(int unreadCount){
        unreadMsgView.setText(String.valueOf(unreadCount));
    }

    public void showUnreadMsgView(){
        unreadMsgView.setVisibility(View.VISIBLE);
    }
    public void hideUnreadMsgView(){
        unreadMsgView.setVisibility(View.INVISIBLE);
    }
}
