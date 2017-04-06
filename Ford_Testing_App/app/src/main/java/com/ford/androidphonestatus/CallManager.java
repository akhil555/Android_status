package com.ford.androidphonestatus;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class CallManager {

	private static final String TAG = "CallManager";
	private static CallManager INSTANCE;
	private static Context mContext;
	@SuppressWarnings("unused")
	private static ContentResolver mCR;
	private TelephonyManager mTelephonyManager;
	private CallManager(Context c){
		mContext = c;
		mCR = c.getContentResolver();
	}

	public static CallManager getInstance(Context c){
		if(INSTANCE == null)
			INSTANCE = new CallManager(c);
		return INSTANCE;
	}
	
	public void deleteRecentCalls(){

		mContext.getContentResolver().delete(CallLog.Calls.CONTENT_URI, null, null);

	}

	
	
	
	public void dialCallNo(String number){
		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		callIntent.setData(Uri.parse("tel:"+number));
		mContext.startActivity(callIntent);
	}
	
	
	/* Dial call from the Phone book contact
	 * Parameter - <Name>: Name of the contact which need to be dialed */
	public void dialCallPhonebook(String name) {
		boolean isSuccess = false;
		try {
			String phoneNo = null;
			ContentResolver cr = mContext.getContentResolver();
			Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
					null, null, null);
			if(cur != null) {
				if (cur.getCount() > 0) {
					while (cur.moveToNext()) {
					String id = cur.getString(cur
								.getColumnIndex(ContactsContract.Contacts._ID));
						String displayName = cur
								.getString(cur
										.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

						if (displayName!= null && displayName.equalsIgnoreCase(name)) {
							if (Integer
									.parseInt(cur.getString(cur
											.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

								Cursor pCur = cr
										.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
												null,
												ContactsContract.CommonDataKinds.Phone.CONTACT_ID
														+ " = ?", new String[] { id },
												null);

								while (pCur != null && pCur.moveToNext()) {
									phoneNo = pCur
											.getString(pCur
													.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
									break;
								}
							}
							if (phoneNo != null && phoneNo.length() > 0) {
								PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Dialing contact from Phone book: %s", name)));
								dialCallNo(phoneNo);
							}else {
								PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to dial contact from Phone book: %s", name)));
							}
						}//else{
							// PhoneStatusService.parse_cmd_result =PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(TAG + " : dialCallPhonebook() 22222 Fail.No conatct found in phonebook"));
						//}
					}
				}else {
					PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage (String.format("Failed to dial contact from Phone book: %s", name)));
				}
			}else {
				PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to dial contact from Phone book: %s", name)));
			}
		} catch (NumberFormatException e) {
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to dail call from Phone book: %s", name)));
			PhoneStatusService.sendException(e, false);
		}
	}

	/* Fetch last incoming  call 
	 * Parameter - <None> */
	public String fetchLastIncomingCall(){
		String lastCallnumber = "";
		//fields to select.
        String[] strFields = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.CACHED_NUMBER_TYPE
        };

        //only incoming
        String strSelection = CallLog.Calls.TYPE
        + " = " + CallLog.Calls.INCOMING_TYPE;

        //most recent first
        String strOrder = CallLog.Calls.DATE + " DESC";

        try {
			//get a cursor
			Cursor mCallCursor = mContext.getContentResolver().query(
			    CallLog.Calls.CONTENT_URI, //content provider URI
			    strFields, //project (fields to get)
			    strSelection, //selection
			    null, //selection args
			    strOrder //sort order.
			);
			
			
			if (mCallCursor != null && mCallCursor.moveToFirst()) {
				lastCallnumber = mCallCursor.getString(0);
			}
		} catch (Exception e) {
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to fetch last Incoming Call Number")));
			PhoneStatusService.sendException(e, false);
		}
		
		if(lastCallnumber != null && lastCallnumber.length() > 0){
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Last Incoming Call Number: %s", lastCallnumber)));
		}else {
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Incoming Call not available")));
		}		
        return lastCallnumber;
	}

	/* Fetch last outgoing call 
	 * Parameter - <None> */
	public String fetchLastOutgoingCall(){
		//fields to select
        String[] strFields = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.CACHED_NUMBER_TYPE
        };

        //only outgoing
        String strSelection = CallLog.Calls.TYPE
        + " = " + CallLog.Calls.OUTGOING_TYPE;

        //most recent first
        String strOrder = CallLog.Calls.DATE + " DESC";


        //get a cursor
        Cursor mCallCursor = null;
		try {
			mCallCursor = mContext.getContentResolver().query(
			    CallLog.Calls.CONTENT_URI, //content provider URI
			    strFields, //project (fields to get)
			    strSelection, //selection
			    null, //selection argument
			    strOrder //sort order.
			);
		} catch (Exception e) {
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to fetch last Outgoing Call Number")));
			PhoneStatusService.sendException(e, false);
		}
        String lastCallnumber = "";
        
        if (mCallCursor != null && mCallCursor.moveToFirst()) {
        	lastCallnumber = mCallCursor.getString(0);
        }
        
        if(lastCallnumber != null && lastCallnumber.length() > 0){
        	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Last Outgoing Call Number: %s", lastCallnumber)));
		}else {
			 PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage (String.format("Outgoing Call not available")));
		}
        return lastCallnumber;
	}

	/* Re-dial last outgoing call
	 * Parameter - <None> */
	public String redialLastCall(){
		    String last_outgoing = fetchLastOutgoingCall();
		    if(last_outgoing != null && last_outgoing.length()>0) {
		    	dialCallNo(last_outgoing);
		    	 PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage (String.format("Redialing last outgoing call: %s", last_outgoing)));
		    }else {
		    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Outgoing Call not available")));
		    }
		    return last_outgoing;
	}

	/* Re-dial call from the Call History log
	 * Parameter - <Name>: Name of the contact from the Call History which need to be redialed */
	public void redialFromCallHistory(String name){
		//fields to select
        String[] strFields = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.CACHED_NUMBER_TYPE
        };   
        //most recent first
        String strOrder = CallLog.Calls.DATE + " DESC";
        //get a cursor
        Cursor mCallCursor = null;
        
        try {
			mCallCursor = mContext.getContentResolver().query(
			    CallLog.Calls.CONTENT_URI, //content provider URI
			    strFields, //project (fields to get)
			    null, //selection
			    null, //selection argument
			    strOrder //sort order.
			);
		} catch (Exception e) {
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(TAG + " : redialFromCallHistory() Fail"));
			PhoneStatusService.sendException(e, false);
		}
        
        int i = 0;
        if (mCallCursor != null && mCallCursor.moveToFirst()) {
            do {
            	if(mCallCursor == null)
            		break;
            	String number = mCallCursor.getString(0);
            	if(number!= null && number.length()>0&&number.equals(name)){
            		dialCallNo(number);
            		 PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage (String.format("Redialing from call history: %s", name)));
            		i = i+1;
            		break;
            	}

                String contactName = mCallCursor.getString(2);
            	if(contactName!= null && contactName.length()>0&&contactName.equalsIgnoreCase(name)){
            		dialCallNo(number);
            		 PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage (String.format("Redialing from call history: %s", name)));
            		i = i+1;
            		break;
            	}             
            }while (mCallCursor.moveToNext());
            if (i == 0){
            	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Call logs does not have: %s", name)));
            }
            }else
            {
            	 PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage (String.format("Call history is empty")));
            }
        }

	/* Enable/disable mobile internet data
	 *  Parameter - <True/False> */
	public void setMobileDataEnabled(boolean enabled) {
	   try {
		final ConnectivityManager conman = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		   final Class<?> conmanClass = Class.forName(conman.getClass().getName());
		   final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
		   iConnectivityManagerField.setAccessible(true);
		   final Object iConnectivityManager = iConnectivityManagerField.get(conman);
		   final Class<?> iConnectivityManagerClass =     Class.forName(iConnectivityManager.getClass().getName());
		   final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
		   setMobileDataEnabledMethod.setAccessible(true);
		   setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
		PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Mobile data: %s", enabled)));
	} catch (IllegalArgumentException e) {
		PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to set mobile data:Illegal Argument")));
		PhoneStatusService.sendException(e, false);
	} catch (ClassNotFoundException e) {
		PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to set mobile data :Class not Found")));
		PhoneStatusService.sendException(e, false);
	} catch (NoSuchFieldException e) {
		PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to set mobile data:No Such Field")));
		PhoneStatusService.sendException(e, false);
	} catch (IllegalAccessException e) {
		PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to set mobile data:Illegal Access Exception")));
		PhoneStatusService.sendException(e, false);
	} catch (NoSuchMethodException e) {
		PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to set mobile data:No such Method")));
		PhoneStatusService.sendException(e, false);
	} catch (InvocationTargetException e) {
		PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to set mobile data::Invocation Target Exception")));
		PhoneStatusService.sendException(e, false);
	}
	}
	
	
	/**
	 * 
	 * */
	
	public void getCallTimeStamp(String phoneNumber){
		
		String strOrder = CallLog.Calls.DATE + " DESC";
		
	   Uri callUri = Uri.parse("content://call_log/calls");

	    Cursor cur = mContext.getContentResolver().query(callUri, null, null, null, null);
	    System.out.println("count"+cur.getCount());
	    // loop through cursor
	    while (cur.moveToNext()) {

	     String callNumber = cur.getString(cur.getColumnIndex(CallLog.Calls.NUMBER));
	    if(callNumber.equalsIgnoreCase(phoneNumber)){
	     long callDate = cur.getLong(cur.getColumnIndex(CallLog.Calls.DATE));
	     SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(callDate);
			Log.i("time", "Call time"+formatter.format(calendar.getTime()));
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("Call time"+formatter.format(calendar.getTime())));
	    }
	    }	}
	
	
	
	/**
	 * 
	 * */
}
