/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.log;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;

/**
 * <p>
 * Logger helper class. Makes the logger operations easy. Application needs to have <b>no.nordicsemi.android.LOG</b> permission to use this logger.
 * </p>
 * <p>
 * To use a logger you must first create a logger session. Log entries can be then appended to the session. Session has 2 parameters: a non-changeable key, which can be f.e. a device address and a
 * name, which can be a human readable device name. nRF Logger application use the key to group devices together. The logger session contains also the application name + optionally a profile name. To
 * create the session invoke {@link #newSession(Context, String, String, String)} method.
 * </p>
 * <p>
 * Log entries may not be deleted from the database except by removing the whole session.
 * </p>
 * <h1>Usage:</h1>
 * <p>
 * Adding log entry:
 * 
 * <pre>
 * LogSession session = Logger.newSession(this, "DFU", deviceAddress, deviceName);
 * Logger.i(session, &quot;Log session has been created&quot;);
 * ...
 * String error = e.getMessage();
 * Logger.e(session, R.string.error_message, error);
 * </pre>
 * 
 * where in <b>strings.xml</b> there is:
 * 
 * <pre>
 * &lt;string name="error_message"&gt;An error occurred: %s&lt;/string&gt;
 * </pre>
 *
 * <p>
 * Getting session log entries:
 * 
 * <pre>
 * final Cursor c = getContentResolver().query(session.getSessionEntriesUri(), new String[] {
 * 		{@link LogContract.Log#TIME}, 
 * 		{@link LogContract.Log#LEVEL}, 
 * 		{@link LogContract.Log#DATA} }, null, null, {@link LogContract.Log#TIME} + " ASC");
 * try {
 * 	while (c.moveToNext()) {
 * 		final String data = c.getString(2 &#47;* DATA *&#47;);
 * 		...
 * 	}
 * } finally {
 * 	c.close();
 * }
 * </pre>
 * 
 * You may use the following Uris to perform operations on the data set:
 * <ul>
 * <li><b>no.nordicsemi.android.log/application</b> - returns all applications that has created at least one log session</li>
 * <li><b>no.nordicsemi.android.log/application/[APP_ID]</b> - returns all sessions from given application</li>
 * <li><b>no.nordicsemi.android.log/session</b> - returns all sessions</li>
 * <li><b>no.nordicsemi.android.log/session/[SESSION_ID]</b> - returns the session with given id</li>
 * <li><b>no.nordicsemi.android.log/session/[SESSION_ID]/log</b> - returns the log entries from the session with given id</li>
 * <li><b>no.nordicsemi.android.log/session/key/[KEY]</b> - returns all sessions with given key</li>
 * <li><b>no.nordicsemi.android.log/session/key/[KEY]/[NUMBER]</b> - returns the session with given key and number. Numbers starts from 1 for every key.</li>
 * <li><b>no.nordicsemi.android.log/session/key/[KEY]/[NUMBER]/log</b> - returns the log entries from the session with given key and number</li>
 * </ul>
 * Please use {@link LogContract} member classes to build Uris.
 * <p>
 * For every log session created on a new day for a single application (and optionally profile) a special "date" session is created. It's number is equal to 0 and key to "!date". To obtain a list of
 * non-date sessions for a given application id sorted by date, key and time use:
 * 
 * <pre>
 * &#064;Override
 * public Loader&lt;Cursor&gt; onCreateLoader(int id, Bundle args) {
 * 	long appId = args.getLong(APPLICATION_ID);
 * 
 * 	Uri sessionsUri = LogContract.Session.createSessionsUri(appId);
 * 	String selection = LogContract.Session.NUMBER + &quot;&gt;0&quot;;
 * 	return new CursorLoader(this, sessionsUri, new String[] { {@link LogContract.Session#NAME}, {@link LogContract.Session#CREATED_AT} }, selection, null, &quot;date((&quot; + {@link LogContract.Session#CREATED_AT} + &quot; / 1000), 'unixepoch') DESC, &quot; + {@link LogContract.Session#KEY}
 * 			+ &quot; ASC, &quot; + {@link LogContract.Session#CREATED_AT} + &quot; DESC&quot;);
 * }
 * </pre>
 */
public class Logger {
	public static final int MARK_CLEAR = 0;
	public static final int MARK_STAR_YELLOW = 1;
	public static final int MARK_STAR_BLUE = 2;
	public static final int MARK_STAR_RED = 3;
	public static final int MARK_FLAG_YELLOW = 4;
	public static final int MARK_FLAG_BLUE = 5;
	public static final int MARK_FLAG_RED = 6;

	private static final int SESSION_ID = 100;
	private static final int SESSION_ID_LOG = 101;
	private static final int SESSION_KEY_NUMBER = 102;
	private static final int SESSION_KEY_NUMBER_LOG = 103;

