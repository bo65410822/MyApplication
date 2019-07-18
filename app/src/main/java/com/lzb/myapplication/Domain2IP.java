package com.lzb.myapplication;

import android.os.AsyncTask;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

/**
 * author: Lzhb
 * created on: 2019/7/18 9:47
 * description:
 */

public class Domain2IP {
    public static String getINetAddress(String host) {

        MyAsyncTask myAsyncTask = new MyAsyncTask();
        AsyncTask<String, Integer, String> execute = myAsyncTask.execute(host);
        String s = null;
        try {
            s = execute.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return s;

    }

    public static class MyAsyncTask extends AsyncTask<String, Integer, String> {
        public String ipAddress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            String hostAddress = null;
            try {
                InetAddress inetAddress = InetAddress.getByName(strings[0]);
                hostAddress = inetAddress.getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            return hostAddress;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            this.ipAddress = s;
        }
    }
}
