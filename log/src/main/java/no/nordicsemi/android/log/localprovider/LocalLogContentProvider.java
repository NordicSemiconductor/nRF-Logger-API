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

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.localprovider.LocalLogDatabaseHelper.Tables;

/**
 * If the nRF Logger application is not installed an app may use the LocalLogContentProvider to
 * show log entries locally, in the application. This provider is a limited version of the one
 * in nRF Logger. It does not support different applications, marking log sessions with a star
 * of a flag nor giving them description. nRF Connect makes use of this provider if nRF Logger
 * is not installed. The log sessions are stored until user closes the connection to the device
 * and removed afterwards, but you may keep them and make your own log viewer in the application.
 * <p>
 * Remember to add the {@code <provider>} tag to the <i>AndroidManifest.xml</i> file giving the
 * same authority as you will return in the derived class.
 */
@SuppressWarnings("ConstantConditions")
public abstract class LocalLogContentProvider extends ContentProvider {
	private static final String TAG = "LocalLogContentProvider";
	private static final String DB_TAG = "local_log_db";

	/**
	 * Duration in ms to sleep after successfully yielding the lock during a batch operation.
	 */
	protected static final int SLEEP_AFTER_YIELD_DELAY = 4000;

	/**
	 * Maximum number of operations allowed in a batch between yield points.
	 */
	private static final int MAX_OPERATIONS_PER_YIELD_POINT = 500;

	/**
	 * Number of inserts performed in bulk to allow before yielding the transaction.
	 */
	private static final int BULK_INSERTS_PER_YIELD_POINT = 50;

	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

	private static final int SESSION = 1020;
	private static final int SESSION_ID = 1021;
	private static final int SESSION_ID_LOG = 1022;
	private static final int SESSION_ID_LOG_CONTENT = 1023;
	private static final int SESSION_KEY = 1024;

	/**
	 * Returns the provider authority. Use one based on your custom package name,
	 * e.g. 'com.example.log'.
	 *
	 * @return The provider authority.
	 */
	protected abstract Uri getAuthorityUri();

	private static final ProjectionMap sSessionColumns;
	private static final ProjectionMap sLogColumns;
	/**
	 * Contains just BaseColumns._COUNT.
	 */
	private static final ProjectionMap sCountProjectionMap;

	static {
		sSessionColumns = ProjectionMap.builder()
				.add(LogContract.Session._ID)
				.add(LogContract.Session.KEY)
				.add(LogContract.Session.NAME)
				.add(LogContract.Session.CREATED_AT)
				.build();

		sLogColumns = ProjectionMap.builder()
				.add(LogContract.Log._ID)
				.add(LogContract.Log.SESSION_ID)
				.add(LogContract.Log.LEVEL)
				.add(LogContract.Log.TIME)
				.add(LogContract.Log.DATA)
				.build();

		sCountProjectionMap = ProjectionMap.builder()
				.add(BaseColumns._COUNT, "COUNT(*)")
				.build();
	}

	private final ThreadLocal<LogTransaction> mTransactionHolder = new ThreadLocal<>();
	private final ThreadLocal<LocalLogDatabaseHelper> mLocalDatabaseHelper = new ThreadLocal<>();
	private LocalLogDatabaseHelper mDatabaseHelper;

	private final ContentValues mValues = new ContentValues();
	private final String[] mSelectionArgs1 = new String[1];

	/**
	 * The database helper to serialize all transactions on. If non-null, any new transaction
	 * created by this provider will automatically retrieve a writable database from this helper
	 * and initiate a transaction on that database.
	 * This should be used to ensure that operations across multiple databases are all blocked
	 * on a single DB lock (to prevent deadlock cases).
	 * Hint: It's always {@link LocalLogDatabaseHelper}.
	 */
	private SQLiteOpenHelper mSerializeOnDbHelper;

	/**
	 * The tag corresponding to the database used for serializing transactions.
	 * Hint: It's always the log db helper tag.
	 */
	private String mSerializeDbTag;

