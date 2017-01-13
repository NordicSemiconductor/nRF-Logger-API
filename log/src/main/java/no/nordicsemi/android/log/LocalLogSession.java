package no.nordicsemi.android.log;

import no.nordicsemi.android.log.localprovider.LocalLogContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * If the nRF Logger application is not installed on the phone/tablet application may still use the logger functionality with it's own provider. LocalLogSession is a limited version
 * of {@link LogSession}, it does not keep the application data. The local provider does not allow to mark sessions or add descriptions.
 */
public class LocalLogSession implements ILogSession {
	private final Context context;
	private final Uri sessionUri;

	/**
	 * Creates new logger session. Must be created before appending log entries. If the nRF Logger application is not installed the method will return <code>null</code>.
	 * 
	 * @param context
	 *            the context (activity, service or application)
	 * @param authority
	 *            the {@link LocalLogContentProvider} authority
	 * @param key
	 *            the session key, which is used to group sessions
	 * @param name
	 *            the human readable session name
	 * @return the {@link LogContract} that can be used to append log entries or <code>null</code> if MCP is not installed. The <code>null</code> value can be next passed to logging methods
	 */
	public static LocalLogSession newSession(final Context context, final Uri authority, final String key, final String name) {
		final Uri uri = authority.buildUpon().appendEncodedPath(LogContract.Session.SESSION_CONTENT_DIRECTORY).appendEncodedPath(LogContract.Session.KEY_CONTENT_DIRECTORY).appendEncodedPath(key)
				.build();
		final ContentValues values = new ContentValues();
		values.put(LogContract.Session.NAME, name);

		try {
			final Uri sessionUri = context.getContentResolver().insert(uri, values);
			return new LocalLogSession(context, sessionUri);
		} catch (final Exception e) {
			Log.e("LocalLogSession", "Error while creating a local log session.", e);
			return null;
		}
	}

	/* package */LocalLogSession(final Context context, final Uri sessionUri) {
		this.context = context;
		this.sessionUri = sessionUri;
	}

	/**
	 * Deletes the session and all its entries from the local database.
	 */
	public void delete() {
		try {
			context.getContentResolver().delete(sessionUri, null, null);
		} catch (final Exception e) {
			Log.e("LocalLogSession", "Error while deleting local log session.", e);
		}
	}

	@Override
	public Context getContext() {
		return context;
	}

	@Override
	public Uri getSessionUri() {
		return sessionUri;
	}

	@Override
	public Uri getSessionEntriesUri() {
		return sessionUri.buildUpon().appendEncodedPath(LogContract.Log.CONTENT_DIRECTORY).build();
	}

	@Override
	public Uri getSessionContentUri() {
		return sessionUri.buildUpon().appendEncodedPath(LogContract.Log.CONTENT_DIRECTORY).appendEncodedPath(LogContract.Session.Content.CONTENT).build();
	}

}
