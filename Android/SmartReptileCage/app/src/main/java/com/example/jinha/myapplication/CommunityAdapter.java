package com.example.jinha.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class CommunityAdapter extends BaseAdapter {

    private ArrayList<ListVO>  listVO = new ArrayList<ListVO>();
    public CommunityAdapter()
    {

    }

    @Override
    public int getCount() {
        return listVO.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();

        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.custom_listview, parent, false);
        }

        TextView title = (TextView)convertView.findViewById(R.id.title);
        TextView writer = (TextView)convertView.findViewById(R.id.writer);
        TextView hit = (TextView)convertView.findViewById(R.id.hit);
        TextView date = (TextView)convertView.findViewById(R.id.date);

        ListVO listViewItem = listVO.get(position);

        title.setText(listViewItem.getTitle());
        writer.setText(listViewItem.getWriter());
        hit.setText(listViewItem.getHit());
        date.setText(listViewItem.getDate());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
               // Toast.makeText(context, (pos+1)+"번째 리스트가 클릭되었습니다.", Toast.LENGTH_SHORT).show();


            }
        });

        return convertView;
    }


    @Override
    public Object getItem(int position) {
        return listVO.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    public void addVO(String title, String writer, String date, String hit)
    {
        ListVO item = new ListVO();
        item.setTitle(title);
        item.setWriter(writer);
        item.setDate(date);
        item.setHit(hit);
        listVO.add(item);
    }
}