	protected LocalLogDatabaseHelper getDatabaseHelper(final Context context) {
		return LocalLogDatabaseHelper.getInstance(context);
	}

	/**
	 * Specifies a database helper (and corresponding tag) to serialize all transactions on.
	 */
	public void setDbHelperToSerializeOn(final SQLiteOpenHelper serializeOnDbHelper, final String tag) {
		mSerializeOnDbHelper = serializeOnDbHelper;
		mSerializeDbTag = tag;
	}

	@Override
	public boolean onCreate() {
		try {
			return initialize();
		} catch (RuntimeException e) {
			Log.e(TAG, "Cannot start provider", e);
			// In production code we don't want to throw here, so that phone will still work
			// in low storage situations.
			// See I5c88a3024ff1c5a06b5756b29a2d903f8f6a2531
			return false;
		}
	}

	private boolean initialize() {
		mDatabaseHelper = getDatabaseHelper(getContext());
		mLocalDatabaseHelper.set(mDatabaseHelper);

		// Set up the DB helper for keeping transactions serialized.
		setDbHelperToSerializeOn(mDatabaseHelper, DB_TAG);

		// Create the URI matcher based on user's authority
		final String authority = getAuthorityUri().getAuthority();
		final UriMatcher matcher = sUriMatcher;
		// Returns all sessions
		matcher.addURI(authority, "session", SESSION);

		// Returns session with given id
		matcher.addURI(authority, "session/#", SESSION_ID);

		// Returns log entries from session with given id
		matcher.addURI(authority, "session/#/log", SESSION_ID_LOG);

		// Returns log entries from session with given id as a single String
		matcher.addURI(authority, "session/#/log/content", SESSION_ID_LOG_CONTENT);

		// Returns all sessions with given key
		matcher.addURI(authority, "session/key/*", SESSION_KEY);

		return true;
	}

	@Override
	public String getType(@NonNull final Uri uri) {
		final int match = sUriMatcher.match(uri);
		switch (match) {
			case SESSION_ID:
				return LogContract.Session.CONTENT_ITEM_TYPE;
			case SESSION_ID_LOG:
				return LogContract.Log.CONTENT_TYPE;
		}
		return null;
	}

	@Override
	public Cursor query(@NonNull final Uri uri, final String[] projection, String selection,
						String[] selectionArgs, final String sortOrder) {
		mLocalDatabaseHelper.set(mDatabaseHelper);
		final SQLiteDatabase db = mLocalDatabaseHelper.get().getReadableDatabase();

		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		final int match = sUriMatcher.match(uri);
		switch (match) {
			case SESSION_ID: {
				final String id = uri.getLastPathSegment();
				qb.setTables(Tables.LOG_SESSIONS);
				qb.setProjectionMap(sSessionColumns);
				qb.appendWhere(LogContract.Session._ID + "=?");
				selectionArgs = appendSelectionArgs(selectionArgs, id);
				break;
			}
			case SESSION_ID_LOG:
			case SESSION_ID_LOG_CONTENT: {
				final String id = uri.getPathSegments().get(1);
				qb.setTables(Tables.LOG);
				qb.setProjectionMap(sLogColumns);
				qb.appendWhere(LogContract.Log.SESSION_ID + "=?");
				selectionArgs = appendSelectionArgs(selectionArgs, id);
				if (match != SESSION_ID_LOG_CONTENT)
					break;

				// prepare content builder
				final StringBuilder builder = new StringBuilder();

				// get session data
				final String[] sessionProjection = new String[]{LogContract.Session.KEY, LogContract.Session.NAME, LogContract.Session.CREATED_AT};
				final String sessionSelection = LogContract.Session._ID + "=?";
				final String[] sessionSelArgs = mSelectionArgs1;
				sessionSelArgs[0] = id;
				Cursor c = db.query(Tables.LOG_SESSIONS, sessionProjection, sessionSelection, sessionSelArgs, null, null, null);
				try {
					if (c.moveToNext()) {
						final Calendar calendar = Calendar.getInstance();
						calendar.setTimeInMillis(c.getLong(2 /* CREATED_AT */));
						final String appName = getContext().getApplicationInfo().loadLabel(getContext().getPackageManager()).toString();
						builder.append(String.format("%s, %tF\n", appName, calendar));
						final String name = c.getString(1 /* NAME */);
						builder.append(String.format("%s (%s)\n", name != null ? name : "No name", c.getString(0 /* KEY */)));
					}
				} finally {
					c.close();
				}

				// get log entries
				final String[] entryProjection = new String[]{LogContract.Log.TIME, LogContract.Log.LEVEL, LogContract.Log.DATA};
				c = query(uri, db, qb, entryProjection, selection, selectionArgs, LogContract.Log.TIME + " ASC");
				try {
					final Calendar calendar = Calendar.getInstance();
					while (c.moveToNext()) {
						builder.append(getLevelAsChar(c.getInt(1 /* LEVEL */)));
						calendar.setTimeInMillis(c.getLong(0 /* TIME */));
						builder.append(String.format("\t%1$tR:%1$tS.%1$tL\t%2$s\n", calendar, c.getString(2 /* DATA */)));
					}
				} finally {
					c.close();
				}

				final MatrixCursor cursor = new MatrixCursor(new String[]{LogContract.Session.Content.CONTENT});
				cursor.addRow(new String[]{builder.toString()});
				return cursor;
			}
		}
		return query(uri, db, qb, projection, selection, selectionArgs, sortOrder);
	}

