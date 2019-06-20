package com.example.jinha.myapplication;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

public class FindActivity extends AppCompatActivity {

    CustomFragmentPagerAdapter mCustomFragmentPagerAdapter;
    ViewPager mViewPager;
    TabLayout slidingTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);

        mCustomFragmentPagerAdapter = new CustomFragmentPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.findviewPager);
        mViewPager.setAdapter(mCustomFragmentPagerAdapter);
        slidingTabs = (TabLayout) findViewById(R.id.slidingTabs);

        slidingTabs.addTab(slidingTabs.newTab().setText("ID"), 0, true); // 페이지 등록
        slidingTabs.addTab(slidingTabs.newTab().setText("PW"), 1, true);

        slidingTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition()); // 슬라이딩이 아니라 위에 페이지를 선택했을 때도 페이지 이동 가능하게.
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }

        });

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(slidingTabs));
    }
}