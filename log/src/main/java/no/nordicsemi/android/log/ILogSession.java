package no.nordicsemi.android.log;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

public interface ILogSession {

	/**
	 * Returns the application context.
	 * 
	 * @return the context
	 */
	Context getContext();

	/**
	 * Returns the session {@link Uri}. The Uri may be saved in {@link Activity#onSaveInstanceState(android.os.Bundle)} to recreate the session using {@link Logger#openSession(Context, Uri)} when
	 * orientation change. Use this Uri also to open the log session in the nRF Logger.
	 * 
	 * <pre>
	 * Intent intent = new Intent(Intent.ACTION_VIEW, mLogSession.getSessionUri());
	 * startActivity(intent);
	 * </pre>
	 * 
	 * @return the session Uri
	 */
	Uri getSessionUri();

	/**
	 * Returns the session entries {@link Uri}. New log entries may be inserted using this Uri.
	 * 
	 * @return the session entries Uri
	 */
	Uri getSessionEntriesUri();

	/**
	 * Returns the session read-only content Uri. It can be used to obtain all log entries in a single row (as a String field) with fixed syntax:<br>
	 * e.g.:
	 * 
	 * <pre>
	 * [Application name], [Creation date]
	 * [Session name] ([Session key])
	 * D	10.34.01.124	This is the oldest log message (debug)
	 * V	10.34.02.238	This is the log message (verbose)
	 * I	10.34.03.527	This is the log message (info)
	 * A	10.34.04.612	This is the log message (application)
	 * W	10.34.04.812	This is the log message (warning)
	 * E	10.34.05.452	This is the newest log message (error)
	 * </pre>
	 * 
	 * @return the {@link Uri} that can be read using {@link ContentResolver#query(Uri, String[], String, String[], String)} method. The value will be in the first row, column number 0 (with id:
	 *         {@link LogContract.Session.Content#CONTENT}).
	 */
	Uri getSessionContentUri();
}