	private char getLevelAsChar(final int level) {
		switch (level) {
			case LogContract.Log.Level.VERBOSE:
				return 'V';
			case LogContract.Log.Level.INFO:
				return 'I';
			case LogContract.Log.Level.APPLICATION:
				return 'A';
			case LogContract.Log.Level.WARNING:
				return 'W';
			case LogContract.Log.Level.ERROR:
				return 'E';
			default:
				return 'D';
		}
	}

	@Override
	public int bulkInsert(@NonNull final Uri uri, @NonNull final ContentValues[] values) {
		final LogTransaction transaction = startTransaction(true);
		int numValues = values.length;
		int opCount = 0;
		boolean dirty = false;
		try {
			for (ContentValues value : values) {
				if (value == null)
					continue;
				if (insertInTransaction(uri, value) != null)
					dirty = true;
				if (++opCount >= BULK_INSERTS_PER_YIELD_POINT) {
					opCount = 0;
					try {
						yield(transaction);
					} catch (RuntimeException re) {
						transaction.markYieldFailed();
						throw re;
					}
				}
			}
			if (dirty) {
				transaction.markDirty();
			}
			transaction.markSuccessful(true);
		} finally {
			endTransaction(uri, true);
		}
		return numValues;
	}

	@Override
	@NonNull
	public ContentProviderResult[] applyBatch(@NonNull final ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {
		int ypCount = 0;
		int opCount = 0;
		final LogTransaction transaction = startTransaction(true);
		try {
			final int numOperations = operations.size();
			final ContentProviderResult[] results = new ContentProviderResult[numOperations];
			for (int i = 0; i < numOperations; i++) {
				if (++opCount >= MAX_OPERATIONS_PER_YIELD_POINT) {
					throw new OperationApplicationException(
							"Too many content provider operations between yield points. " +
							"The maximum number of operations per yield point is " +
							MAX_OPERATIONS_PER_YIELD_POINT, ypCount);
				}
				final ContentProviderOperation operation = operations.get(i);
				if (i > 0 && operation.isYieldAllowed()) {
					opCount = 0;
					try {
						if (yield(transaction)) {
							ypCount++;
						}
					} catch (RuntimeException re) {
						transaction.markYieldFailed();
						throw re;
					}
				}
				results[i] = operation.apply(this, results, i);
			}
			transaction.markSuccessful(true);
			return results;
		} finally {
			endTransaction(LogContract.Session.CONTENT_URI, true);
		}
	}

	@Override
	public Uri insert(@NonNull final Uri uri, final ContentValues values) {
		final LogTransaction transaction = startTransaction(false);
		try {
			final Uri result = insertInTransaction(uri, values);
			if (result != null) {
				transaction.markDirty();
			}
			transaction.markSuccessful(false);
			return result;
		} finally {
			endTransaction(uri, false);
		}
	}

	@Override
	public int delete(@NonNull final Uri uri, final String selection, final String[] selectionArgs) {
		LogTransaction transaction = startTransaction(false);
		try {
			int deleted = deleteInTransaction(uri, selection, selectionArgs);
			if (deleted > 0) {
				transaction.markDirty();
			}
			transaction.markSuccessful(false);
			return deleted;
		} finally {
			endTransaction(uri, false);
		}
	}

	@Override
	public int update(@NonNull final Uri uri, final ContentValues values, final String selection,
					  final String[] selectionArgs) {
		LogTransaction transaction = startTransaction(false);
		try {
			int updated = updateInTransaction(uri, values, selection, selectionArgs);
			if (updated > 0) {
				transaction.markDirty();
			}
			transaction.markSuccessful(false);
			return updated;
		} finally {
			endTransaction(uri, false);
		}
	}

	private Cursor query(final Uri uri, final SQLiteDatabase db, final SQLiteQueryBuilder qb,
						 final String[] projection, final String selection,
						 final String[] selectionArgs, final String sortOrder) {
		if (projection != null && projection.length == 1 && BaseColumns._COUNT.equals(projection[0])) {
			qb.setProjectionMap(sCountProjectionMap);
		}
		final Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		if (c != null) {
			c.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return c;
	}

	protected Uri insertInTransaction(@NonNull final Uri uri, final ContentValues values) {
		mLocalDatabaseHelper.set(mDatabaseHelper);

		final int match = sUriMatcher.match(uri);
		long id = 0;

		switch (match) {
			case SESSION_ID_LOG: {
				final long sessionId = Long.parseLong(uri.getPathSegments().get(1));

				// This allows the time to be set using the bulk insert
				if (!values.containsKey(LogContract.Log.TIME))
					values.put(LogContract.Log.TIME, System.currentTimeMillis());
				values.put(LogContract.Log.SESSION_ID, sessionId);
				id = insertLog(uri, values);
				break;
			}
			case SESSION: {
				final String key = values.getAsString(LogContract.Session.KEY);
				if (key == null)
					return null;

				final long time = System.currentTimeMillis();
				values.put(LogContract.Session.CREATED_AT, time);
				id = insertSession(uri, values);
				if (id < 0)
					return null;
			}
			case SESSION_KEY: {
				final String key = uri.getLastPathSegment();
				final long time = System.currentTimeMillis();

				values.put(LogContract.Session.KEY, key);
				values.put(LogContract.Session.CREATED_AT, time);
				id = insertSession(uri, values);
				if (id < 0)
					return null;
				return ContentUris.withAppendedId(
						Uri.withAppendedPath(getAuthorityUri(), LogContract.Session.SESSION_CONTENT_DIRECTORY),
						id);
			}
		}

		if (id < 0)
			return null;
		return ContentUris.withAppendedId(uri, id);
	}

	@SuppressWarnings("unused")
	private long insertSession(final Uri uri, final ContentValues values) {
		mValues.clear();
		mValues.putAll(values);

		final SQLiteDatabase db = mLocalDatabaseHelper.get().getWritableDatabase();
		return db.insert(Tables.LOG_SESSIONS, null, mValues);
	}

	@SuppressWarnings("unused")
	private long insertLog(final Uri uri, final ContentValues values) {
		mValues.clear();
		mValues.putAll(values);

		final SQLiteDatabase db = mLocalDatabaseHelper.get().getWritableDatabase();
		return db.insert(Tables.LOG, null, mValues);
	}

	@SuppressWarnings("unused")
	private int deleteInTransaction(final Uri uri, final String selection,
									final String[] selectionArgs) {
		mLocalDatabaseHelper.set(mDatabaseHelper);
		final int match = sUriMatcher.match(uri);

		switch (match) {
			case SESSION: {
				return deleteSessions();
			}
			case SESSION_ID: {
				final long id = ContentUris.parseId(uri);
				return deleteSession(id);
			}
		}
		return 0;
	}

	private int deleteSessions() {
		final SQLiteDatabase db = mLocalDatabaseHelper.get().getWritableDatabase();

		db.delete(Tables.LOG, null, null);
		return db.delete(Tables.LOG_SESSIONS, null, null);
	}

	private int deleteSession(final long sessionId) {
		final SQLiteDatabase db = mLocalDatabaseHelper.get().getWritableDatabase();
		final String[] args = mSelectionArgs1;
		args[0] = String.valueOf(sessionId);

		db.delete(Tables.LOG, LogContract.Log.SESSION_ID + "=?", args);
		return db.delete(Tables.LOG_SESSIONS, LogContract.Session._ID + "=?", args);
	}

	@SuppressWarnings("unused")
	private int updateInTransaction(final Uri uri, final ContentValues values,
									final String selection, final String[] selectionArgs) {
		throw new UnsupportedOperationException("Updating log is not supported. You can not change the history.");
	}

	/**
	 * If we are not yet already in a transaction, this starts one (on the DB to serialize on,
	 * if present) and sets the thread-local transaction variable for tracking.
	 * If we are already in a transaction, this returns that transaction, and the batch parameter
	 * is ignored.
	 *
	 * @param callerIsBatch Whether the caller is operating in batch mode.
	 */
	private LogTransaction startTransaction(final boolean callerIsBatch) {
		LogTransaction transaction = mTransactionHolder.get();
		if (transaction == null) {
			transaction = new LogTransaction(callerIsBatch);
			if (mSerializeOnDbHelper != null) {
				transaction.startTransactionForDb(mSerializeOnDbHelper.getWritableDatabase(), mSerializeDbTag);
			}
			mTransactionHolder.set(transaction);
		}
		return transaction;
	}

	/**
	 * Ends the current transaction and clears out the member variable. This does not set the
	 * transaction as being successful.
	 *
	 * @param uri           uri to be notified about the change.
	 * @param callerIsBatch Whether the caller is operating in batch mode.
	 */
	@SuppressWarnings("unused")
	private void endTransaction(final Uri uri, final boolean callerIsBatch) {
		final LogTransaction transaction = mTransactionHolder.get();
		if (transaction != null && (!transaction.isBatch() || callerIsBatch)) {
			try {
				if (transaction.isDirty()) {
					notifyChange(Uri.withAppendedPath(getAuthorityUri(),
							LogContract.Session.SESSION_CONTENT_DIRECTORY));
				}
				transaction.finish(callerIsBatch);
			} finally {
				// No matter what, make sure we clear out the thread-local transaction reference.
				mTransactionHolder.set(null);
			}
		}
	}

	protected boolean yield(LogTransaction transaction) {
		// Now proceed with the DB yield.
		SQLiteDatabase db = transaction.getDbForTag(DB_TAG);
		return db != null && db.yieldIfContendedSafely(SLEEP_AFTER_YIELD_DELAY);
	}

	protected void notifyChange(final Uri uri) {
		getContext().getContentResolver().notifyChange(uri, null, false);
	}

	/**
	 * Inserts an argument at the beginning of the selection arg list.
	 */
	private String[] appendSelectionArgs(final String[] selectionArgs, final String... arg) {
		if (selectionArgs == null) {
			return arg;
		} else {
			final int newLength = selectionArgs.length + arg.length;
			final String[] newSelectionArgs = new String[newLength];
			System.arraycopy(arg, 0, newSelectionArgs, 0, arg.length);
			System.arraycopy(selectionArgs, 0, newSelectionArgs, arg.length, selectionArgs.length);
			return newSelectionArgs;
		}
	}
}
