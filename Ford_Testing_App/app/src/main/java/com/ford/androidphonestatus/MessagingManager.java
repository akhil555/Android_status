package com.ford.androidphonestatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;


@SuppressLint("NewApi") public class MessagingManager {

	private static final String TAG = "MessagingManager";
	private static MessagingManager INSTANCE;
	private static Context mContext;
	int unreadMessagesCount = 0;
	@SuppressWarnings("unused")
	private static ContentResolver mCR;

	private MessagingManager(Context c){
		mContext = c;
		mCR = c.getContentResolver();
	}

	public static MessagingManager getInstance(Context c){
		if(INSTANCE == null)
			INSTANCE = new MessagingManager(c);
		return INSTANCE;
	}

	public void sendSMS(String text,String ...numbers){
		if(numbers != null && numbers.length>0){
			for(String num : numbers){
				try {     
					// Get the default instance of the SmsManager
					SmsManager smsManager = SmsManager.getDefault();
					smsManager.sendTextMessage(num, 
							null,  
							text, 
							null, 
							null);
					PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Sending SMS to the number: %s", num)));
				} catch (Exception e) {
					PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed sending SMS to the number: %s", num)));
					PhoneStatusService.sendException(e, false);
				}
			}
		}
	}
	
	


	public int deleteMessageByBody(String text){
		int deletecount =0;
		try {
	        Uri uriSms = Uri.parse("content://sms/inbox");
	        Cursor c = mContext.getContentResolver().query(uriSms,
	            new String[] { "_id", "thread_id", "address",
	                "person", "date", "body" }, null, null, null);
	        if (c != null && c.moveToFirst()) {
	            do {
	                long id = c.getLong(0);
	                String body = c.getString(5);

	                if (body.equals(text)) {
	                    mContext.getContentResolver().delete(
	                        Uri.parse("content://sms/" + id), null, null);
	                    deletecount = deletecount+1;
	                   PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Deleted SMS with body content: %s", text)));
	                }
	            } while (c.moveToNext());
	        }
	    } catch (Exception e) {
	    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed deleting SMS with body content: %s", text)));
			PhoneStatusService.sendException(e, false);
	    	}
	    if(deletecount <= 0){
	    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("No SMS found with the body content: %s", text)));
	    }
	    return deletecount;
	}

	public int deleteMessageByNumber(String number){
		int deletecount = 0;
		try {
	        Uri uriSms = Uri.parse("content://sms/inbox");
	        Cursor c = mContext.getContentResolver().query(uriSms,
	            new String[] { "_id", "thread_id", "address",
	                "person", "date", "body" }, null, null, null);

	        if (c != null && c.moveToFirst()) {
	            do {
	                long id = c.getLong(0);
	                String address = c.getString(2);
	                if (address.equals(number)) {
	                    mContext.getContentResolver().delete(
	                        Uri.parse("content://sms/" + id), null, null);
	                    deletecount++;
	                   PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Deleted SMS with number: %s", number)));
	                }
	            } while (c.moveToNext());
	        }
	    } catch (Exception e) {
	    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed deleting SMS with number: %s", number)));	
			PhoneStatusService.sendException(e, false);
	    	}
	    if(deletecount <= 0){
	    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("No SMS found with number: %s", number)));
	    }
		return deletecount;
	}

 public int deleteMessageAll(){
	 int retVal = 0;
		 String defaultSmsApp = Sms.getDefaultSmsPackage(mContext);
		 final String myPackageName = getClass().getPackage().getName();
		
		 if (!Sms.getDefaultSmsPackage(mContext).equals(myPackageName)) {
			
		 Intent intent = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
		 
		 intent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
	
		 
		try {
	        Uri uriSms = Uri.parse("content://sms/inbox");
	        Cursor c = mContext.getContentResolver().query(uriSms,
	            new String[] { "_id"}, null, null, null);

	        if (c != null && c.moveToFirst()) {
	            do {
	                long id = c.getLong(0);
	         
	                mContext.getContentResolver().delete(
	                        Uri.parse("content://sms/" + id), null, null);
	                retVal++;
	            } while (c.moveToNext());
	           PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Deleted all SMS")));
	        }
	    } catch (Exception e) {
	    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed deleting SMS")));
			PhoneStatusService.sendException(e, false);
	    	}
	    if(retVal <= 0){
	    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Inbox is empty")));
	    }
		 }
		 else{
			 System.out.println("equal");
			 Intent intent = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
			 intent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, defaultSmsApp);
			 
		 }
		return retVal;
	}
	
	public void readMessage(String NameOrNumber){
//		 Uri mSmsinboxQueryUri = Uri.parse("content://sms/inbox");
//         Cursor cursor1 = mContext.getContentResolver().query(mSmsinboxQueryUri,new String[] { "_id", "thread_id", "address", "person", "date","body", "type" }, null, null, null);
//        
//         String[] columns = new String[] { "address", "person", "date", "body","type" };
//         if (cursor1.getCount() > 0) {
//        	 System.out.println("cursor count"+cursor1.getCount());
//          
//            while (cursor1.moveToNext()){
//                     String address = cursor1.getString(cursor1.getColumnIndex(columns[0])).toString();
//              
//                if(address.contains(number)){ //put your number here
//                	System.out.println("equal");
//                     String name = cursor1.getString(cursor1.getColumnIndex(columns[1]));
//                     long date = cursor1.getLong(cursor1.getColumnIndex(columns[2]));
//                     String body = cursor1.getString(cursor1.getColumnIndex(columns[3]));
//                     String type = cursor1.getString(cursor1.getColumnIndex(columns[4]));
//                     SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
//         			Calendar calendar = Calendar.getInstance();
//        			calendar.setTimeInMillis(date);
//                   PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("Message time"+formatter.format(calendar.getTime())+body);
//               }        }
//         }
		
		long req_thread_id = -1;
		String where = ContactsContract.Data.DISPLAY_NAME + " = ? ";
		String[] params = new String[] { NameOrNumber };

		boolean isContactAvailable = false;
		String number = null ;
		Cursor cur = mCR.query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
				null, null, null);
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				String existName = cur
						.getString(cur
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (existName != null && existName.equalsIgnoreCase(NameOrNumber)) {
					number =  cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					isContactAvailable = true;
					break;
				}
			}
		}
	try {
		Uri uriSms = Uri.parse("content://sms/inbox/");
		Cursor c = mContext.getContentResolver().query(
				uriSms,
				new String[] { "_id", "thread_id", "address", "person",
						"date", "body" }, null, null, null);

		if (c != null && c.moveToFirst()) {
			do {
				long id = c.getLong(0);
				long threadId = c.getLong(1);
				String address = c.getString(2);
				//Log.e(TAG , "address --> "+address);
				String person = c.getString(3);
				//Log.e(TAG , "address --> "+person);
				String body = c.getString(5);
				long date = c.getLong(4);

				if (address.contains(NameOrNumber) || (isContactAvailable && number!= null && address.contains(number))) {
					req_thread_id = threadId;
					  SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
						Calendar calendar = Calendar.getInstance();
						calendar.setTimeInMillis(date);
						Log.i("time", "Call time"+formatter.format(calendar.getTime()));
						PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("Message time"+formatter.format(calendar.getTime())+body));
					System.out.println("msg body is"+body);
					//break;
					
				}
				
			} while (c.moveToNext());
		}
	}
		 catch (Exception e) {
				PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to open sms with the number: %s", NameOrNumber)));
				PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(TAG + " : openMessage() Fail"));
				PhoneStatusService.sendException(e, false);
			}         
         
         
	}
	
	public int readMessageByNumber(String number){
		
		
		int count = 0;
		int readcount = 0;
		Uri Sms_readmsg = Uri.parse("content://sms/inbox");
		Cursor c = mContext.getContentResolver().query(Sms_readmsg, new String[] { "_id", "thread_id", "address",
                "person", "date", "body" }, null, null, null);
		 if (c != null && c.moveToFirst()) {
			 do{	
				
				String address = c.getString(2);
             if(address.equals(number)){
            	 count = count +1;
            
                
            	 readcount = count-getUnreadSMSCount();
            	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Number of sms read count from number",readcount)));
                  		readcount++;
            			        } 
				 }
             
		 while(c.moveToNext());
		 }                   
             else{
            	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Messages are unread")));
             }
  			
		return readcount;
	}

	
	
	public int unreadMessageByNumber(String number){
		int unreadcount = 0;
		
		Uri Sms_unreadmsg = Uri.parse("content://sms/inbox");
		Cursor c = mContext.getContentResolver().query(Sms_unreadmsg, new String[] { "_id", "thread_id", "address",
                "person", "date", "body" }, "read = 0", null, null);
		
		if (c != null && c.moveToFirst()) {
		do{
				 String address = c.getString(2);
				 if(address.equals(number)){
					unreadcount = c.getCount();
					c.close();
					PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Number of sms read count from number",unreadcount)));
               		unreadcount++;	
				 }
			 }
			 while(c.moveToNext());
		}
//		 else{
//			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Messages are read"));
//		 }
		return unreadcount;
	}
	
	
	/**
	 * 
	 * */
	

	public int getUnreadSMSNumber(){
		try {
			Uri SMS_INBOX = Uri.parse("content://sms/inbox");
			Cursor c = mContext.getContentResolver().query(SMS_INBOX, null, "read = 0", null, null);
			unreadMessagesCount = c.getCount();

			Toast.makeText(mContext,"get unread sms count"+unreadMessagesCount,
					Toast.LENGTH_LONG).show();
			c.close();
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Number of unread sms count: %s", unreadMessagesCount)));
		} catch (Exception e) {
			Toast.makeText(mContext,"Failed to get unread sms count",
					Toast.LENGTH_LONG).show();
		}
		return unreadMessagesCount;
	}
	
	
	public int getReadSMSNumber(){
		
		int count = 0;
		int readSMSCount = 0;
		try {
			Uri uriSms = Uri.parse("content://sms/inbox");
			Cursor c = mContext.getContentResolver().query(uriSms,
					new String[] { "_id"}, null, null, null);
			if (c != null && c.moveToFirst()) {
				do {
					count = count +1;
				} while (c.moveToNext());
				readSMSCount = count-unreadMessagesCount;
				System.out.println("read count is"+readSMSCount);
				Toast.makeText(mContext,"read sms count"+readSMSCount,Toast.LENGTH_LONG).show();
				 PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Number of read sms count: %s", readSMSCount)));
			}
			else{
					PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Inbox is empty")));
			}
		} catch (Exception e) {
			Toast.makeText(mContext,"Failed to get read sms count",
					Toast.LENGTH_LONG).show();
		}
		return readSMSCount;
	}
	
	
	public int getTotalSMSNumber(){
		int count =0;
		try {
			Uri uriSms = Uri.parse("content://sms/inbox");
			Cursor c = mContext.getContentResolver().query(uriSms,
					new String[] { "_id"}, null, null, null);

			if (c != null && c.moveToFirst()) {
				do {
					count = count +1;
				} while (c.moveToNext());

//				Toast.makeText(mContext,"total sms count"+count,
//						Toast.LENGTH_LONG).show();

				  PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Total sms count: %s", count)));
			}
			else{

				Toast.makeText(mContext,"inbox is empty",
						Toast.LENGTH_LONG).show();

					PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Inbox is empty")));
			}
		} catch (Exception e) {
			Toast.makeText(mContext,"Failed to get total sms count",
					Toast.LENGTH_LONG).show();
		}
		return count;
	}
	
	
