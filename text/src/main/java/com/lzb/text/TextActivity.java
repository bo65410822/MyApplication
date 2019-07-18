package com.lzb.text;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.lzb.annotation.Router;

/**
 * author: Lzhb
 * created on: 2019/7/18 11:11
 * description:
 */
@Router(path = "/main/test")
public class TextActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        textView.setText("1222123121231");
        setContentView(textView);
    }
}
