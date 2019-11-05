package com.example.jinha.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class FindIdTab1 extends Fragment {

    EditText editName, editPhone1, editPhone2, editPhone3;
    Button btnFindId;

    public static FindIdTab1 newInstance(){
        Bundle args = new Bundle();
        FindIdTab1 fragment = new FindIdTab1();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_findid,container,false);

        editName = (EditText)view.findViewById(R.id.editName);
        editPhone1 = (EditText)view.findViewById(R.id.editPhone1);
        editPhone2 = (EditText)view.findViewById(R.id.editPhone2);
        editPhone3 = (EditText)view.findViewById(R.id.editPhone3);
        btnFindId = (Button)view.findViewById(R.id.btnNextId);

        editName.setFilters(new InputFilter[]{filterKor}); // 이름 Edittext 한글만 입력가능

        btnFindId.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String name = editName.getText().toString();
                String phone1 = editPhone1.getText().toString();
                String phone2 = editPhone2.getText().toString();
                String phone3 = editPhone3.getText().toString();
                String phone = phone1 + "-" + phone2 + "-" + phone3;
                Toast.makeText(getContext(), phone, Toast.LENGTH_SHORT).show();
                if(name.equals("") || phone.equals(""))
                    Toast.makeText(getContext(), "항목 입력을 확인해주세요", Toast.LENGTH_SHORT).show();
                else
                    findId(name, phone);
            }
        });

        return view;
    }


    public InputFilter filterKor = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            Pattern ps = Pattern.compile("^[ㄱ-ㅣ가-힣]*$");
            if (!ps.matcher(source).matches()) {
                return "";
            }
            return null;
        }
    };

    public void findId(final String name, final String phone)
    {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String getCageStatusUrl = getString(R.string.FindIdUrl);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, getCageStatusUrl, new Response.Listener<String>()
        {
            @Override
            public void onResponse(String response)
            {
                // Response
                try
                {
                    JSONObject jsonMain = new JSONObject(response);
                    String result = jsonMain.optString("RESULT"); // 아이디가 중복되었을 시에 1을 리턴
                    if(result.equals("1"))
                    {
                        String Id = jsonMain.optString("ID");
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("아이디 찾기");
                        builder.setMessage(Id);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();

                    }
                    else
                    {
                        Toast.makeText(getContext(), "회원정보 가져오기 실패", Toast.LENGTH_SHORT).show();
                    }
                }
                catch(JSONException e)
                {
                    Toast.makeText(getContext(), "시스템 오류 json", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                // Error Handling
                Toast.makeText(getContext(), "시스템 오류", Toast.LENGTH_SHORT).show();
            }
        })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError
            {
                Map<String, String> params = new HashMap<>();
                params.put("NAME", name);
                params.put("PHONE", phone);
                return params;
            }
        };
        queue.add(stringRequest);
    }
}