public void getMessageDate(String phonenumber){
		
		Uri uri = Uri.parse("content://sms/");
		ContentResolver contentResolver = mCR;
		String sms = "address='"+phonenumber+"'";
		Cursor cursor = contentResolver.query(uri, new String[]{"_id","date"}, sms, null, null);
		System.out.println("cursor count"+cursor.getCount());
		while(cursor.moveToNext()){

			
			long strdate = cursor.getLong(cursor.getColumnIndex("date"));
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(strdate);

			Log.i("time", "Message time"+formatter.format(calendar.getTime()));
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("Message time"+formatter.format(calendar.getTime()))) ;
		}
	}
	

/**
 * 
 * */
	
	
	public int getUnreadSMSCount(){
		int unreadMessagesCount = 0;
		try {
			Uri SMS_INBOX = Uri.parse("content://sms/inbox");
			Cursor c = mContext.getContentResolver().query(SMS_INBOX, null, "read = 0", null, null);
			unreadMessagesCount = c.getCount();
			c.close();
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Number of unread sms count: %s", unreadMessagesCount)));
		 } catch (Exception e) {
		    	Toast.makeText(mContext,"Failed to get unread sms count",
						Toast.LENGTH_LONG).show();
		    	}
			return unreadMessagesCount;
	}

	public int getReadSMSCount() {
		int count = 0;
		int readSMSCount = 0;
		try {
	        Uri uriSms = Uri.parse("content://sms/inbox");
	        Cursor c = mContext.getContentResolver().query(uriSms,
	            new String[] { "_id"}, null, null, null);
	        if (c != null && c.moveToFirst()) {
	            do {
	            	count = count +1;
	            } while (c.moveToNext());
	            readSMSCount = count-getUnreadSMSCount();
	           PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Number of read sms count: %s", readSMSCount)));
	        }
	        else{
	        	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Inbox is empty")));
	        }
	    } catch (Exception e) {
	    	Toast.makeText(mContext,"Failed to get read sms count",
					Toast.LENGTH_LONG).show();
	    	}
		return readSMSCount;
	}

	public int getTotalSMSCount(){
		int count =0;
		try {
	        Uri uriSms = Uri.parse("content://sms/inbox");
	        Cursor c = mContext.getContentResolver().query(uriSms,
	            new String[] { "_id"}, null, null, null);

	        if (c != null && c.moveToFirst()) {
	            do {
	            	count = count +1;
	            } while (c.moveToNext());
	           PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Total sms count: %s", count)));
	        }
	        else{
	        	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Inbox is empty")));
	        }
	    } catch (Exception e) {
	    	Toast.makeText(mContext,"Failed to get total sms count",
					Toast.LENGTH_LONG).show();
	    	}
	    return count;
	}

	@SuppressWarnings("unused")
	public void openMessageByBodyText(String bodyText) {
		long req_thread_id = -1;
		try {
			Uri uriSms = Uri.parse("content://sms/inbox");
			Cursor c = mContext.getContentResolver().query(
					uriSms,
					new String[] { "_id", "thread_id", "address", "person",
							"date", "body" }, null, null, null);

			if (c != null && c.moveToFirst()) {
				do {
					long id = c.getLong(0);
					long threadId = c.getLong(1);
					String address = c.getString(2);
					String person = c.getString(3);
					String body = c.getString(5);

					if (body.equals(bodyText)) {
						req_thread_id = threadId;
						break;
					}
				} while (c.moveToNext());
			}
			if(req_thread_id != -1 ) {
				Intent defineIntent = new Intent(Intent.ACTION_VIEW);
				defineIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				defineIntent.setData(Uri.parse("content://mms-sms/conversations/"
						+ req_thread_id));
				mContext.startActivity(defineIntent);
				PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Opened sms with the body: %s", bodyText)));
			}else{
				PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("SMS not found with body content: %s", bodyText)));
			}
		} catch (Exception e) {
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to open sms with the body: %s", bodyText)));
			PhoneStatusService.sendException(e, false);
		}
	}

	public void test(){
		Uri mSmsinboxQueryUri = Uri.parse("content://sms");
    Cursor cursor1 = mContext.getContentResolver().query(
            mSmsinboxQueryUri,
            new String[] { "_id", "thread_id", "address", "person", "date",
                    "body", "type" }, null, null, null);
    ((Activity) mContext).startManagingCursor(cursor1);
    String[] columns = new String[] { "address", "person", "date", "body",
            "type" };
    if (cursor1.getCount() > 0) {
        String count = Integer.toString(cursor1.getCount());
        Log.e("Count",count);
        while (cursor1.moveToNext()) {
            String address = cursor1.getString(cursor1
                    .getColumnIndex(columns[0]));
            String name = cursor1.getString(cursor1
                    .getColumnIndex(columns[1]));
            String date = cursor1.getString(cursor1
                    .getColumnIndex(columns[2]));
            String msg = cursor1.getString(cursor1
                    .getColumnIndex(columns[3]));
            String type = cursor1.getString(cursor1
                    .getColumnIndex(columns[4]));
            if(name.contains(""))
           Log.e(TAG ,"Address:" + address + "\n"
                    + "Name:" + name + "\n"
                    + "Date:" + date + "\n"
                    + "MSG:" + msg + "\n"
                    + "type:" + type + "\n\n\n\n\n\n\n\n\n"
                    );
           Log.e(TAG ,"\n*****************************");
           

       }
   }
    }
	
	@SuppressWarnings("unused")
	public void openMessageByNumber(String NameOrNumber) {
		long req_thread_id = -1;
			String where = ContactsContract.Data.DISPLAY_NAME + " = ? ";
			String[] params = new String[] { NameOrNumber };

			boolean isContactAvailable = false;
			String number = null ;
			Cursor cur = mCR.query(
					ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
					null, null, null);
			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					String existName = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (existName != null && existName.equalsIgnoreCase(NameOrNumber)) {
						number =  cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
						isContactAvailable = true;
						break;
					}
				}
			}
		try {
			Uri uriSms = Uri.parse("content://sms/inbox");
			Cursor c = mContext.getContentResolver().query(
					uriSms,
					new String[] { "_id", "thread_id", "address", "person",
							"date", "body" }, null, null, null);

			if (c != null && c.moveToFirst()) {
				do {
					long id = c.getLong(0);
					long threadId = c.getLong(1);
					String address = c.getString(2);
					Log.e(TAG , "address --> "+address);
					String person = c.getString(3);
					Log.e(TAG , "address --> "+person);
					String body = c.getString(5);

					if (address.contains(NameOrNumber) || (isContactAvailable && number!= null && address.contains(number))) {
						req_thread_id = threadId;
						break;
					}
				} while (c.moveToNext());
			}
			if(req_thread_id != -1 ) {
				Intent defineIntent = new Intent(Intent.ACTION_VIEW);
				defineIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				defineIntent.setData(Uri.parse("content://mms-sms/conversations/"
						+ req_thread_id));
				mContext.startActivity(defineIntent);
				PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Opened sms with the number: %s", NameOrNumber)));
			}else{
				PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to open sms with the number: %s", NameOrNumber)));
			}		
		} catch (Exception e) {
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to open sms with the number: %s", NameOrNumber)));
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(TAG + " : openMessage() Fail"));
			PhoneStatusService.sendException(e, false);
		}
	}
	
	public void getAllSms(String folderName) {
		
		 String[] columns = new String[] { "address", "person", "date", "body",
         "type" };
		
		Uri message = Uri.parse("content://sms/"+folderName);
	    ContentResolver cr = mContext.getContentResolver();

	    Cursor c = cr.query(message, null, null, null, null);
	 	    String sms = "";
	      while (c.moveToNext()) {
	    	  String msg = c.getString(c
	                    .getColumnIndex(columns[3]));
	          sms += "From :" + c.getString(2) + " : " + msg+"\n"; 
	         
	         PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(TAG + sms));
	      }
		
	}
	
	
	
	
	 void markMessageRead(Context context ,String NameOrNumber) {
		 if (getTotalSMSCount() > 0) {
        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        try{

        while (cursor.moveToNext()) {
        	if ((cursor.getString(cursor.getColumnIndex("address")).equals(NameOrNumber)) && (cursor.getInt(cursor.getColumnIndex("read")) == 0)) {
                        String SmsMessageId = cursor.getString(cursor.getColumnIndex("_id"));
                        ContentValues values = new ContentValues();
                        values.put("read", true);
                        context.getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id=" + SmsMessageId, null);
                        PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Opened sms with the number: %s", NameOrNumber)));
                        return;
                    }
        }
               
            
  }catch(Exception e)
  {
      Log.e("Mark Read", "Error in Read: "+e.toString());
  }
		 }
		 else{
			 PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Inbox Empty")));
		 }
}
	 
	 
	 void markAllMessageRead(Context context) {
		 	if (getTotalSMSCount() > 0) {
	        Uri uri = Uri.parse("content://sms/inbox");
	        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
	        try{
	        	
	        while (cursor.moveToNext()) {
	        	
	                        String SmsMessageId = cursor.getString(cursor.getColumnIndex("_id"));
	                        ContentValues values = new ContentValues();
	                        values.put("read", true);
	                        context.getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id=" + SmsMessageId, null);
	                        PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Read all Messages")));
	                       
	                   
	        }
	               
	            
	  }catch(Exception e)
	  {
	      Log.e("Mark Read", "Error in Read: "+e.toString());
	  }
		 	}
		 	else {
		 		 PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Inbox Empty")));
		 	}
	}	 
	
}