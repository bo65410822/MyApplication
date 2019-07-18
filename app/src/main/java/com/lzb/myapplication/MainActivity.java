package com.lzb.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.lzb.core.LZBRouter;

//import com.lzb.db_library.BaseDao;
//import com.lzb.db_library.DbFactory;

//@Router(path = "/main/test")
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //一般在application中声明
        LZBRouter.getInstance().init(this.getApplication());

    }

    public void test(View view) {
        LZBRouter.getInstance().navigation("/main/test", this);
    }
}
