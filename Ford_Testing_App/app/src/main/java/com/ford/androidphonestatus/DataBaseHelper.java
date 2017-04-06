package com.ford.androidphonestatus;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;

public class DataBaseHelper extends SQLiteOpenHelper {

	//public static final String SMS_URI = �/data/data/org.secure.sms/databases/�;
	public static final String db_name = "DBBANKING.db";
	public static final int version =1;
	Context context;
	public DataBaseHelper(Context context) {
	super(context, db_name, null, version);
	// TODO Auto-generated constructor stub
	this.context =context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	// TODO Auto-generated method stub

	db.execSQL("create table datatable(address varchar(10), date varchar(10), body varchar(30))");
	Toast.makeText(context, "database created", Toast.LENGTH_LONG).show();
	Log.i("dbcreate", "DATABASE HAS CREATED");
	}

	public boolean checkDataBase(String db) {

	SQLiteDatabase checkDB = null;

	try {
	String myPath = "data/data/"+ context.getPackageName() +"/databases/" + db;
	checkDB = SQLiteDatabase.openDatabase(myPath, null,
	SQLiteDatabase.OPEN_READONLY);

	} catch (SQLiteException e) {

	// database does�t exist yet.

	} catch (Exception e) {

	}

	if (checkDB != null) {

	checkDB.close();

	}

	return checkDB != null ? true : false;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	// TODO Auto-generated method stub
	if (oldVersion >= newVersion)
	return;

	if (oldVersion == 1) {
	Log.d("New Version", "Datas can be upgraded");
	}

	Log.d("Sample Data", "onUpgrade : " + newVersion);
	}

	}