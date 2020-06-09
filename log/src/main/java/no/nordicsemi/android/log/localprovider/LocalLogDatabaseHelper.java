/*
 * Copyright (c) 2020, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this
 *    list of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may
 *    be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package no.nordicsemi.android.log.localprovider;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.log.LogContract;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

@SuppressWarnings("unused")
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
		String LOG_SESSIONS = "sessions";
		/** Log entries table. See {@link LogContract.Log} for column names */
		String LOG = "log";
	}

	public interface Projections {
		String[] ID = { BaseColumns._ID };
		String[] MAX_NUMBER = { "MAX(" + LogContract.Session.NUMBER + ")" };
	}

	public interface SessionColumns {
		String CONCRETE_ID = Tables.LOG_SESSIONS + "." + BaseColumns._ID;
		String CONCRETE_KEY = Tables.LOG_SESSIONS + "." + LogContract.Session.KEY;
		String CONCRETE_NAME = Tables.LOG_SESSIONS + "." + LogContract.Session.NAME;
		String CONCRETE_APPLICATION_ID = Tables.LOG_SESSIONS + "." + LogContract.Session.APPLICATION_ID;
		String CONCRETE_CREATED_AT = Tables.LOG_SESSIONS + "." + LogContract.Session.CREATED_AT;
		String CONCRETE_NUMBER = Tables.LOG_SESSIONS + "." + LogContract.Session.NUMBER;
		String CONCRETE_DESCRIPTION = Tables.LOG_SESSIONS + "." + LogContract.Session.DESCRIPTION;
		String CONCRETE_MARK = Tables.LOG_SESSIONS + "." + LogContract.Session.MARK;
	}

	public interface LogColumns {
		String CONCRETE_ID = Tables.LOG + "." + BaseColumns._ID;
		String CONCRETE_SESSION_ID = Tables.LOG + "." + LogContract.Log.SESSION_ID;
		String CONCRETE_TIME = Tables.LOG + "." + LogContract.Log.TIME;
		String CONCRETE_LEVEL = Tables.LOG + "." + LogContract.Log.LEVEL;
		String CONCRETE_DATA = Tables.LOG + "." + LogContract.Log.DATA;
	}

	private static LocalLogDatabaseHelper sInstance = null;

	static synchronized LocalLogDatabaseHelper getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new LocalLogDatabaseHelper(context, DATABASE_NAME, DATABASE_VERSION);
		}
		return sInstance;
	}

	private LocalLogDatabaseHelper(Context context, String databaseName, int version) {
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
	private static final String CREATE_LOG_SESSIONS = "CREATE TABLE " + Tables.LOG_SESSIONS +
			"(" +
				LogContract.Session._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				LogContract.Session.KEY + " TEXT NOT NULL, " +
				LogContract.Session.NAME + " TEXT, " +
				LogContract.Session.CREATED_AT + " INTEGER NOT NULL" +
			");";

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
	private static final String CREATE_LOG = "CREATE TABLE " + Tables.LOG +
			"(" +
				LogContract.Log._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				LogContract.Log.SESSION_ID + " INTEGER NOT NULL, " +
				LogContract.Log.TIME + " INTEGER NOT NULL, " +
				LogContract.Log.LEVEL + " INTEGER NOT NULL, " +
				LogContract.Log.DATA + " TEXT NOT NULL, FOREIGN KEY(" +
				LogContract.Log.SESSION_ID +
			") REFERENCES " + Tables.LOG_SESSIONS + "(" + LogContract.Session._ID + "));";

	@Override
	public void onCreate(final SQLiteDatabase db) {
		final List<String> ddls = new ArrayList<>();
		ddls.add(CREATE_LOG_SESSIONS);
		ddls.add(CREATE_LOG);

		for (String ddl : ddls) {
			db.execSQL(ddl);
		}

		// Set sequence starts.
		initializeAutoIncrementSequences(db);
	}

	private void initializeAutoIncrementSequences(final SQLiteDatabase db) {
		// Default implementation does nothing.
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
		// empty for now
	}

}
