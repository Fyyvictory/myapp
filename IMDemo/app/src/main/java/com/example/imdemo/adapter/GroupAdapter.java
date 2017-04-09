package com.example.imdemo.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.imdemo.R;
import com.hyphenate.chat.EMGroup;

import java.util.List;

/**
 * Created by SH on 2016/12/30.
 */

public class GroupAdapter extends ArrayAdapter<EMGroup> {

    private LayoutInflater inflater;
    private String newGroup;
    private List<EMGroup> list;

    public GroupAdapter(Context context, int resource, List<EMGroup> list) {
        super(context, resource);
        this.inflater = LayoutInflater.from(context);
        newGroup = "创建群组";
        this.list = list;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0)
            return 0;
        else if(position == 1)
            return 1;
        else
            return 2;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(getItemViewType(position) == 0){
            if(convertView == null){
                convertView = inflater.inflate(R.layout.searchbar,parent,false);
            }
            final EditText search = (EditText) convertView.findViewById(R.id.query);
            final ImageButton imgClear = (ImageButton) convertView.findViewById(R.id.search_clear);
            search.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    if(s.length()>0){
                        imgClear.setVisibility(View.VISIBLE);
                    }else{
                        imgClear.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            imgClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    search.getText().clear();
                }
            });
        }else if(getItemViewType(position) == 1){
            if(convertView == null){
                convertView = inflater.inflate(R.layout.addgroup,parent,false);
            }
        }else if(getItemViewType(position) == 2){
            if(convertView == null){
                convertView = inflater.inflate(R.layout.grouprow,parent,false);
            }
            TextView groupName = (TextView) convertView.findViewById(R.id.name);
            groupName.setText(list.get(position - 2).getGroupName());
        }

        return convertView;
    }

    @Override
    public int getCount() {
        return super.getCount()+2;
    }
}
