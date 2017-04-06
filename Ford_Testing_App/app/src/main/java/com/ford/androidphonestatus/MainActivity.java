package com.ford.androidphonestatus;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.WindowManager;



public class MainActivity extends AppCompatActivity {


    private static Activity mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //clearing the parse_cmd_list --List which
        PhoneStatusService.parse_cmd_result.clear();
        // Start the service
        Intent intent = new Intent(getApplicationContext(), PhoneStatusService.class);
        startService(intent);
        Log.d("Service", "PhoneStatusService Started");
    }

    @Override
    public void onDestroy() {

        this.stopService(new Intent(this, PhoneStatusService.class));
        super.onDestroy();
        Log.d("TAG", "In the onDestroy() event :PhoneStatusService Stopped");
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }
}
