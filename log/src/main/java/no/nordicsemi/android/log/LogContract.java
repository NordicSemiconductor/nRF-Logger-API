/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.log;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * The log contract is used to write and read the log entries into the database. Do not use directly when writing, use {@link Logger} member methods instead.
 */
public class LogContract {
	/** The authority for the contacts provider. */
	public static final String AUTHORITY = "no.nordicsemi.android.log";
	/** A content:// style uri to the authority for the log provider. */
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

	protected interface LogColumns {
		/** The session id. This value is auto incremented for each new session. */
		String SESSION_ID = "session_id";
		/** The time the log entry was saved. This value is inserted automatically and will be overwritten by the provider is preset. */
		String TIME = "time";
		/** Log entry level. See {@link LogContract.Log.Level} for options. */
		String LEVEL = "level";
		/** The log entry data. */
		String DATA = "data";
	}

	protected interface SessionColumns {
		/** The key. This can be a device address or other property to group multiple sessions. */
		String KEY = "key";
		/** The human readable name of the session. This can be the device name. */
		String NAME = "name";
		/** The id the application that created the session. */
		String APPLICATION_ID = "application_id";
		/** The time the session was created. This value is inserted automatically and will be overwritten by the provider is preset. */
		String CREATED_AT = "created_at";
		/** The number of the session. This value incremented updated automatically when you insert new session. */
		String NUMBER = "number";
		/** The optional session description. */
		String DESCRIPTION = "description";
		/** The optional session mark. Default 0. */
		String MARK = "mark";
	}

	protected interface ApplicationColumns {
		/** The name of the application that created the session. */
		String APPLICATION = "application";
	}

	public final static class Log implements BaseColumns, LogColumns {
		private Log() {
			// empty
		}

		/**
		 * The directory twig for session log entries.
		 */
		public static final String CONTENT_DIRECTORY = "log";

		/**
		 * Returns the {@link Uri} to log entries from session with given id.
		 * 
		 * @param sessionId
		 *            the session id
		 * @return the {@link Uri} that can be used to read log entries from
		 */
		public static Uri createUri(final long sessionId) {
			return Session.CONTENT_URI.buildUpon().appendEncodedPath(String.valueOf(sessionId)).appendEncodedPath(CONTENT_DIRECTORY).build();
		}

		/**
		 * Returns the {@link Uri} to log entries from session with given id.
		 * 
		 * @param key
		 *            the session key
		 * @param number
		 *            the session number. Numbers are incremented automatically for each key starting from 1 when creating new session.
		 * @return the {@link Uri} that can be used to read log entries from
		 */
		public static Uri createUri(final String key, final int number) {
			return Session.CONTENT_URI.buildUpon().appendEncodedPath(Session.KEY_CONTENT_DIRECTORY).appendEncodedPath(key).appendEncodedPath(String.valueOf(number))
					.appendEncodedPath(CONTENT_DIRECTORY).build();
		}

		/**
		 * The MIME type of {@link Session#CONTENT_URI} providing a directory of log entries.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/no.nordicsemi.android.log.enties";

		/**
		 * The MIME type of a {@link Session#CONTENT_URI} subdirectory of a single log entry.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/no.nordicsemi.android.log.entry";

		/**
		 * A helper class that contains predefined static level values:
		 * <ul>
		 * <li>{@link #DEBUG}</li>
		 * <li>{@link #VERBOSE}</li>
		 * <li>{@link #INFO}</li>
		 * <li>{@link #APPLICATION}</li>
		 * <li>{@link #WARNING}</li>
		 * <li>{@link #ERROR}</li>
		 * </ul>
		 */
		public final class Level {
			/** Level used just for debugging purposes. It has lowest level */
			public final static int DEBUG = 0;
			/** Log entries with minor importance */
			public final static int VERBOSE = 1;
			/** Default logging level for important entries */
			public final static int INFO = 5;
			/** Log entries level for applications */
			public final static int APPLICATION = 10;
			/** Log entries with high importance */
			public final static int WARNING = 15;
			/** Log entries with very high importance, like errors */
			public final static int ERROR = 20;

			private Level() {
				// empty
			}
		}
	}

