/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.log;

import android.content.ContentProvider;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The log session. This object can be created with the use of
 * {@link Logger#newSession(Context, String, String)} and is used to append new log entries to
 * the nRF Logger log.
 */
@SuppressWarnings("unused")
public class LogSession implements ILogSession {
	private final Context context;
	private final Uri sessionUri;

	/* package */LogSession(final Context context, final Uri sessionUri) {
		this.context = context.getApplicationContext();
		this.sessionUri = sessionUri;
	}

	@NonNull
	@Override
	public Context getContext() {
		return context;
	}

	@NonNull
	@Override
	public Uri getSessionUri() {
		return sessionUri;
	}

	@NonNull
	@Override
	public Uri getSessionEntriesUri() {
		return sessionUri.buildUpon().appendEncodedPath(LogContract.Log.CONTENT_DIRECTORY).build();
	}

	/**
	 * Returns the {@link Uri} that may by used to obtain all sessions created by the same
	 * application (and the same profile) as this session. It may be used to open the list of
	 * log sessions in the nRF Logger application or to obtain list of sessions using
	 * {@link ContentProvider}. Keep in mind that sessions with {@link LogContract.Session#NUMBER}
	 * equal to 0 are "date sessions". Date sessions are created each time a new session is being
	 * added by the application (and profile) in a new day. See {@link Logger} for more information.
	 * 
	 * <pre>
	 * Intent intent = new Intent(Intent.ACTION_VIEW, mLogSession.getSessionsUri());
	 * startActivity(intent);
	 * </pre>
	 * 
	 * @return The Uri for all sessions created by the app used to create this session or
	 * <code>null</code> if the session Uri is invalid or the owner app data does not exist in the
	 * database.
	 */
	@SuppressWarnings({"ConstantConditions", "TryFinallyCanBeTryWithResources"})
	@Nullable
	public Uri getSessionsUri() {
		try {
			final Cursor cursor = context.getContentResolver().query(sessionUri,
					new String[] { LogContract.Session.APPLICATION_ID },
					null, null, null);
			try {
				if (cursor.moveToNext()) {
					final long appId = cursor.getLong(0);
					return LogContract.Session.createSessionsUri(appId);
				}
				return null;
			} finally {
				cursor.close();
			}
		} catch (final Exception e) {
			return null;
		}
	}

	@NonNull
	@Override
	public Uri getSessionContentUri() {
		return sessionUri.buildUpon()
				.appendEncodedPath(LogContract.Log.CONTENT_DIRECTORY)
				.appendEncodedPath(LogContract.Session.Content.CONTENT)
				.build();
	}

	@NonNull
	@Override
	public String toString() {
		return sessionUri.toString();
	}
}
