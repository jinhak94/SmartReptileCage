package com.example.jinha.myapplication;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class CustomFragmentPagerAdapter extends FragmentPagerAdapter {

    private static int PAGE_NUMBER = 2;

    public CustomFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }


    @Override
    public Fragment getItem(int position)
    {
        switch (position) {
            case 0 : // 여기 서의 숫자와 MainActivity.java에서 Tab에서탭 번호가 매칭이되느 겁니다.
                return new FindIdTab1();
            case 1 :
                return new FindPwTab2();
            default:
                break;
        }
        return null;
    }


    @Override
    public int getCount() {
        return PAGE_NUMBER; // 원하는 페이지 수
    }
}
