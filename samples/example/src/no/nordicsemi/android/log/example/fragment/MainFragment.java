package no.nordicsemi.android.log.example.fragment;

import no.nordicsemi.android.log.LogContract.Log.Level;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;
import no.nordicsemi.android.log.example.R;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class MainFragment extends Fragment {
	private static final String SIS_SESSION_URL = "session_url";

	private EditText mField;
	private Spinner mLogLevelSpinner;
	private Button mAddButton;
	private Button mShowSessionInLoggerButton;
	private Button mShowAllSessionsInLoggerButton;

	/** The log session used to add entries. Log session is recreated after rotation change. */
	private LogSession mLogSession;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			Uri uri = savedInstanceState.getParcelable(SIS_SESSION_URL);
			mLogSession = Logger.openSession(getActivity(), uri);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mLogSession != null) {
			// Save the session URI
			outState.putParcelable(SIS_SESSION_URL, mLogSession.getSessionUri());
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);

		// Configure the first button
		rootView.findViewById(R.id.action_create).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogFragment dialog = new KeyNameDialogFragment();
				dialog.setTargetFragment(MainFragment.this, 0);
				dialog.show(getFragmentManager(), null);
			}
		});

		// Session may have been recreated after rotation change
		boolean sessionOpen = mLogSession != null;

		// Configure controls
		mField = (EditText) rootView.findViewById(R.id.field);
		mField.setEnabled(sessionOpen);
		mLogLevelSpinner = (Spinner) rootView.findViewById(R.id.log_level);
		mLogLevelSpinner.setEnabled(sessionOpen);
		/*
		 * Set INFO level as default. This only applies when the view is created for the 1st time. The Spinner will save it's state if rotation change.
		 */
		mLogLevelSpinner.setSelection(2);

		Button addButton = mAddButton = (Button) rootView.findViewById(R.id.action_append);
		addButton.setEnabled(sessionOpen);
		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String text = mField.getText().toString();
				int position = mLogLevelSpinner.getSelectedItemPosition();

				int level = Level.INFO;
				switch (position) {
				case 0:
					level = Level.DEBUG;
					break;
				case 1:
					level = Level.VERBOSE;
					break;
				case 3:
					level = Level.WARNING;
					break;
				case 4:
					level = Level.ERROR;
					break;
				default:
				case 2:
					level = Level.INFO;
					break;
				}

				append(text, level);
			}
		});

		Button openButton = mShowSessionInLoggerButton = (Button) rootView.findViewById(R.id.action_open);
		openButton.setEnabled(sessionOpen);
		openButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mLogSession != null) {
					// Open the log session in any app that supports nRF Logger log provider, f.e. in nRF Logger 
					Intent intent = new Intent(Intent.ACTION_VIEW, mLogSession.getSessionUri());
					startActivity(intent);
				} else {
					// nRF Logger is not installed, open the Google Play
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=no.nordicsemi.android.log"));
					startActivity(intent);
				}
			}
		});

		Button openSessionsButton = mShowAllSessionsInLoggerButton = (Button) rootView.findViewById(R.id.action_open_sessions);
		openSessionsButton.setEnabled(sessionOpen);
		openSessionsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Open the sessions in any app that supports nRF Logger log provider, f.e. in nRF Logger 
				Intent intent = new Intent(Intent.ACTION_VIEW, mLogSession.getSessionsUri());
				startActivity(intent);
			}
		});

		return rootView;
	}

	/**
	 * Creates the new log session and makes the controls enabled. If the nRF Logger application is not installed it will change the "Open in nRF Logger" button to "Download nRF Logger".
	 */
	public void createLogSession(String key, String name) {
		mLogSession = Logger.newSession(getActivity(), key, name);
		// The session is null if nRF Logger is not installed
		if (mLogSession == null) {
			Toast.makeText(getActivity(), R.string.error_no_lgger, Toast.LENGTH_SHORT).show();

			// The button will be used to download the nRF Logger
			mShowSessionInLoggerButton.setText(R.string.action_download);
			mShowSessionInLoggerButton.setEnabled(true);
			return;
		}

		// The nRF Logger app exists
		mField.setEnabled(true);
		mLogLevelSpinner.setEnabled(true);
		mAddButton.setEnabled(true);
		mShowSessionInLoggerButton.setText(R.string.action_open);
		mShowSessionInLoggerButton.setEnabled(true);
		mShowAllSessionsInLoggerButton.setEnabled(true);
	}

	/**
	 * Appends a new log line.
	 * 
	 * @param text
	 *            the log content
	 * @param level
	 *            the entry level
	 */
	private void append(String text, int level) {
		Logger.log(mLogSession, level, text);

		mField.setText(null);
		mField.requestFocus();
	}
}
