package no.nordicsemi.android.log.example.fragment;

import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LocalLogSession;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.LogContract.Log.Level;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;
import no.nordicsemi.android.log.example.LogAdapter;
import no.nordicsemi.android.log.example.R;
import no.nordicsemi.android.log.example.localprovider.LocalLogContract;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class MainFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	private static final String SIS_SESSION_URL = "session_url";

	private static final int LOG_REQUEST_ID = 1;
	private static final String[] LOG_PROJECTION = { LogContract.Log._ID, LogContract.Log.TIME, LogContract.Log.LEVEL, LogContract.Log.DATA };

	private EditText mField;
	private Spinner mLogLevelSpinner;
	private Button mAddButton;
	private Button mShowSessionInLoggerButton;
	private Button mShowAllSessionsInLoggerButton;

	/** The log session used to add entries. Log session is recreated after rotation change. */
	private ILogSession mLogSession;
	/** The adapter used to populate the list with log entries. */
	private CursorAdapter mLogAdapter;

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
					level = Level.APPLICATION;
					break;
				case 4:
					level = Level.WARNING;
					break;
				case 5:
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
				if (mLogSession instanceof LogSession) {
					// Open the sessions in any app that supports nRF Logger log provider, f.e. in nRF Logger 
					Intent intent = new Intent(Intent.ACTION_VIEW, ((LogSession) mLogSession).getSessionsUri());
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				} else {
					// nRF Logger is not installed, open the Google Play
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=no.nordicsemi.android.log"));
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
			}
		});

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Create the log adapter, initially with null cursor
		mLogAdapter = new LogAdapter(getActivity());
		setListAdapter(mLogAdapter);

		// The mLogSession is not null when it was created before and the orientation changed afterwards
		if (mLogSession != null) {
			getLoaderManager().restartLoader(LOG_REQUEST_ID, null, this);
		}
	}

	@Override
	public void onDestroy() {
		try {
			// Let's delete the local log session when exit
			if (mLogSession != null) {
				LocalLogSession session = (LocalLogSession) mLogSession;
				session.delete();
			}
		} catch (ClassCastException e) {
			// do nothing, nRF Logger is installed
		}
		super.onDestroy();
	}

	/**
	 * Creates the new log session and makes the controls enabled. If the nRF Logger application is not installed it will change the "Open in nRF Logger" button to "Download nRF Logger".
	 */
	public void createLogSession(String key, String name) {
		mLogSession = Logger.newSession(getActivity(), key, name);

		// Enable buttons
		mField.setEnabled(true);
		mLogLevelSpinner.setEnabled(true);
		mAddButton.setEnabled(true);
		mShowSessionInLoggerButton.setEnabled(true);
		mShowAllSessionsInLoggerButton.setEnabled(true);

		// The session is null if nRF Logger is not installed
		if (mLogSession == null) {
			Toast.makeText(getActivity(), R.string.error_no_nrf_logger, Toast.LENGTH_SHORT).show();
			mLogSession = LocalLogSession.newSession(getActivity(), LocalLogContract.AUTHORITY_URI, key, name);

			// The button will be used to download the nRF Logger
			mShowAllSessionsInLoggerButton.setText(R.string.action_download);
			mShowSessionInLoggerButton.setEnabled(false);
		}

		getLoaderManager().restartLoader(LOG_REQUEST_ID, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case LOG_REQUEST_ID: {
			return new CursorLoader(getActivity(), mLogSession.getSessionEntriesUri(), LOG_PROJECTION, null, null, LogContract.Log.TIME);
		}
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mLogAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mLogAdapter.swapCursor(null);
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
