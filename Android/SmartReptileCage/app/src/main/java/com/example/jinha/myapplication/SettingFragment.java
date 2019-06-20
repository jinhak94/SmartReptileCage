package com.example.jinha.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;


public class SettingFragment extends Fragment {


    private ListView listView;
    private ArrayList<String> item;
    private ArrayAdapter<String> mAdapter;
    SharedPreferences setting;
    SharedPreferences.Editor editor;
    String ID;
    String PW;
    static final String[] LIST_MENU = {"로그아웃" ,"설정"};

    @SuppressLint("CommitPrefEdits")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState)
    {

        View view = inflater.inflate(R.layout.fragment_setting, container,false);
//         Inflate the layout for this fragment
        listView = (ListView)view.findViewById(R.id.setting_list);
        mAdapter = new ArrayAdapter<String>(Objects.requireNonNull(getActivity()), android.R.layout.simple_list_item_1,LIST_MENU);
        listView.setAdapter(mAdapter);
        setting = this.getActivity().getSharedPreferences("setting",0);
        editor = setting.edit();
        if (getArguments() != null) {
            ID = getArguments().getString("ID", "");
            PW = getArguments().getString("PW", "");
        }
        // 리스트뷰 클릭 이벤트
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                switch(position)
                {
                    case 0: // 로그아웃
                        // + 로그아웃 기능 추가
                        editor.remove("ID");
                        editor.remove("PW");
                        editor.remove("Auto_Login_enabled");
                        editor.clear();
                        editor.apply();
                        Intent intent = new Intent(getContext(), LoginDialogActivity.class);
                        startActivity(intent);
                        break;
                    case 1:
                        PopupMenu p = new PopupMenu(getActivity(), view); // anchor : 팝업을 띄울 기준될 위젯
                        getActivity().getMenuInflater().inflate(R.menu.menu_systemsetting, p.getMenu());
                        // 이벤트 처리
                        p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                        {
                            @Override
                            public boolean onMenuItemClick(MenuItem item)
                            {
                                switch (item.getItemId())
                                {
                                    case R.id.bluetoothSetting :
                                        Intent intent1 = new Intent(getContext(), BluetoothFragment.class);
                                        startActivity(intent1);
                                        break;
                                    case R.id.notification :
                                        Intent intent2 = new Intent(getContext(), SettingSystemActivity.class);
                                        intent2.putExtra("ID",ID);
                                        intent2.putExtra("PW",PW);
                                        startActivity(intent2);
                                        break;
                                }
                                return false;
                            }
                        });
                        p.show(); // 메뉴를 띄우기
                        break;
                    default:
                        break;
                }
            }
        });

        return view;
    }

}