package com.example.jinha.myapplication;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GraphFragmentTab3 extends Fragment {
    View layout;
    public int routemax = 0;
    public String[] route;
    GridView gridview;
    GridViewAdapter adapter;
    //GraphAsyncTask asyncTask;
    public Integer[] routeInt;
    /*
    JSONThread t;
    AdapterThread a;

    class JSONThread extends Thread{
        int i = 0;
        public void run(){
            getJsonObject("jinhak94");
        }
    }

    class AdapterThread extends Thread{
        public void run(){

        }
    }*/

    /*@Override
    public void onStart(){
        super.onStart();
        //t = new JSONThread();
        //a = new AdapterThread();
        //t.start();
        //a.start();
    }


    @Override
    public void onStop(){
        super.onStop();
    }*/

    @Override
    public void onResume(){
        super.onResume();
        getJsonObject("jinhak94");
        //t = new JSONThread();
        //a = new AdapterThread();
        //t.start();
        //a.start();
    }
/*
    public class GraphAsyncTask extends AsyncTask<String,Void,String> {

        public String result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            getJsonObject("jinhak94");
            return result;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }
*/
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        layout = inflater.inflate(R.layout.fragment_graph_tab3, container, false);
        getJsonObject("jinhak94");

        return layout;
    }
    public synchronized void getJsonObject(final String id)
    {
        RequestQueue queue = Volley.newRequestQueue(Objects.requireNonNull(getContext()));
        String url = getString(R.string.moveUrl);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>()
        {
            @Override
            public void onResponse(String response)
            {
                // Response
                try
                {
                    //wait();
                    JSONObject jsonMain = new JSONObject(response);
                    String result = jsonMain.optString("result");
                    String rt = jsonMain.optString("route");
                    route = rt.split(",");
                    routeInt = new Integer[route.length];

                    routemax = 0;
                    for(int i=0; i<route.length;i++){
                        routeInt[i] = Integer.parseInt(route[i]);
                        if (routemax < routeInt[i])
                            routemax = routeInt[i];
                    }

                    adapter = new GridViewAdapter(getContext(), routeInt);
                    gridview = layout.findViewById(R.id.gridView);
                    gridview.setAdapter(adapter);
                    gridview.setBackgroundResource(R.drawable.black_border);

                    //asyncTask = new GraphAsyncTask();
                    //asyncTask.execute();
                    //notify();
                    if(result.equals("1"))
                    {

                    }
                    else
                    {
                        Toast.makeText(getContext(), "아이디/비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "시스템 오류", Toast.LENGTH_SHORT).show();
            }
        })
        {
            @Override
            protected Map<String,String> getParams() throws AuthFailureError
            {
                Map<String, String> params = new HashMap<>();
                params.put("ID", id);
                return params;
            }
        };
        queue.add(stringRequest);
    }
}