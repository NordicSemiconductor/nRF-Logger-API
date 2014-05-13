package no.nordicsemi.android.log.example.fragment;

import no.nordicsemi.android.log.example.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
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

public class KeyNameDialogFragment extends DialogFragment implements OnEditorActionListener, DialogInterface.OnClickListener {
	private EditText mKeyView;
	private EditText mNameView;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_title).setNegativeButton(android.R.string.no, null).setPositiveButton(android.R.string.ok, this);

		View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_dialog_key_name, null);
		mKeyView = (EditText) view.findViewById(R.id.key);
		mNameView = (EditText) view.findViewById(R.id.name);
		builder.setView(view);
		return builder.create();
	}

	@Override
	/**
	 * Called when OK button has been pressed.
	 */
	public void onClick(DialogInterface dialog, int which) {
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
			MainFragment parentFragment = (MainFragment) getTargetFragment();
			parentFragment.createLogSession(key, name);
		}
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (EditorInfo.IME_ACTION_DONE == actionId) {
			// Return input text to activity
			onClick(null, DialogInterface.BUTTON_POSITIVE);
			return true;
		}
		return false;
	}
}
