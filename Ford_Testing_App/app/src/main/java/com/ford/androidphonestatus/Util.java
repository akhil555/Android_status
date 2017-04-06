package com.ford.androidphonestatus;

import java.sql.Timestamp;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.telephony.CellInfoGsm;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.TelephonyManager;

public class Util {
	

	
	/* Return Mobile phone name */
	public static String generateDeviceInfo() {        
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        String deviceName = myDevice.getName();
        
        StringBuilder sb = new StringBuilder();
        if(deviceName != null) {
        	 sb.append("Device Name : "+deviceName + " \n");
        }
        return  sb.toString();    
	}
	


	/* Provide detailed information of the Mobile phone*/
	public static String detailedDeviceInfo() {
        String RELEASE = android.os.Build.VERSION.RELEASE;
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        String MODEL = android.os.Build.MODEL; 
        String BRAND = android.os.Build.BRAND; 
        String MANUFACTURER = android.os.Build.MANUFACTURER; 
        
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        String deviceName = myDevice.getName();
        
        StringBuilder sb = new StringBuilder();
        if(deviceName != null) {
        	 sb.append("Device Name : "+deviceName + " \n");
        }
        sb.append("Device Info : \n"+
               "RELEASE : "+ RELEASE+ " \n" + 
               "SDK_INT : "+ SDK_INT+ " \n" + 
               "MODEL : "+ MODEL+ " \n" + 
               "BRAND : "+ BRAND+ " \n" + 
               "MANUFACTURER : "+ MANUFACTURER+ " \n\n" );
        return  sb.toString();    
	}
    
	public static String getTimestamp(){
		return String.format("%1$-23s", new Timestamp(System.currentTimeMillis()));
	}
	


}