package com.example.jinha.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
// 앱 처음 실행시 뜨는 로고 액티비티
public class SplashActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        try{
            Thread.sleep(2000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        startActivity(new Intent(this, LoginDialogActivity.class));
        finish();
    }
}
