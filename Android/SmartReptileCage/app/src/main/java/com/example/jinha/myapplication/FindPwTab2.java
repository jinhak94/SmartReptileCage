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

public class FindPwTab2 extends Fragment {

    EditText editId, editName, editPhone;
    Button btnFindPw;

    public static FindPwTab2 newInstance() {
        Bundle args = new Bundle();
        FindPwTab2 fragment = new FindPwTab2();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_findpw, container, false);

        editId = (EditText)view.findViewById(R.id.editID);
        editName = (EditText)view.findViewById(R.id.editName);
        editPhone = (EditText)view.findViewById(R.id.editPhone);
        btnFindPw = (Button)view.findViewById(R.id.btnNextPw);

        editName.setFilters(new InputFilter[]{filterKor}); // 이름 Edittext 한글만 입력가능
        editId.setFilters(new InputFilter[]{filterEngNum}); // 아이디 Edittext 영어숫자만 입력가능

        btnFindPw.setOnClickListener(new View.OnClickListener() {
            String id = editId.getText().toString();
            String name = editName.getText().toString();
            String phone = editPhone.getText().toString();
            @Override
            public void onClick(View v)
            {
                if(id.equals("") || name.equals("") || phone.equals(""))
                    Toast.makeText(getContext(), "항목 입력을 확인해주세요", Toast.LENGTH_SHORT).show();
                else
                    findPw(id, name, phone);
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

    public InputFilter filterEngNum = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            Pattern ps = Pattern.compile("[a-zA-Z0-9]*$");
            if (!ps.matcher(source).matches()) {
                return "";
            }
            return null;
        }
    };

    public void findPw(final String id, final String name, final String phone)
    {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String getCageStatusUrl = getString(R.string.getCageStatusUrl);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, getCageStatusUrl, new Response.Listener<String>()
        {
            @Override
            public void onResponse(String response)
            {
                // Response
                try
                {
                    JSONArray jarray = new JSONObject(response).getJSONArray("List"); // 대괄호 구별
                    JSONObject jObject = jarray.getJSONObject(0); // 중괄호 구별
                    String result = jObject.optString("RESULT"); // 아이디가 중복되었을 시에 1을 리턴
                    if(result.equals("1"))
                    {
                        String password = jObject.optString("PASSWORD");
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle(password);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
                    }
                    else
                    {
                        Toast.makeText(getContext(), "회원정보 가져오기 실패", Toast.LENGTH_SHORT).show();
                    }
                }
                catch(JSONException e)
                {
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
                params.put("ID", id);
                params.put("NAME", name);
                params.put("PHONE", phone);
                return params;
            }
        };
        queue.add(stringRequest);
    }
}