	public final static class Session implements BaseColumns, SessionColumns {
		private Session() {
			// empty
		}

		/**
		 * The directory twig for sessions. Must be used with concatenated key or session id, f.e:
		 * <ul>
		 * <li>{@code /session/key/[MY_KEY]} returns all sessions with given key,</li>
		 * <li>{@code /session/key/[MY_KEY]/[SESSION_NUMBER]} returns the session with given key and number. Number is not the session id. Numbers are incremented from 1 for each key independently.
		 * Use {@code /session/[ID]} if session id required.</li>
		 * <li>{@code /session/key/[MY_KEY]/[SESSION_NUMBER]/Log.CONTENT_DIRECTORY} returns the log entries from the session above</li>
		 * </ul>
		 */
		public static final String SESSION_CONTENT_DIRECTORY = "session";

		/**
		 * The directory twig for sessions with given key. Must be used with concatenated key f.e.
		 * <ul>
		 * <li>{@code /session/key/[MY_KEY]} returns all sessions with given key,</li>
		 * <li>{@code /session/key/[MY_KEY]/[SESSION_NUMBER]} returns the session with given key and number. Number is not the session id. Numbers are incremented from 1 for each key independently.
		 * Use {@code /session/[ID]} if session id required.</li>
		 * <li>{@code /session/key/[MY_KEY]/[SESSION_NUMBER]/Log.CONTENT_DIRECTORY} returns the log entries from the session above</li>
		 * </ul>
		 */
		public static final String KEY_CONTENT_DIRECTORY = "key";

		/**
		 * The directory twig for session from given application. Must be used with concatenated application id.
		 */
		public static final String APPLICATION_CONTENT_DIRECTORY = "application";

		/**
		 * The content:// style URI for log session table. Can be used directly for all sessions, with session id (one or 0 sessions returned) or with content directories.
		 */
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, SESSION_CONTENT_DIRECTORY);

		/**
		 * Returns the {@link Uri} to session with given id.
		 * 
		 * @param sessionId
		 *            the session id
		 * @return the {@link Uri} that can be used to read session log entries
		 */
		public static Uri createUri(final long sessionId) {
			return CONTENT_URI.buildUpon().appendEncodedPath(String.valueOf(sessionId)).build();
		}

		/**
		 * Returns the {@link Uri} to session with given key and number.
		 * 
		 * @param key
		 *            the session key
		 * @param number
		 *            the session number. Numbers are incremented automatically for each key starting from 1 when creating new session.
		 * @return the {@link Uri} that can be used to read session
		 */
		public static Uri createUri(final String key, final int number) {
			return CONTENT_URI.buildUpon().appendEncodedPath(KEY_CONTENT_DIRECTORY).appendEncodedPath(key).appendEncodedPath(String.valueOf(number)).build();
		}

		/**
		 * Returns the {@link Uri} to sessions from given application.
		 * 
		 * @param applicationId
		 *            the application id
		 * @return the {@link Uri} that can be used to read log sessions
		 */
		public static Uri createSessionsUri(final long applicationId) {
			return Session.CONTENT_URI.buildUpon().appendEncodedPath(APPLICATION_CONTENT_DIRECTORY).appendEncodedPath(String.valueOf(applicationId)).build();
		}

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of sessions.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/no.nordicsemi.android.log.sessions";

		/**
		 * The MIME type of a {@link #CONTENT_URI} subdirectory of a single log session.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/no.nordicsemi.android.log.session";

		public final static class Content {
			/**
			 * If added after the session number will remove the session content as a single field with fixed formatting.
			 */
			public static final String CONTENT = "content";
		}
	}

	public final static class Application implements BaseColumns, ApplicationColumns {
		private Application() {
			// empty
		}

		/**
		 * The directory twig for session from given application. Must be used with concatenated application id.
		 */
		public static final String APPLICATION_CONTENT_DIRECTORY = "application";

		/**
		 * The content:// style URI for log application table. Can be used directly for all applications or with appended id for single applications with given id.
		 */
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, APPLICATION_CONTENT_DIRECTORY);

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of applications.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/no.nordicsemi.android.log.applications";

		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single application.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/no.nordicsemi.android.log.application";
	}
}
