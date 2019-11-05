package com.example.jinha.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    //ViewPager viewPager;
    private BackPressCloseHandler backPressCloseHandler;
    ViewPager viewPager;
    public static final int sub = 1001;
    final Fragment fragment_graph = new GraphFragment();
    final Fragment fragment_reptile = new CageFragment();
    final Fragment fragment_community = new CommunityFragment();
    final Fragment fragment_map = new MapFragment();
    final Fragment fragment_setting = new SettingFragment();
    final Fragment fragment_systemsetting = new SettingFragment();
    String community = "";
    String reptile = "";
    String map = "";
    String setting = "";
    String graph = "";
    final FragmentManager fm = getSupportFragmentManager();
    android.app.FragmentManager fmanager;
    FragmentTransaction tran;
    private BottomNavigationView optionBottomNavigation;
    private MenuItem bottomNavigationMenu;

    Fragment active = fragment_reptile;

   // @Override
    //public void sendInput(long input){
      //  //Toast.makeText(getApplicationContext(),input,Toast.LENGTH_LONG).show();
    //    Intent intent = new Intent(getApplicationContext(), CommunityArticleReadActivity.class);
      //  intent.putExtra("input", input);
    //    startActivity(intent);
    //}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        String ID = intent.getStringExtra("ID");
        String PW = intent.getStringExtra("PW");
        //Toast.makeText(getApplicationContext(),ID, Toast.LENGTH_SHORT).show();
        //Toast.makeText(getApplicationContext(),PW,Toast.LENGTH_SHORT).show();
        Bundle bundle = new Bundle();
        bundle.putString("PW",PW);
        bundle.putString("ID", ID);
        fragment_community.setArguments(bundle);
        fragment_setting.setArguments(bundle);
        fragment_graph.setArguments(bundle);

        backPressCloseHandler = new BackPressCloseHandler(this);
        // Assume thisActivity is the current activity
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);

        // User Permission Check - Prerequisite After Mashmallow
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        fm.beginTransaction().add(R.id.main_container, fragment_map, "4").hide(fragment_map).commit();
        fm.beginTransaction().add(R.id.main_container, fragment_setting, "5").hide(fragment_setting).commit();
        fm.beginTransaction().add(R.id.main_container, fragment_community, "3").hide(fragment_community).commit();
        fm.beginTransaction().add(R.id.main_container,fragment_reptile, "1").commit();
        fm.beginTransaction().add(R.id.main_container, fragment_graph, "2").hide(fragment_graph).commit();

    }

    @Override
    public void onBackPressed(){
        backPressCloseHandler.onBackPressed();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        return true ;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //community = getResources().getString(R.string.title_community);
        //prevBottomNavigation = bottomNavigationView.getMenu().getItem(position);
        //prevBottomNavigation.setChecked(true);
        //mOnNavigationItemSelectedListener.onNavigationItemSelected(item);
        //onPageSelected(item.getOrder());
        switch (item.getItemId()) {
            //case R.id.reptile:
            //mOnNavigationItemSelectedListener.onNavigationItemSelected(item);
            //  onPageSelected(0);
            // return true;

            case R.id.community:
                // TODO : process the click event for action_search item.
                Intent intent = new Intent(getApplicationContext(),  BluetoothFragment.class);
                startActivity(intent);
                return true;

            /*case R.id.map:
                mOnNavigationItemSelectedListener.onNavigationItemSelected(item);
                onPageSelected(2);
                return true;

            case R.id.setting:
                mOnNavigationItemSelectedListener.onNavigationItemSelected(item);
                onPageSelected(3);
                return true;
            default :
                return true;*/
        }
        return true;
    }


    private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            community = getResources().getString(R.string.titleCommunity);
            reptile = getResources().getString(R.string.titleReptile);
            map = getResources().getString(R.string.titleMap);
            setting = getResources().getString(R.string.titleSetting);
            graph = getResources().getString(R.string.titleGraph);
            String i = item.getTitle().toString();

            if (i.equals(reptile) ) {
                fm.beginTransaction().hide(active).show(fragment_reptile).commit();
                active = fragment_reptile;
                return true;
            }
            else if (item.getItemId()==R.id.navigation_community) {

                fm.beginTransaction().hide(active).show(fragment_community).commit();
                active = fragment_community;
                //fm.beginTransaction().replace(R.id.pager, fragment_community).commit();
                //Intent intent = new Intent(getApplicationContext(), CommunityActivity.class);
                //startActivity(intent);
                //fm.beginTransaction().detach(fragment_community).attach(fragment_community).commit();
                return true;}
            else if (i.equals(setting)) {
                fm.beginTransaction().hide(active).show(fragment_setting).commit();

                active = fragment_setting;
                return true;
            } else if (i.equals(map)) {
                fm.beginTransaction().hide(active).show(fragment_map).commit();
                active = fragment_map;
                return true;
            }
            else if(i.equals(graph)){
                fm.beginTransaction().hide(active).show(fragment_graph).commit();
                active = fragment_graph;
                fragment_graph.onResume();
                return true;
            }
            return false;
        }
    };
}