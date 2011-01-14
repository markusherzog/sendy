package com.markusherzog.sendy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 */
public class SettingsDbAdapter {

	public static final String KEY_ROWID = "_id";
	public static final String KEY_ACCOUNT = "account";
	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSWORD = "password";
	public static final String KEY_USEHTTPS = "usehttps";
	
    private static final String TAG = "SettingsDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {
    	private static final int DATABASE_VERSION = 3;
        DatabaseHelper(Context context) {
            super(context, "data", null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL("create table accounts (_id integer primary key autoincrement, "
                    + "account text not null, username text not null, password text not null, usehttps integer not null);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            if(oldVersion < 3) {
            	db.execSQL("alter table accounts add usehttps integer not null default 1;");
            }
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public SettingsDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public SettingsDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new account using the account name, user name and password provided. If the account is
     * successfully created return the new rowId for that account, otherwise return
     * a -1 to indicate failure.
     * 
     * @param account the name of the account
     * @param username the username for this account
     * @param password the password for this account
     * @return rowId or -1 if failed
     */
    public long createAccount(String account, String username, String password, boolean usehttps) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_ACCOUNT, account);
        initialValues.put(KEY_USERNAME, username);
        initialValues.put(KEY_PASSWORD, password);
        initialValues.put(KEY_USEHTTPS, password);
        return mDb.insert("accounts", null, initialValues);
    }

    /**
     * Delete the account with the given rowId
     * 
     * @param rowId id of account to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteAccount(long rowId) {

        return mDb.delete("accounts", KEY_ROWID + " = " + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all accounts in the database
     * 
     * @return Cursor over all accounts
     */
    public Cursor fetchAllAccounts() {

        return mDb.query("accounts", new String[] {KEY_ROWID, KEY_ACCOUNT, KEY_USERNAME, KEY_PASSWORD, KEY_USEHTTPS}, null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the account that matches the given rowId
     * 
     * @param rowId id of account to retrieve
     * @return Cursor positioned to matching account, if found
     * @throws SQLException if account could not be found/retrieved
     */
    public Cursor fetchAccount(long rowId) throws SQLException {

        Cursor mCursor = mDb.query(true, "accounts", new String[] {KEY_ROWID, KEY_ACCOUNT, KEY_USERNAME, KEY_PASSWORD, KEY_USEHTTPS}, KEY_ROWID + " = " + rowId,
        		null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the account using the details provided. The account to be updated is
     * specified using the rowId, and it is altered to use the username and password
     * values passed in. the name of the account can not be changed
     * 
     * @param rowId id of account to update
     * @param username the username for this account
     * @param password the password for this account
     * @return true if the account was successfully updated, false otherwise
     */
    public boolean updateAccount(long rowId, String username, String password, boolean usehttps) {
        ContentValues args = new ContentValues();
        args.put(KEY_USERNAME, username);
        args.put(KEY_PASSWORD, password);
        args.put(KEY_USEHTTPS, usehttps);

        return mDb.update("accounts", args, KEY_ROWID + " = " + rowId, null) > 0;
    }
}
