package com.example.jinha.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginDialogActivity extends AppCompatActivity {
    private BackPressCloseHandler backPressCloseHandler;
    SharedPreferences setting;
    SharedPreferences.Editor editor;
    Button btnJoin;
    Button btnFindId;
    Button btnFindPw;
    Button btnLogin;
    CheckBox stateLogin;
    EditText userName;
    EditText password;
    String txtId, txtPassword;
    String ID, PW;


    @SuppressLint("CommitPrefEdits")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.login_dialog);
        super.onCreate(savedInstanceState);
        backPressCloseHandler = new BackPressCloseHandler(this);
        btnJoin =  (Button)findViewById(R.id.btnJoin);
        btnFindId = (Button)findViewById(R.id.btnFindId);
        btnFindPw = (Button)findViewById(R.id.btnFindPw);
        btnLogin = (Button)findViewById(R.id.btnLogin);
        stateLogin = (CheckBox)findViewById(R.id.stateLogin);
        userName = (EditText)findViewById(R.id.userName);
        password = (EditText)findViewById(R.id.password);
        setting = getSharedPreferences("setting", MODE_PRIVATE);
        editor = setting.edit();

//        SharedPreferences sf = getSharedPreferences("sFile", MODE_PRIVATE); //저장된 값을 불러오기 위해 같은 네임파일을 찾음.
//        String text = sf.getString("text","");  //text라는 key에 저장된 값이 있는지 확인, 아무 값도 들어있지 않으면 ""를 반환

        ID = userName.getText().toString();
        PW = password.getText().toString();
        //Toast.makeText(getApplicationContext(),setting.getBoolean("Auto_Login_enabled",false),Toast.LENGTH_SHORT).show();
        stateLogin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    editor.putString("ID", ID);
                    editor.putString("PW", PW);
                    editor.putBoolean("Auto_Login_enabled",true);
                    editor.apply();
                }else{
                    editor.remove("ID");
                    editor.remove("PW");
                    editor.remove("Auto_Login_enabled");
                    editor.clear();
                    editor.commit();
                }
            }
        });

        if(setting.getBoolean("Auto_Login_enabled", false)){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);

        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 테스트용 코드
                String ID = userName.getText().toString();
                txtId = userName.getText().toString();
                txtPassword = password.getText().toString();
                saveId(txtId); // 아이디를 파일에 저장하는 함수
                //Toast.makeText(getApplicationContext(), txtId, Toast.LENGTH_SHORT);
                Intent intent = new Intent(getApplicationContext(),MainActivity.class );
                intent.putExtra("ID",txtId);
                intent.putExtra("PW",txtPassword);
                startActivity(intent);
            }
        });

        btnLogin.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN :
                        btnLogin.setBackgroundColor(Color.GRAY);
                        break;
                    case MotionEvent.ACTION_UP :
                        btnLogin.setBackgroundColor(Color.BLACK);
                        break;
                }
                return false;
            }
        });


        btnJoin.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);

            }
        });

        btnFindId.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(getApplicationContext(), FindActivity.class);
                startActivity(intent);
            }
        });

        btnFindPw.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(getApplicationContext(), FindActivity.class);
                startActivity(intent);
            }
        });
    }

    public void saveId(String inputId)  // 아이디를 파일에 저장하는 함수
    {
        try
        {
            BufferedWriter bw = new BufferedWriter(new FileWriter(getFilesDir() + "id.txt", false));
            bw.write(inputId);
            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            Toast.makeText(this, "Exception", Toast.LENGTH_SHORT).show();
        }
    }
    public void onStop(){
        super.onStop();
    }
    @Override
    public void onBackPressed(){
        backPressCloseHandler.onBackPressed();
    }

    public void MemberCheck(final String id, final String password)
    {
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        String url = getString(R.string.loginUrl);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>()
        {
            @Override
            public void onResponse(String response)
            {
                // Response
                try
                {
                    JSONArray jarray = new JSONObject(response).getJSONArray("List");
                    JSONObject jObject = jarray.getJSONObject(0);
                    String result = jObject.optString("RESULT");
                    if(result.equals("1"))
                    {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "아이디/비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show();
                    }
                }
                catch(org.json.JSONException e)
                {
                }
            }
        }, new Response.ErrorListener()
        {

            @Override
            public void onErrorResponse(VolleyError error)
            {
                // Error Handling
                Toast.makeText(getApplicationContext(), "시스템 오류", Toast.LENGTH_SHORT).show();
            }
        })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError
            {
                Map<String, String> params = new HashMap<>();
                params.put("ID", id);
                params.put("PASSWORD", password);
                return params;
            }
        };
        queue.add(stringRequest);
    }
}