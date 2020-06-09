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

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.util.Log;

import no.nordicsemi.android.log.localprovider.LocalLogContentProvider;

/**
 * If the nRF Logger application is not installed on the phone/tablet application may still use
 * the logger functionality with it's own provider. LocalLogSession is a limited version of
 * {@link LogSession}, it does not keep the application data.
 * The local provider does not allow to mark sessions or add descriptions.
 */
@SuppressWarnings("unused")
public class LocalLogSession implements ILogSession {
	private final Context context;
	private final Uri sessionUri;

	/**
	 * Creates new logger session. Must be created before appending log entries.
	 * If the nRF Logger application is not installed the method will return <code>null</code>.
	 * 
	 * @param context
	 *            the context (activity, service or application)
	 * @param authority
	 *            the {@link LocalLogContentProvider} authority
	 * @param key
	 *            the session key, which is used to group sessions
	 * @param name
	 *            the human readable session name
	 * @return the {@link LogContract} that can be used to append log entries or <code>null</code>
	 * if MCP is not installed. The <code>null</code> value can be next passed to logging methods
	 */
	public static LocalLogSession newSession(@NonNull final Context context,
											 @NonNull final Uri authority,
											 @NonNull final String key, @NonNull final String name) {
		final Uri uri = authority.buildUpon()
				.appendEncodedPath(LogContract.Session.SESSION_CONTENT_DIRECTORY)
				.appendEncodedPath(LogContract.Session.KEY_CONTENT_DIRECTORY)
				.appendEncodedPath(key)
				.build();
		final ContentValues values = new ContentValues();
		values.put(LogContract.Session.NAME, name);

		try {
			final Uri sessionUri = context.getContentResolver().insert(uri, values);
			if (sessionUri != null)
				return new LocalLogSession(context, sessionUri);
			return null;
		} catch (final Exception e) {
			Log.e("LocalLogSession", "Error while creating a local log session.", e);
			return null;
		}
	}

	/* package */LocalLogSession(@NonNull final Context context, @NonNull final Uri sessionUri) {
		this.context = context.getApplicationContext();
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
		return sessionUri.buildUpon()
				.appendEncodedPath(LogContract.Log.CONTENT_DIRECTORY)
				.build();
	}

	@NonNull
	@Override
	public Uri getSessionContentUri() {
		return sessionUri.buildUpon()
				.appendEncodedPath(LogContract.Log.CONTENT_DIRECTORY)
				.appendEncodedPath(LogContract.Session.Content.CONTENT)
				.build();
	}

}
