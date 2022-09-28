package com.example.ping;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //urls规则可以看Ping里面的getIp方法，从第7位开始到下一个：截至，获取具体域名来ping
                Ping.startSniffer("https://www.baidu.com:");
            }
        });
    }
}