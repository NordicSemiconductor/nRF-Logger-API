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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

@SuppressWarnings("unused")
public interface ILogSession {

	/**
	 * Returns the application context.
	 *
	 * @return the context
	 */
	@NonNull
	Context getContext();

	/**
	 * Returns the session {@link Uri}. The Uri may be saved in
	 * {@link Activity#onSaveInstanceState(android.os.Bundle)} to recreate the session using
	 * {@link Logger#openSession(Context, Uri)} when orientation change.
	 * Use this Uri also to open the log session in the nRF Logger.
	 *
	 * <pre>
	 * Intent intent = new Intent(Intent.ACTION_VIEW, mLogSession.getSessionUri());
	 * startActivity(intent);
	 * </pre>
	 *
	 * @return the session Uri
	 */
	@NonNull
	Uri getSessionUri();

	/**
	 * Returns the session entries {@link Uri}. New log entries may be inserted using this Uri.
	 *
	 * @return the session entries Uri
	 */
	@NonNull
	Uri getSessionEntriesUri();

	/**
	 * Returns the session read-only content Uri. It can be used to obtain all log entries
	 * in a single row (as a String field) with fixed syntax, e.g.:
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
	 * @return The {@link Uri} that can be read using
	 * {@link ContentResolver#query(Uri, String[], String, String[], String)} method.
	 * The value will be in the first row, column number 0
	 * (with id: {@link LogContract.Session.Content#CONTENT}).
	 */
	@NonNull
	Uri getSessionContentUri();
}
