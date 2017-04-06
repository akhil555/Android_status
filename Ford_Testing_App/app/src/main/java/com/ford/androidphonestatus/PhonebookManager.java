package com.ford.androidphonestatus;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class PhonebookManager {

	private static final String TAG = "PhonebookManager";
	private static PhonebookManager INSTANCE;
	private static Context mContext;
	private static ContentResolver mCR;

	private PhonebookManager(Context c) {
		mContext = c;
		mCR = c.getContentResolver();
	}

	public static PhonebookManager getInstance(Context c) {
		if (INSTANCE == null)
			INSTANCE = new PhonebookManager(c);
		return INSTANCE;
	}
	
	/**
	 * 
	 * Delete Contacts
	 * */
	
	public void deleteAllContacts(){
		ContentResolver contentResolver = mContext.getContentResolver();
		Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		while(cursor.moveToNext()){

			String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
			Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
			contentResolver.delete(uri, null, null);

		}
		PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("All contacts are deleted")));
	}

	

	/*
	 * Delete Phone Book contact Parameter - <Contact Name>
	 */
	public void deleteContact(String name) {
		try {
			String where = ContactsContract.Data.DISPLAY_NAME + " = ? ";
			String[] params = new String[] { name };

			boolean isContactAvailable = false;

			Cursor cur = mCR.query(
					Phone.CONTENT_URI, null,
					null, null, null);
			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					String existName = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (existName != null && existName.equalsIgnoreCase(name)) {
						isContactAvailable = true;
						break;
					}
				}
			}

			if (isContactAvailable) {
				ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
				ops.add(ContentProviderOperation
						.newDelete(ContactsContract.RawContacts.CONTENT_URI)
						.withSelection(where, params).build());
				mCR.applyBatch(ContactsContract.AUTHORITY, ops);
				PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
						"Deleted Contact: %s", name)));
			} else {
				PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to delete the contact: %s", name)+" No such conatct in the phonebook"));
			}
		} catch (RemoteException e) {
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Failed to delete the contact: %s", name)));
			PhoneStatusService.sendException(e, false);
		} catch (OperationApplicationException e) {
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Failed to delete the contact: %s", name)));
			PhoneStatusService.sendException(e, false);
		}
	}

	/*
	 * Fetch total contact count Parameter - <None>
	 */
	public int getTotalContactCount() {
		Cursor cur = mCR.query(ContactsContract.Contacts.CONTENT_URI, null,
				null, null, null);
		int count = 0;
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				count = count + 1;
			}
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Total contact count: %s", count)));
		} else {
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Total contact count: %s", count)));
		}
		return count;
	}


	/*
	 * Add contact in the Phone Book Parameter: <Name>, <Mobile Number>, <Home
	 * Number>, <Work Number>, <Email-Id>, <Company>
	 */
	//public void addContact(String displayName, String... params) {
	public void addContact(String displayName, String mobileNumber,
						   String homeNumber,String workNumber,String emailID,
						   String company,String jobTitle) {
//		String mobileNumber = null;
//		String homeNumber = null;
//		String workNumber = null;
//		String emailID = null;
//		String company = null;
//		String jobTitle = null;
//
//		try {
//			mobileNumber = params[0];
//			homeNumber = params[1];
//			workNumber = params[2];
//			emailID = params[3];
//			company = params[4];
//			jobTitle = params[5];
//		} catch (Exception e1) {
//			// Ignore
//		}
		try {
			Cursor cur = mCR.query(ContactsContract.Contacts.CONTENT_URI, null,
					null, null, null);

			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					String existName = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (existName != null && existName.equals(displayName)) {
						return;
					}
				}
			}

			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation
					.newInsert(ContactsContract.RawContacts.CONTENT_URI)
					.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
					.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
					.build());

			// ------------------------------------------------------ Names
			if (displayName != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
								displayName).build());
			}
			// ------------------------------------------------------ Mobile
			// Number
			if (mobileNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Phone.NUMBER,
								mobileNumber)
						.withValue(
								ContactsContract.CommonDataKinds.Phone.TYPE,
								ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
						.build());
			}

			// ------------------------------------------------------ Home
			// Numbers
			if (homeNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Phone.NUMBER,
								homeNumber)
						.withValue(
								ContactsContract.CommonDataKinds.Phone.TYPE,
								ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
						.build());
			}

			// ------------------------------------------------------ Work
			// Numbers
			if (workNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Phone.NUMBER,
								workNumber)
						.withValue(
								ContactsContract.CommonDataKinds.Phone.TYPE,
								ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------ Email
			if (emailID != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Email.DATA,
								emailID)
						.withValue(
								ContactsContract.CommonDataKinds.Email.TYPE,
								ContactsContract.CommonDataKinds.Email.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------
			// Organization
			if (company != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.COMPANY,
								company)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}
			if (jobTitle != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TITLE,
								jobTitle)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}



			// Asking the Contact provider to create a new contact
			mCR.applyBatch(ContactsContract.AUTHORITY, ops);
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Adding Contact: %s",
					displayName)));
		} catch (Exception e) {
			Toast.makeText(mContext, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();

			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Failed to add contact: %s", displayName)));
			PhoneStatusService.sendException(e, false);
		}
	}
	public void addContact_new(String displayName, String mobileNumber,
						   String homeNumber,String workNumber,String emailID,
						   String company,String jobTitle,Bitmap mBitmap) {
//		String mobileNumber = null;
//		String homeNumber = null;
//		String workNumber = null;
//		String emailID = null;
//		String company = null;
//		String jobTitle = null;
//
//		try {
//			mobileNumber = params[0];
//			homeNumber = params[1];
//			workNumber = params[2];
//			emailID = params[3];
//			company = params[4];
//			jobTitle = params[5];
//		} catch (Exception e1) {
//			// Ignore
//		}
		try {
			Cursor cur = mCR.query(ContactsContract.Contacts.CONTENT_URI, null,
					null, null, null);

			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					String existName = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (existName != null && existName.equals(displayName)) {
						return;
					}
				}
			}

			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation
					.newInsert(ContactsContract.RawContacts.CONTENT_URI)
					.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
					.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
					.build());

			// ------------------------------------------------------ Names
			if (displayName != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
								displayName).build());
			}
			// ------------------------------------------------------ Mobile
			// Number
			if (mobileNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Phone.NUMBER,
								mobileNumber)
						.withValue(
								ContactsContract.CommonDataKinds.Phone.TYPE,
								ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
						.build());
			}

			// ------------------------------------------------------ Home
			// Numbers
			if (homeNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Phone.NUMBER,
								homeNumber)
						.withValue(
								ContactsContract.CommonDataKinds.Phone.TYPE,
								ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
						.build());
			}

			// ------------------------------------------------------ Work
			// Numbers
			if (workNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Phone.NUMBER,
								workNumber)
						.withValue(
								ContactsContract.CommonDataKinds.Phone.TYPE,
								ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------ Email
			if (emailID != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Email.DATA,
								emailID)
						.withValue(
								ContactsContract.CommonDataKinds.Email.TYPE,
								ContactsContract.CommonDataKinds.Email.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------
			// Organization
			if (company != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.COMPANY,
								company)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}
			if (jobTitle != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TITLE,
								jobTitle)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}
			if(mBitmap!=null) {

				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				// If an image is selected successfully
				mBitmap.compress(Bitmap.CompressFormat.PNG, 75, stream);

				// Adding insert operation to operations list
				// to insert Photo in the table ContactsContract.Data
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
						.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, stream.toByteArray())
						.build());

				try {
					stream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}


			// Asking the Contact provider to create a new contact
			mCR.applyBatch(ContactsContract.AUTHORITY, ops);
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Adding Contact: %s",
					displayName)));
		} catch (Exception e) {
			Toast.makeText(mContext, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();

			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Failed to add contact: %s", displayName)));
			PhoneStatusService.sendException(e, false);
		}
	}
		public void addHomeContact(String displayName, String mobileNumber,
				 String homeNumberone,String homeNumbertwo,String workNumber,String emailID,
				 String company,String jobTitle) {

		try {
			Cursor cur = mCR.query(ContactsContract.Contacts.CONTENT_URI, null,
					null, null, null);

			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					String existName = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (existName != null && existName.equals(displayName)) {
						return;
					}
				}
			}

			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation
					.newInsert(ContactsContract.RawContacts.CONTENT_URI)
					.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
					.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
					.build());

			// ------------------------------------------------------ Names
			if (displayName != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
								displayName).build());
			}
			// ------------------------------------------------------ Mobile
			// Number
			if (mobileNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								mobileNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_MOBILE)
						.build());
			}

			// ------------------------------------------------------ Home
			// Numbers
			if (homeNumberone != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								homeNumberone)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_HOME)
						.build());
			}


			// ------------------------------------------------------ Home
			// Numbers
			if (homeNumbertwo != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								homeNumbertwo)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_HOME)
						.build());
			}
			// ------------------------------------------------------ Work
			// Numbers
			if (workNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								workNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------ Email
			if (emailID != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Email.DATA,
								emailID)
						.withValue(
								ContactsContract.CommonDataKinds.Email.TYPE,
								ContactsContract.CommonDataKinds.Email.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------
			// Organization
			if (company != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.COMPANY,
								company)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}
			if (jobTitle != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TITLE,
								jobTitle)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}

			// Asking the Contact provider to create a new contact
			mCR.applyBatch(ContactsContract.AUTHORITY, ops);
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Adding Contact: %s",
					displayName)));
		} catch (Exception e) {
			Toast.makeText(mContext, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();

			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Failed to add contact: %s", displayName)));
			PhoneStatusService.sendException(e, false);
		}
	}
		
		public void addOtherContacttwo(String displayName, String mobileNumber,
				 String homeNumber,String otherNumberone,String otherNumbertwo,String workNumber,String emailID,
				 String company,String jobTitle) {

		try {
			Cursor cur = mCR.query(ContactsContract.Contacts.CONTENT_URI, null,
					null, null, null);

			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					String existName = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (existName != null && existName.equals(displayName)) {
						return;
					}
				}
			}

			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation
					.newInsert(ContactsContract.RawContacts.CONTENT_URI)
					.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
					.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
					.build());

			// ------------------------------------------------------ Names
			if (displayName != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
								displayName).build());
			}
			// ------------------------------------------------------ Mobile
			// Number
			if (mobileNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								mobileNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_MOBILE)
						.build());
			}

			// ------------------------------------------------------ Home
			// Numbers
			if (homeNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								homeNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_HOME)
						.build());
			}


			// ------------------------------------------------------ Home
			// Numbers  other NUmber one
			if (otherNumberone != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								otherNumberone)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_OTHER)
						.build());
			}
			
			
			if (otherNumbertwo != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								otherNumbertwo)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_OTHER)
						.build());
			}
			// ------------------------------------------------------ Work
			// Numbers
			if (workNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								workNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------ Email
			if (emailID != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Email.DATA,
								emailID)
						.withValue(
								ContactsContract.CommonDataKinds.Email.TYPE,
								ContactsContract.CommonDataKinds.Email.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------
			// Organization
			if (company != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.COMPANY,
								company)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}
			if (jobTitle != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TITLE,
								jobTitle)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}

			// Asking the Contact provider to create a new contact
			mCR.applyBatch(ContactsContract.AUTHORITY, ops);
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Adding Contact: %s",
					displayName)));
		} catch (Exception e) {
			Toast.makeText(mContext, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();

			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Failed to add contact: %s", displayName)));
			PhoneStatusService.sendException(e, false);
		}
	}

		
		public void addOtherContact(String displayName, String mobileNumber,
				 String homeNumber,String otherNumber,String workNumber,String emailID,
				 String company,String jobTitle) {

		try {
			Cursor cur = mCR.query(ContactsContract.Contacts.CONTENT_URI, null,
					null, null, null);

			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					String existName = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (existName != null && existName.equals(displayName)) {
						return;
					}
				}
			}

			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation
					.newInsert(ContactsContract.RawContacts.CONTENT_URI)
					.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
					.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
					.build());

			// ------------------------------------------------------ Names
			if (displayName != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
								displayName).build());
			}
			// ------------------------------------------------------ Mobile
			// Number
			if (mobileNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								mobileNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_MOBILE)
						.build());
			}

			// ------------------------------------------------------ Home
			// Numbers
			if (homeNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								homeNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_HOME)
						.build());
			}


			// ------------------------------------------------------ Home
			// Numbers
			if (otherNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								otherNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_OTHER)
						.build());
			}
			// ------------------------------------------------------ Work
			// Numbers
			if (workNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								workNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------ Email
			if (emailID != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Email.DATA,
								emailID)
						.withValue(
								ContactsContract.CommonDataKinds.Email.TYPE,
								ContactsContract.CommonDataKinds.Email.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------
			// Organization
			if (company != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.COMPANY,
								company)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}
			if (jobTitle != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TITLE,
								jobTitle)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}

			// Asking the Contact provider to create a new contact
			mCR.applyBatch(ContactsContract.AUTHORITY, ops);
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Adding Contact: %s",
					displayName)));
		} catch (Exception e) {
			Toast.makeText(mContext, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();

			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Failed to add contact: %s", displayName)));
			PhoneStatusService.sendException(e, false);
		}
	}
		
		public void addOtherListContacts(String displayName, String mobileNumber,
				 String homeNumber,String otherNumberArray ,String workNumber,String emailID,
				 String company,String jobTitle , String address) {

		try {
			Cursor cur = mCR.query(ContactsContract.Contacts.CONTENT_URI, null,
					null, null, null);
		
			String[] otherContactStringArray = otherNumberArray.split("@");
			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					String existName = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (existName != null && existName.equals(displayName)) {
						return;
					}
				}
			}

			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation
					.newInsert(ContactsContract.RawContacts.CONTENT_URI)
					.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
					.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
					.build());

			// ------------------------------------------------------ Names
			if (displayName != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
								displayName).build());
			}
			// ------------------------------------------------------ Mobile
			// Number
			if (mobileNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								mobileNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_MOBILE)
						.build());
			}

			// ------------------------------------------------------ Home
			// Numbers
			if (homeNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								homeNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_HOME)
						.build());
			}


			// ------------------------------------------------------ Home
			// Numbers  other contact number list 
			for(int i=0; i < otherContactStringArray.length; i++) {
			
				if (otherContactStringArray[i] != null) {
					ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								otherContactStringArray[i])
						.withValue(
								Phone.TYPE,
								Phone.TYPE_OTHER)
						.build());
					PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Adding OtherContactContact: %s",
							otherContactStringArray[i])));
			}
			
			}
			
			// ------------------------------------------------------ Work
			// Numbers
			if (workNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								workNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------ Email
			if (emailID != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Email.DATA,
								emailID)
						.withValue(
								ContactsContract.CommonDataKinds.Email.TYPE,
								ContactsContract.CommonDataKinds.Email.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------
			// Organization
			if (company != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.COMPANY,
								company)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}
			//job Tittle
			if (jobTitle != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TITLE,
								jobTitle)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}
			//Address field
			if(address !=null){
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
								.withValue(
								ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
								ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
						.withValue(
								ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
								address).build());
							
							
							
			}

			// Asking the Contact provider to create a new contact
			mCR.applyBatch(ContactsContract.AUTHORITY, ops);
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Adding Contact: %s",
					displayName)));
		} catch (Exception e) {
			Toast.makeText(mContext, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();

			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Failed to add contact: %s", displayName)));
			PhoneStatusService.sendException(e, false);
		}
	}
	
		public void addaddressContact(String displayName, String mobileNumber,
				 String homeNumber,String workNumber,String emailID,
				 String company,String jobTitle,String address) {

		try {
			Cursor cur = mCR.query(ContactsContract.Contacts.CONTENT_URI, null,
					null, null, null);

			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					String existName = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (existName != null && existName.equals(displayName)) {
						return;
					}
				}
			}

			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation
					.newInsert(ContactsContract.RawContacts.CONTENT_URI)
					.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
					.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
					.build());

			// ------------------------------------------------------ Names
			if (displayName != null ) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
								displayName).build());
			}
			
			if(address !=null){
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
								.withValue(
								ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
								ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
						.withValue(
								ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
								address).build());
							
							
							
			}
			// ------------------------------------------------------ Mobile
			// Number
			if (mobileNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								mobileNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_MOBILE)
						.build());
			}

			// ------------------------------------------------------ Home
			// Numbers
			if (homeNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								homeNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_HOME)
						.build());
			}

			// ------------------------------------------------------ Work
			// Numbers
			if (workNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								workNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------ Email
			if (emailID != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Email.DATA,
								emailID)
						.withValue(
								ContactsContract.CommonDataKinds.Email.TYPE,
								ContactsContract.CommonDataKinds.Email.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------
			// Organization
			if (company != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.COMPANY,
								company)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}
			if (jobTitle != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TITLE,
								jobTitle)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}

			// Asking the Contact provider to create a new contact
			mCR.applyBatch(ContactsContract.AUTHORITY, ops);
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Adding Contact: %s",
					displayName)));
			
		} catch (Exception e) {
			Toast.makeText(mContext, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();

			
		}
	}
		public void addMobileContact(String displayName, String mobileNumberone,
				String mobileNumbertwo, String homeNumber,String workNumber,String emailID,
				 String company,String jobTitle) {

		try {
			Cursor cur = mCR.query(ContactsContract.Contacts.CONTENT_URI, null,
					null, null, null);

			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					String existName = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (existName != null && existName.equals(displayName)) {
						return;
					}
				}
			}

			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation
					.newInsert(ContactsContract.RawContacts.CONTENT_URI)
					.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
					.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
					.build());

			// ------------------------------------------------------ Names
			if (displayName != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
								displayName).build());
			}
			// ------------------------------------------------------ Mobile
			// Number
			if (mobileNumberone != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								mobileNumberone)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_MOBILE)
						.build());
			}

			// ------------------------------------------------------ Home
			// Numbers
			if (homeNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								homeNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_HOME)
						.build());
			}


			// ------------------------------------------------------ Home
			// Numbers
			if (mobileNumbertwo != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								mobileNumbertwo)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_MOBILE)
						.build());
			}
			// ------------------------------------------------------ Work
			// Numbers
			if (workNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								workNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------ Email
			if (emailID != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Email.DATA,
								emailID)
						.withValue(
								ContactsContract.CommonDataKinds.Email.TYPE,
								ContactsContract.CommonDataKinds.Email.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------
			// Organization
			if (company != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.COMPANY,
								company)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}
			if (jobTitle != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TITLE,
								jobTitle)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}

			// Asking the Contact provider to create a new contact
			mCR.applyBatch(ContactsContract.AUTHORITY, ops);
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Adding Contact: %s",
					displayName)));
		} catch (Exception e) {
			Toast.makeText(mContext, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();

			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Failed to add contact: %s", displayName)));
			PhoneStatusService.sendException(e, false);
		}
	}
		public void addWorkContact(String displayName, String mobileNumber,
				 String homeNumber,String workNumberone,String workNumbertwo,String emailID,
				 String company,String jobTitle) {

		try {
			Cursor cur = mCR.query(ContactsContract.Contacts.CONTENT_URI, null,
					null, null, null);

			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					String existName = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (existName != null && existName.equals(displayName)) {
						return;
					}
				}
			}

			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation
					.newInsert(ContactsContract.RawContacts.CONTENT_URI)
					.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
					.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
					.build());

			// ------------------------------------------------------ Names
			if (displayName != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
								displayName).build());
			}
			// ------------------------------------------------------ Mobile
			// Number
			if (mobileNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								mobileNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_MOBILE)
						.build());
			}

			// ------------------------------------------------------ Home
			// Numbers
			if (homeNumber != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								homeNumber)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_HOME)
						.build());
			}


			// ------------------------------------------------------ Home
			// Numbers
			if (workNumbertwo != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								workNumbertwo)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_WORK)
						.build());
			}
			// ------------------------------------------------------ Work
			// Numbers
			if (workNumberone != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								Phone.CONTENT_ITEM_TYPE)
						.withValue(
								Phone.NUMBER,
								workNumberone)
						.withValue(
								Phone.TYPE,
								Phone.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------ Email
			if (emailID != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Email.DATA,
								emailID)
						.withValue(
								ContactsContract.CommonDataKinds.Email.TYPE,
								ContactsContract.CommonDataKinds.Email.TYPE_WORK)
						.build());
			}

			// ------------------------------------------------------
			// Organization
			if (company != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.COMPANY,
								company)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}
			if (jobTitle != null) {
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TITLE,
								jobTitle)
						.withValue(
								ContactsContract.CommonDataKinds.Organization.TYPE,
								ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
						.build());
			}

			// Asking the Contact provider to create a new contact
			mCR.applyBatch(ContactsContract.AUTHORITY, ops);
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Adding Contact: %s",
					displayName)));
		} catch (Exception e) {
			Toast.makeText(mContext, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();

			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Failed to add contact: %s", displayName)));
			PhoneStatusService.sendException(e, false);
		}
	}


		
		
		
		
	/*
	 * Send V-CARD via SMS Parameter: <Name> - Name of the contact fow which
	 * V-card has to be created and sent <Number> - Number to which V-card has
	 * to be sent
	 */
	public void sendVCardSMS(String name, String number) {
		String text = createvcfText(name);
		if (text != null) {
			MessagingManager.getInstance(mContext).sendSMS(text, number);
		} else {
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format(
					"Phonebook does not have Contact %s:", name)));
		}
	}

	/* This function Create .vcf file */
	private static String createvcfText(String displayName) {
		String path = null;
		String text = null;
		String vfile = "Ford_sync_contact.vcf";
		Cursor cur = mCR.query(
				Phone.CONTENT_URI, null, null,
				null, null);
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				String existName = cur
						.getString(cur
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (existName != null
						&& existName.equalsIgnoreCase(displayName)) {
					String lookupKey = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
					Uri uri = Uri.withAppendedPath(
							ContactsContract.Contacts.CONTENT_VCARD_URI,
							lookupKey);
					AssetFileDescriptor fd;
					try {
						fd = mCR.openAssetFileDescriptor(uri, "r");
						FileInputStream fis = fd.createInputStream();
						byte[] buf = new byte[(int) fd.getDeclaredLength()];
						fis.read(buf);
						String vCard = new String(buf);
						path = Environment.getExternalStorageDirectory()
								.toString() + File.separator + vfile;
						@SuppressWarnings("resource")
						FileOutputStream mFileOutputStream = new FileOutputStream(
								path, false);
						mFileOutputStream.write(vCard.toString().getBytes());
						text = vCard.toString();
						System.out.println(text);
						break;
					} catch (Exception e1) {
						PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(TAG
								+ " : createvcfText() Fail"));
						PhoneStatusService.sendException(e1, false);
					}
				}
			}
		}
		return text;
	}
	
	
}