	private static final UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static final ContentValues values = new ContentValues();

	static {
		final UriMatcher matcher = mUriMatcher;
		matcher.addURI(LogContract.AUTHORITY, "session/#", SESSION_ID);
		matcher.addURI(LogContract.AUTHORITY, "session/#/log", SESSION_ID_LOG);
		matcher.addURI(LogContract.AUTHORITY, "session/key/*/#", SESSION_KEY_NUMBER);
		matcher.addURI(LogContract.AUTHORITY, "session/key/*/#/log", SESSION_KEY_NUMBER_LOG);
	}

	/**
	 * Creates new logger session. Must be created before appending log entries. If the nRF Logger application is not installed the method will return <code>null</code>.
	 * 
	 * @param context
	 *            the context (activity, service or application)
	 * @param key
	 *            the session key, which is used to group sessions
	 * @param name
	 *            the human readable session name
	 * @return the {@link LogSession} that can be used to append log entries or <code>null</code> if MCP is not installed. The <code>null</code> value can be next passed to logging methods
	 */
	public static LogSession newSession(final Context context, final String key, final String name) {
		return newSession(context, null, key, name);
	}

	/**
	 * Creates new logger session. Must be created before appending log entries. If the nRF Logger application is not installed the method will return <code>null</code>.
	 * 
	 * @param context
	 *            the context (activity, service or application)
	 * @param profile
	 *            application profile which will be concatenated to the application name
	 * @param key
	 *            the session key, which is used to group sessions
	 * @param name
	 *            the human readable session name
	 * @return the {@link LogSession} that can be used to append log entries or <code>null</code> if MCP is not installed. The <code>null</code> value can be next passed to logging methods
	 */
	public static LogSession newSession(final Context context, final String profile, final String key, final String name) {
		final ArrayList<ContentProviderOperation> ops = new ArrayList<>();
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(LogContract.Application.CONTENT_URI);
		final String appName = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
		if (profile != null)
			builder.withValue(LogContract.Application.APPLICATION, appName + " " + profile);
		else
			builder.withValue(LogContract.Application.APPLICATION, appName);
		ops.add(builder.build());

		final Uri uri = LogContract.Session.CONTENT_URI.buildUpon().appendEncodedPath(LogContract.Session.KEY_CONTENT_DIRECTORY).appendEncodedPath(key).build();
		builder = ContentProviderOperation.newInsert(uri);
		builder.withValueBackReference(LogContract.Session.APPLICATION_ID, 0);
		builder.withValue(LogContract.Session.NAME, name);
		ops.add(builder.build());

		try {
			final ContentProviderResult[] results = context.getContentResolver().applyBatch(LogContract.AUTHORITY, ops);
			final Uri sessionUri = results[1].uri;
			return new LogSession(context, sessionUri);
		} catch (final Exception e) {
			// the nRF Logger application is not installed, do nothing
			return null;
		}
	}

	/**
	 * Returns the log session object. The given Uri must points session log entries: .../session/#, .../session/[KEY]/[NUMBER], or both with ./log appended.
	 * For Uris not matching these patterns a {@link LocalLogSession} object will be created.
	 * 
	 * @param context
	 *            the application context
	 * @param uri
	 *            the session uri
	 * @return the log session
	 */
	public static ILogSession openSession(final Context context, final Uri uri) {
		if (uri == null)
			return null;

		final int match = mUriMatcher.match(uri);
		switch (match) {
		case SESSION_ID:
		case SESSION_KEY_NUMBER:
			return new LogSession(context, uri);
		case SESSION_ID_LOG:
		case SESSION_KEY_NUMBER_LOG:
			// we have to cut the last part from the Uri
			final Uri.Builder builder = LogContract.Session.CONTENT_URI.buildUpon();
			final List<String> segments = uri.getPathSegments();
			for (int i = 1; i < segments.size() - 1; ++i) {
				builder.appendEncodedPath(segments.get(i));
			}
			return new LogSession(context, builder.build());
		default:
			return new LocalLogSession(context, uri);
		}
	}

	/**
	 * Sets the session description. Passing <code>null</code> will clear the description.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param description
	 *            the new description or <code>null</code>.
	 */
	public static void setSessionDescription(final LogSession session, final String description) {
		if (session == null)
			return;

		synchronized (values) {
			values.clear();
			values.put(LogContract.Session.DESCRIPTION, description);
			try {
				session.getContext().getContentResolver().update(session.getSessionUri(), values, null, null);
			} catch (final Exception e) {
				// the nRF Logger application is not installed, do nothing
			}
		}
	}

