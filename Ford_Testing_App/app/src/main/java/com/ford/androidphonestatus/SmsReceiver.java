package com.ford.androidphonestatus;

//written By :AKhil Jumade (AK314314)
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.ford.androidphonestatus.DataBaseHelper;
import java.util.Calendar;


public class SmsReceiver extends BroadcastReceiver {

private Context context;
private String msg_from;
//private MessageDataBaseAdapter messageDataBaseAdapter;
private String msgBody;
public static final String SMS_EXTRA_NAME = "pdus";
public static final String SMS_URI = "content://sms//inbox";

public static final String ADDRESS = "address";
public static final String PERSON = "person";
public static final String DATE = "date";
public static final String READ = "read";
public static final String STATUS = "status";
public static final String TYPE = "type";
public static final String BODY = "body";
public static final String SEEN = "seen";

public static final int MESSAGE_TYPE_INBOX = 1;
public static final int MESSAGE_TYPE_SENT = 2;

public static final int MESSAGE_IS_NOT_READ = 0;
public static final int MESSAGE_IS_READ = 1;

public static final int MESSAGE_IS_NOT_SEEN = 0;
public static final int MESSAGE_IS_SEEN = 1;

@Override
public void onReceive(Context context, Intent intent) {
    this.context = context;
    //android.provider.Telephony.SMS_DELIVER
    if (intent.getAction().equals("android.provider.Telephony.SMS_DELIVER")) {
        

        Bundle bundle = intent.getExtras(); // ---get the SMS message passed
        // Get ContentResolver object for pushing encrypted SMS to incoming folder
        ContentResolver contentResolver = context.getContentResolver();                             // in---
        SmsMessage[] msgs = null;

        if (bundle != null) {
            // ---retrieve the SMS message received---
            try {
                Object[] pdus = (Object[]) bundle.get("pdus");
                msgs = new SmsMessage[pdus.length];
                // String strMessageFrom =
                // bundle.getDisplayOriginatingAddress();
                for (int i = 0; i < msgs.length; i++) {
                    //Calendar c = Calendar.getInstance();
                    // System.out.println("Current time => "+c.getTime());
                    // SCSLToast.showShortToast(c.getTime().toString());
                    //SimpleDateFormat date = new SimpleDateFormat(
                    //      "dd-MMM-yyyy hh:mm:ss a");
                    //String formattedDate = date.format(c.getTime());

                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    msg_from = msgs[i].getOriginatingAddress();
                    msgBody = msgs[i].getMessageBody();                     
                    //Db operation
                     saveSmsDataToDefaulDB( contentResolver, msgs[i] );
                    
                }
                ///
                } catch (Exception e) {
                // Log.d("Exception caught",e.getMessage());
                //messageDataBaseAdapter.close();
            }
        }

    }
}

/*Additional/optional code for storing SMS to Default Database
private void putSmsToDatabase( SmsMessage sms, Context context )
{
	DataBaseHelper dataBaseHelper = new DataBaseHelper(context);
	
	SQLiteDatabase db = dataBaseHelper.getWritableDatabase();
	
	String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime(    ));
	// Create SMS row
	ContentValues values = new ContentValues();
	
	values.put( ADDRESS , sms.getOriginatingAddress().toString() );
	values.put(DATE, mydate);
	values.put(BODY, sms.getMessageBody().toString());
	// values.put( READ, MESSAGE_IS_NOT_READ );
	// values.put( STATUS, sms.getStatus() );
	// values.put( TYPE, MESSAGE_TYPE_INBOX );
	// values.put( SEEN, MESSAGE_IS_NOT_SEEN );
	
	db.insert(SMS_URI, null, values);
	
	db.close();

} */
private void saveSmsDataToDefaulDB( ContentResolver contentResolver, SmsMessage sms )
 {
     // Create SMS row
     ContentValues values = new ContentValues();
     values.put( ADDRESS, sms.getOriginatingAddress());
     values.put( DATE, sms.getTimestampMillis());
     values.put( READ, MESSAGE_IS_NOT_READ);
     values.put( STATUS, sms.getStatus());
     values.put( TYPE, MESSAGE_TYPE_INBOX);
     values.put( SEEN, MESSAGE_IS_NOT_SEEN);
     values.put( BODY, sms.getMessageBody());  
     contentResolver.insert( Uri.parse(SMS_URI), values);
     
     
     //HideSMSToast.showShortToast("Content written in default message app db");
 }}