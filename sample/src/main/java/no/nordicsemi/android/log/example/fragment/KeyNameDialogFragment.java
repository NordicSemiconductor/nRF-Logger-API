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

package no.nordicsemi.android.log.example.fragment;

import no.nordicsemi.android.log.example.R;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class KeyNameDialogFragment extends DialogFragment implements
		OnEditorActionListener, View.OnClickListener {
	public static final String RESULT = "session";
	public static final String RESULT_KEY = "session_key";
	public static final String RESULT_NAME = "session_name";

	private EditText mKeyView;
	private EditText mNameView;

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
		final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
				.setTitle(R.string.dialog_title)
				.setNegativeButton(android.R.string.no, null)
				.setPositiveButton(android.R.string.ok, null);

		final View view = getLayoutInflater()
				.inflate(R.layout.fragment_dialog_key_name, null);
		mKeyView = view.findViewById(R.id.key);
		mNameView = view.findViewById(R.id.name);
		builder.setView(view);

		// Setting onClickListener to a button this way allows to
		// validate the input.
		final AlertDialog dialog = builder.create();
		dialog.show();
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
		return dialog;
	}

	/**
	 * Called when OK button has been pressed.
	 */
	@Override
	public void onClick(final View v) {
		// Validate key and name
		String key = mKeyView.getText().toString().trim();
		String name = mNameView.getText().toString().trim();
		boolean valid = true;

		if (TextUtils.isEmpty(key)) {
			valid = false;
			mKeyView.setError(getString(R.string.dialog_error));
		}

		if (valid) {
			final Bundle result = new Bundle();
			result.putString(KeyNameDialogFragment.RESULT_KEY, key);
			result.putString(KeyNameDialogFragment.RESULT_NAME, name);
			getParentFragmentManager().setFragmentResult(KeyNameDialogFragment.RESULT, result);
			dismiss();
		}
	}

	@Override
	public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
		if (EditorInfo.IME_ACTION_DONE == actionId) {
			// Return input text to activity
			onClick(null);
			return true;
		}
		return false;
	}
}