	/**
	 * Sets the session mark (star or flag). The default value is 0. Session marks supported by nRF Logger application: {@link #MARK_CLEAR}, {@link #MARK_STAR_YELLOW}, {@link #MARK_STAR_BLUE},
	 * {@link #MARK_STAR_RED}, {@link #MARK_FLAG_YELLOW}, {@link #MARK_FLAG_BLUE}, {@link #MARK_FLAG_RED}.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param mark
	 *            the new mark. {@link #MARK_CLEAR} will clear the mark.
	 */
	public static void setSessionMark(final LogSession session, final int mark) {
		if (session == null)
			return;

		synchronized (values) {
			values.clear();
			values.put(LogContract.Session.MARK, mark);
			try {
				session.getContext().getContentResolver().update(session.getSessionUri(), values, null, null);
			} catch (final Exception e) {
				// the nRF Logger application is not installed, do nothing
			}
		}
	}

	/**
	 * Logs the message in DEBUG (lowest) level.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param message
	 *            the message to be logged
	 */
	public static void d(final ILogSession session, final String message) {
		log(session, LogContract.Log.Level.DEBUG, message);
	}

	/**
	 * Logs the message in VERBOSE level.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param message
	 *            the message to be logged
	 */
	public static void v(final ILogSession session, final String message) {
		log(session, LogContract.Log.Level.VERBOSE, message);
	}

	/**
	 * Logs the message in INFO level.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param message
	 *            the message to be logged
	 */

	public static void i(final ILogSession session, final String message) {
		log(session, LogContract.Log.Level.INFO, message);
	}

	/**
	 * Logs the message in APPLICATION level.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param message
	 *            the message to be logged
	 */

	public static void a(final ILogSession session, final String message) {
		log(session, LogContract.Log.Level.APPLICATION, message);
	}

	/**
	 * Logs the message in WARNING level.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param message
	 *            the message to be logged
	 */

	public static void w(final ILogSession session, final String message) {
		log(session, LogContract.Log.Level.WARNING, message);
	}

	/**
	 * Logs the message in ERROR (highest) level.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param message
	 *            the message to be logged
	 */

	public static void e(final ILogSession session, final String message) {
		log(session, LogContract.Log.Level.ERROR, message);
	}

	/**
	 * Adds the log entry to nRF Logger log. If the parameter session is <code>null</code> it will exit immediately.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)}
	 * @param level
	 *            the log level, one of {@link LogContract.Log.Level#DEBUG}, {@link LogContract.Log.Level#VERBOSE}, {@link LogContract.Log.Level#INFO}, {@link LogContract.Log.Level#WARNING},
	 *            {@link LogContract.Log.Level#ERROR}
	 * @param message
	 *            the message to be logged
	 */
	public static void log(final ILogSession session, final int level, final String message) {
		if (session == null)
			return;

		synchronized (values) {
			values.clear();
			values.put(LogContract.Log.LEVEL, level);
			values.put(LogContract.Log.DATA, message);
			try {
				session.getContext().getContentResolver().insert(session.getSessionEntriesUri(), values);
			} catch (final Exception e) {
				// nRF Logger application is not installed, do nothing
			}
		}
	}

	/**
	 * Returns the log entry. If the parameter session is <code>null</code> it will exit immediately.
	 *
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)}
	 * @param level
	 *            the log level, one of {@link LogContract.Log.Level#DEBUG}, {@link LogContract.Log.Level#VERBOSE}, {@link LogContract.Log.Level#INFO}, {@link LogContract.Log.Level#WARNING},
	 *            {@link LogContract.Log.Level#ERROR}
	 * @param message
	 *            the message to be logged
	 */
	public static ContentValues logEntry(final ILogSession session, final int level, final String message) {
		if (session == null)
			return null;

		final ContentValues values = new ContentValues();
		values.put(LogContract.Log.TIME, System.currentTimeMillis()); // This will be overwritten in nRF Logger 1.0-1.5 by bulk time
		values.put(LogContract.Log.LEVEL, level);
		values.put(LogContract.Log.DATA, message);
		return values;
	}

	/**
	 * Logs the message in DEBUG (lowest) level.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param messageResId
	 *            the log message resource id
	 * @param params
	 *            additional (optional) parameters used to fill the message
	 */
	public static void d(final ILogSession session, final int messageResId, final Object... params) {
		log(session, LogContract.Log.Level.DEBUG, messageResId, params);
	}

	/**
	 * Logs the message in VERBOSE level.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param messageResId
	 *            the log message resource id
	 * @param params
	 *            additional (optional) parameters used to fill the message
	 */
	public static void v(final ILogSession session, final int messageResId, final Object... params) {
		log(session, LogContract.Log.Level.VERBOSE, messageResId, params);
	}

