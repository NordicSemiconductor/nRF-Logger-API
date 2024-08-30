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

package no.nordicsemi.android.log.timber;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.Logger;
import no.nordicsemi.android.log.timber.annotation.LogPriority;
import timber.log.Timber;

/**
 * A Timber tree that logs messages to nRF Logger app or
 * {@link no.nordicsemi.android.log.localprovider.LocalLogContentProvider}.
 * <p>
 * If neither can be found, the logs will be ignored.
 */
@SuppressWarnings("unused")
public class nRFLoggerTree extends Timber.Tree {
	@Nullable
	private ILogSession session;

	private boolean logTags = true;

	// Constructors

	/**
	 * Creates a new instance of nRFLoggerTree.
	 *
	 * @param context The context.
	 * @param key The key of the session. Sessions with the same key will be grouped together.
	 * @param name An optional name of the session.
	 */
	public nRFLoggerTree(final @NonNull Context context,
						 final @NonNull String key, final @Nullable String name) {
		this.session = Logger.newSession(context, null, key, name);
	}

	/**
	 * Creates a new instance of nRFLoggerTree.
	 *
	 * @param context The context.
	 * @param profile The profile name, which will be concatenated to the app's name in nRF Logger.
	 * @param key The key of the session. Sessions with the same key will be grouped together.
	 * @param name An optional name of the session.
	 */
	public nRFLoggerTree(final @NonNull Context context,
						 final @Nullable String profile,
						 final @NonNull String key, final @Nullable String name) {
		this.session = Logger.newSession(context, profile, key, name);
	}

	/**
	 * Creates a new instance of nRFLoggerTree.
	 *
	 * @param session The log session. If null, the logs will be ignored.
	 */
	public nRFLoggerTree(final @Nullable ILogSession session) {
		this.session = session;
	}

	/**
	 * Creates a new instance of nRFLoggerTree.
	 *
	 * @param context The context.
	 * @param uri The URI of the session. The URI must point to the session in nRF Logger or
	 *            {@link no.nordicsemi.android.log.localprovider.LocalLogContentProvider} session.
	 *            If null, the logs will be ignored.
	 */
	public nRFLoggerTree(final @NonNull Context context, final @Nullable Uri uri) {
		this.session = Logger.openSession(context, uri);
	}

	// Public API

	/**
	 * Reuses the existing tree to log to a new log session.
	 *
	 * @param key The key of the session. Sessions with the same key will be grouped together.
	 * @param name An optional name of the session.
	 */
	public void newSession(final @NonNull String key, final @Nullable String name) {
		if (session != null) {
			session = Logger.newSession(session.getContext(), null, key, name);
		}
	}

	/**
	 * Reuses the existing tree to log to a new log session.
	 *
	 * @param profile The profile name, which will be concatenated to the app's name in nRF Logger.
	 * @param key The key of the session. Sessions with the same key will be grouped together.
	 * @param name An optional name of the session.
	 */
	public void newSession(final @Nullable String profile,
						   final @NonNull String key, final @Nullable String name) {
		if (session != null) {
			session = Logger.newSession(session.getContext(), profile, key, name);
		}
	}

	/**
	 * Returns the current log session.
	 * <p>
	 * The session may be used to get the {@link ILogSession#getSessionUri()}.
	 * @return The log session.
	 */
	@Nullable
	public ILogSession getSession() {
		return session;
	}

	/**
	 * Sets whether the non-null tags should be logged as:
	 * <p>
	 * <code>[&lt;tag&gt;] &lt;message&gt;</code>
	 * </p>
	 * @param enable true to enable (default), false to disable.
	 */
	public void setLoggingTagsEnabled(final boolean enable) {
		this.logTags = enable;
	}

	// Tree API

	@Override
	protected boolean isLoggable(@Nullable final String tag, @LogPriority final int priority) {
		return session != null;
	}

	@Override
	protected void log(@LogPriority final int priority, @Nullable final String tag,
					   @NonNull final String message, @Nullable final Throwable t) {
		if (session == null)
			return;

		final int level = LogContract.Log.Level.fromPriority(priority);

		// Ignore t. Stack trace is already added to the message by prepareLog

		if (!logTags || tag == null || tag.isEmpty()) {
			Logger.log(session, level, message);
		} else {
			Logger.log(session, level, "[" + tag + "] " + message);
		}
	}
}
