package no.nordicsemi.android.log.example.fragment;

import no.nordicsemi.android.log.example.R;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class KeyNameDialogFragment extends DialogFragment implements
		OnEditorActionListener, DialogInterface.OnClickListener {
	public static final String RESULT = "session";
	public static final String RESULT_KEY = "session_key";
	public static final String RESULT_NAME = "session_name";

	private EditText mKeyView;
	private EditText mNameView;

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
				.setTitle(R.string.dialog_title)
				.setNegativeButton(android.R.string.no, null)
				.setPositiveButton(android.R.string.ok, this);

		final View view = LayoutInflater.from(requireContext())
				.inflate(R.layout.fragment_dialog_key_name, null);
		mKeyView = view.findViewById(R.id.key);
		mNameView = view.findViewById(R.id.name);
		builder.setView(view);
		return builder.create();
	}

	/**
	 * Called when OK button has been pressed.
	 */
	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		// Validate key and name
		String key = mKeyView.getText().toString().trim();
		String name = mNameView.getText().toString().trim();
		boolean valid = true;

		if (TextUtils.isEmpty(key)) {
			valid = false;
			mKeyView.setError(getString(R.string.dialog_error));
		}

		if (TextUtils.isEmpty(name)) {
			valid = false;
			mNameView.setError(getString(R.string.dialog_error));
		}

		if (valid) {
			final Bundle result = new Bundle();
			result.putString(KeyNameDialogFragment.RESULT_KEY, key);
			result.putString(KeyNameDialogFragment.RESULT_NAME, name);
			getParentFragmentManager().setFragmentResult(KeyNameDialogFragment.RESULT, result);
		}
	}

	@Override
	public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
		if (EditorInfo.IME_ACTION_DONE == actionId) {
			// Return input text to activity
			onClick(null, DialogInterface.BUTTON_POSITIVE);
			return true;
		}
		return false;
	}
}