	/**
	 * Logs the message in INFO level.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param messageResId
	 *            the log message resource id
	 * @param params
	 *            additional (optional) parameters used to fill the message
	 */
	public static void i(final ILogSession session, final int messageResId, final Object... params) {
		log(session, LogContract.Log.Level.INFO, messageResId, params);
	}

	/**
	 * Logs the message in APPLICATION level.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param messageResId
	 *            the log message resource id
	 * @param params
	 *            additional (optional) parameters used to fill the message
	 */
	public static void a(final ILogSession session, final int messageResId, final Object... params) {
		log(session, LogContract.Log.Level.APPLICATION, messageResId, params);
	}

	/**
	 * Logs the message in WARNING level.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param messageResId
	 *            the log message resource id
	 * @param params
	 *            additional (optional) parameters used to fill the message
	 */
	public static void w(final ILogSession session, final int messageResId, final Object... params) {
		log(session, LogContract.Log.Level.WARNING, messageResId, params);
	}

	/**
	 * Logs the message in ERROR (highest) level.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)} method. This may be <code>null</code>, than it does nothing.
	 * @param messageResId
	 *            the log message resource id
	 * @param params
	 *            additional (optional) parameters used to fill the message
	 */
	public static void e(final ILogSession session, final int messageResId, final Object... params) {
		log(session, LogContract.Log.Level.ERROR, messageResId, params);
	}

	/**
	 * Adds the log entry to nRF Logger log. If the parameter session is <code>null</code> it will exit immediately.
	 * 
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)}
	 * @param level
	 *            the log level, one of {@link LogContract.Log.Level#DEBUG}, {@link LogContract.Log.Level#VERBOSE}, {@link LogContract.Log.Level#INFO}, {@link LogContract.Log.Level#WARNING},
	 *            {@link LogContract.Log.Level#ERROR}
	 * @param messageResId
	 *            the log message resource id
	 * @param params
	 *            additional (optional) parameters used to fill the message
	 */
	public static void log(final ILogSession session, final int level, final int messageResId, final Object... params) {
		if (session == null)
			return;

		values.clear();
		values.put(LogContract.Log.LEVEL, level);
		values.put(LogContract.Log.DATA, session.getContext().getString(messageResId, params));
		try {
			session.getContext().getContentResolver().insert(session.getSessionEntriesUri(), values);
		} catch (final Exception e) {
			// the nRF Logger application is not installed, do nothing
		}
	}

	/**
	 * Returns the log entry. If the parameter session is <code>null</code> it will exit immediately.
	 *
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)}
	 * @param level
	 *            the log level, one of {@link LogContract.Log.Level#DEBUG}, {@link LogContract.Log.Level#VERBOSE}, {@link LogContract.Log.Level#INFO}, {@link LogContract.Log.Level#WARNING},
	 *            {@link LogContract.Log.Level#ERROR}
	 * @param messageResId
	 *            the log message resource id
	 * @param params
	 *            additional (optional) parameters used to fill the message
	 */
	public static ContentValues logEntry(final ILogSession session, final int level, final int messageResId, final Object... params) {
		if (session == null)
			return null;

		final ContentValues values = new ContentValues();
		values.put(LogContract.Log.TIME, System.currentTimeMillis()); // This will be overwritten in nRF Logger 1.0-1.5 by bulk time
		values.put(LogContract.Log.LEVEL, level);
		values.put(LogContract.Log.DATA, session.getContext().getString(messageResId, params));
		return values;
	}

	/**
	 * Inserts an array of log entries in a bulk insert operation.
	 * The entry timestamp will be overwritten by the bulk operation time if used with nRF Logger 1.5 or older.
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)}
	 * @param values
	 * 			  an array of values obtained using {@link #logEntry(ILogSession, int, String)} method
	 */
	public static void log(final ILogSession session, final ContentValues[] values) {
		if (session == null || values == null || values.length == 0)
			return;

		try {
			session.getContext().getContentResolver().bulkInsert(session.getSessionEntriesUri(), values);
		} catch (final Exception e) {
			// the nRF Logger application is not installed, do nothing
		}
	}

	/**
	 * Inserts a list of log entries in a bulk insert operation.
	 * The entry timestamp will be overwritten by the bulk operation time if used with nRF Logger 1.5 or older.
	 * @param session
	 *            the session created using {@link #newSession(Context, String, String)}
	 * @param values
	 * 			  a list of values obtained using {@link #logEntry(ILogSession, int, String)} method
	 */
	public static void log(final ILogSession session, final List<ContentValues> values) {
		if (session == null || values == null || values.isEmpty())
			return;

		try {
			session.getContext().getContentResolver().bulkInsert(session.getSessionEntriesUri(), values.toArray(new ContentValues[values.size()]));
		} catch (final Exception e) {
			// the nRF Logger application is not installed, do nothing
		}
	}
}
