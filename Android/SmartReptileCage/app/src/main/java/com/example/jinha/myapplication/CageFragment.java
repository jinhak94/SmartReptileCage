package com.example.jinha.myapplication;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class CageFragment extends Fragment {
    // Fragment Related variables
    private FragmentActivity rootActivity;
    private View rootView;

    // Layout 변수 선언
    TextView textTemperature, textHumidity, textDate;
    Button btnShowStreaming;
    ProgressBar progressTemp, progressHumi;
    private boolean runFlag = false;
    private final String zero = "0 / 100";
    public CageFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView =  inflater.inflate(R.layout.fragment_cage, container, false);
        rootActivity = getActivity();

        // Strict Mode 설정
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        textTemperature = rootView.findViewById(R.id.txtTemp);
        textHumidity = rootView.findViewById(R.id.txtHum);
        btnShowStreaming = rootView.findViewById(R.id.btnShowStreaming);
        textDate = rootView.findViewById(R.id.txtDate);
        progressTemp = rootView.findViewById(R.id.progressTemp);
        progressHumi = rootView.findViewById(R.id.progressHumi);


        final String finalID = readId();
        Toast.makeText(getContext(), finalID+"함수 밖", Toast.LENGTH_SHORT);

        btnShowStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final WebView webView = new WebView(getActivity());
                final String tmpFinalID = readId();
                Toast.makeText(getContext(), tmpFinalID+"리스너 안", Toast.LENGTH_SHORT);
                String addr = getString(R.string.streamingCage) + tmpFinalID;
//                Toast.makeText(rootActivity, addr, Toast.LENGTH_SHORT).show();
                webView.loadUrl(addr);
                webView.getSettings().setLoadWithOverviewMode(true);
                webView.getSettings().setUseWideViewPort(true);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Streaming")
                        .setView(webView)
                        .setNeutralButton("Done", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                destroyWebView(webView);
                            }
                        })
                        .setCancelable(true)
                        .show();
            }
        });

        Thread getThread;
        final Handler mHandler = new Handler();

        getThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for(;;) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            getCageStatus(finalID);
                        }
                    });
                    try
                    {
                        Thread.sleep(5000); //5초마다 온습도 정보 가져오게 하기위해서
                    }
                    catch(Exception e)
                    {
                        initStatus();
                        e.printStackTrace();
                    }
                }
            }
        });

        getThread.start();

        return rootView;
    }

    public String readId()
    {
        String str = null;
        try{
            BufferedReader br = new BufferedReader(new FileReader(getContext().getFilesDir() + "id.txt"));
            str = null;
            str = br.readLine();
            //Toast.makeText(getContext(), str+"readId 함수 안", Toast.LENGTH_SHORT).show();
        } catch (IOException e){
            e.printStackTrace();
        } catch(Exception e) {
            Toast.makeText(getContext(), "Exception", Toast.LENGTH_SHORT).show();
        }

        return str;
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (runFlag) {
            initStatus();
        }
    }

    public void destroyWebView(WebView mWebView) {
        mWebView.clearHistory();
        mWebView.clearCache(true);
        mWebView.loadUrl("about:blank");
        mWebView.onPause();
        mWebView.removeAllViews();
        mWebView.destroyDrawingCache();
        mWebView.pauseTimers();
        mWebView.destroy();
        mWebView = null;
    }

    public void initStatus()
    {
        textHumidity.setText(zero);
        progressHumi.setProgress(0);
        textTemperature.setText(zero);
        progressTemp.setProgress(0);
        textDate.setText("");
    }

    public void setStatus(String temp, String humi)
    {
        int Temp = Integer.parseInt(temp);
        int Humi = Integer.parseInt(humi);
        textTemperature.setText(temp + " / 100");
        progressTemp.setProgress(Temp);
        textHumidity.setText(humi + " / 100");
        progressHumi.setProgress(Humi);
    }

    public void getCageStatus(final String id) //DB에서 온습도 정보 가져오기
    {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String getCageStatusUrl = getString(R.string.getCageStatusUrl); //온습도를 받아오는 url
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
                        String date = jObject.optString("date"); // 1970년으로부터의 시간
                        String temp = jObject.optString("temp");
                        String humi = jObject.optString("humi");
                        long time = Long.parseLong(date);
                        long curTime = System.currentTimeMillis() / 1000;
                        long timePeriod = curTime - time;
                        if(timePeriod >= 300) {
                            return;
                        }
                        DateFormat df = new SimpleDateFormat("yyyy/MM/dd a hh:mm:ss");
                        Date dateObj = new Date(time * 1000);
                        String str = df.format(dateObj);
                        textDate.setText(str);
                        setStatus(temp, humi);
                    }
                    else
                    {
                        textHumidity.setText("?");
                        progressHumi.setProgress(0);
                        textTemperature.setText("?");
                        progressTemp.setProgress(0);
                        textDate.setText("");
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
                //Toast.makeText(getContext(), "시스템 오류", Toast.LENGTH_SHORT).show();
            }
        })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError
            {
                Map<String, String> params = new HashMap<>();
                params.put("ID", id);
                return params;
            }
        };
        queue.add(stringRequest);
    }
}