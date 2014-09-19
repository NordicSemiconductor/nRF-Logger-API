package no.nordicsemi.android.log.example.localprovider;

import no.nordicsemi.android.log.LogContract;
import android.net.Uri;

public class LocalLogContract extends LogContract {
	/** The authority for the contacts provider. */
	public static final String AUTHORITY = "no.nordicsemi.android.log.example";
	/** A content:// style uri to the authority for the log provider. */
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

	public final static class LocalSession {
		/**
		 * The content:// style URI for log session table. Can be used directly for all sessions, with session id (one or 0 sessions returned) or with content directories.
		 */
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "session");
	}
}
