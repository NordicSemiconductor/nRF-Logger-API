/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.log.localprovider;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.log.LogContract;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/* package */class LocalLogDatabaseHelper extends SQLiteOpenHelper {

	/**
	 * The database that the provider uses as its underlying data store
	 */
	private static final String DATABASE_NAME = "local_log.db";

	/**
	 * The database version
	 */
	private static final int DATABASE_VERSION = 1;

	public interface Tables {
		/** Log sessions table. See {@link LogContract.Session} for column names */
		public static final String LOG_SESSIONS = "sessions";
		/** Log entries table. See {@link LogContract.Log} for column names */
		public static final String LOG = "log";
	}

	public interface Projections {
		public static final String[] ID = { BaseColumns._ID };
		public static final String[] MAX_NUMBER = { "MAX(" + LogContract.Session.NUMBER + ")" };
	}

	public interface SessionColumns {
		public static final String CONCRETE_ID = Tables.LOG_SESSIONS + "." + BaseColumns._ID;
		public static final String CONCRETE_KEY = Tables.LOG_SESSIONS + "." + LogContract.Session.KEY;
		public static final String CONCRETE_NAME = Tables.LOG_SESSIONS + "." + LogContract.Session.NAME;
		public static final String CONCRETE_APPLICATION_ID = Tables.LOG_SESSIONS + "." + LogContract.Session.APPLICATION_ID;
		public static final String CONCRETE_CREATED_AT = Tables.LOG_SESSIONS + "." + LogContract.Session.CREATED_AT;
		public static final String CONCRETE_NUMBER = Tables.LOG_SESSIONS + "." + LogContract.Session.NUMBER;
		public static final String CONCRETE_DESCRIPTION = Tables.LOG_SESSIONS + "." + LogContract.Session.DESCRIPTION;
		public static final String CONCRETE_MARK = Tables.LOG_SESSIONS + "." + LogContract.Session.MARK;
	}

	public interface LogColumns {
		public static final String CONCRETE_ID = Tables.LOG + "." + BaseColumns._ID;
		public static final String CONCRETE_SESSION_ID = Tables.LOG + "." + LogContract.Log.SESSION_ID;
		public static final String CONCRETE_TIME = Tables.LOG + "." + LogContract.Log.TIME;
		public static final String CONCRETE_LEVEL = Tables.LOG + "." + LogContract.Log.LEVEL;
		public static final String CONCRETE_DATA = Tables.LOG + "." + LogContract.Log.DATA;
	}

	private static LocalLogDatabaseHelper sInstance = null;

	public static synchronized LocalLogDatabaseHelper getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new LocalLogDatabaseHelper(context, DATABASE_NAME, DATABASE_VERSION);
		}
		return sInstance;
	}

	protected LocalLogDatabaseHelper(Context context, String databaseName, int version) {
		super(context, databaseName, null, version);
	}

	/**
	 * The SQL code that creates the log sessions table:
	 * 
	 * <pre>
	 * -------------------------------------------------------------------------------
	 * |                                   sessions                                  |            
	 * -------------------------------------------------------------------------------
	 * | _id (int, pk, auto increment) | key (text) | name (text) | created_at (int) |
	 * -------------------------------------------------------------------------------
	 * </pre>
	 */
	private static final String CREATE_LOG_SESSIONS = "CREATE TABLE " + Tables.LOG_SESSIONS + "(" + LogContract.Session._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + LogContract.Session.KEY
			+ " TEXT NOT NULL, " + LogContract.Session.NAME + " TEXT, " + LogContract.Session.CREATED_AT + " INTEGER NOT NULL);";

	/**
	 * The SQL code that creates the log table:
	 * 
	 * <pre>
	 * -------------------------------------------------------------------------------------------------
	 * |                                              log                                              |
	 * -------------------------------------------------------------------------------------------------
	 * | _id (int, pk, auto increment) | session_id (int, fk) | time (int) | level (int) | data (text) |
	 * -------------------------------------------------------------------------------------------------
	 * </pre>
	 */
	private static final String CREATE_LOG = "CREATE TABLE " + Tables.LOG + "(" + LogContract.Log._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + LogContract.Log.SESSION_ID + " INTEGER NOT NULL, "
			+ LogContract.Log.TIME + " INTEGER NOT NULL, " + LogContract.Log.LEVEL + " INTEGER NOT NULL, " + LogContract.Log.DATA + " TEXT NOT NULL, FOREIGN KEY(" + LogContract.Log.SESSION_ID
			+ ") REFERENCES " + Tables.LOG_SESSIONS + "(" + LogContract.Session._ID + "));";

	@Override
	public void onCreate(final SQLiteDatabase db) {
		final List<String> ddls = new ArrayList<String>();
		ddls.add(CREATE_LOG_SESSIONS);
		ddls.add(CREATE_LOG);

		for (String ddl : ddls) {
			db.execSQL(ddl);
		}

		// Set sequence starts.
		initializeAutoIncrementSequences(db);
	}

	protected void initializeAutoIncrementSequences(final SQLiteDatabase db) {
		// Default implementation does nothing.
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
		// empty for now
	}

}
