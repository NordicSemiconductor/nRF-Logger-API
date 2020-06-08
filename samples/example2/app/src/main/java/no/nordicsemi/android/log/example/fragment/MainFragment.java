package no.nordicsemi.android.log.example.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LocalLogSession;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.LogContract.Log.Level;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;
import no.nordicsemi.android.log.example.LogAdapter;
import no.nordicsemi.android.log.example.R;
import no.nordicsemi.android.log.example.localprovider.MyLogContentProvider;

public class MainFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String SIS_SESSION_URL = "session_url";
	private static final String SIS_SELECTED_LEVEL = "selected_level";

	private static final int LOG_REQUEST_ID = 1;
	private static final String[] LOG_PROJECTION = {
			LogContract.Log._ID,
			LogContract.Log.TIME,
			LogContract.Log.LEVEL,
			LogContract.Log.DATA
	};

	private View mField;
	private View mDropDown;
	private Button mAddButton;
	private Button mShowSessionInLoggerButton;
	private Button mShowAllSessionsInLoggerButton;

	/** The log session used to add entries. Log session is recreated after rotation change. */
	private ILogSession mLogSession;
	/** The adapter used to populate the list with log entries. */
	private CursorAdapter mLogAdapter;

	private int mSelectedLevel = 2;

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			Uri uri = savedInstanceState.getParcelable(SIS_SESSION_URL);
			mLogSession = Logger.openSession(requireContext(), uri);
			mSelectedLevel = savedInstanceState.getInt(SIS_SELECTED_LEVEL);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull final Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mLogSession != null) {
			// Save the session URI
			outState.putParcelable(SIS_SESSION_URL, mLogSession.getSessionUri());
			outState.putInt(SIS_SELECTED_LEVEL, mSelectedLevel);
		}
	}

	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
							 @Nullable final ViewGroup container,
							 @Nullable final Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);

		// Configure the first button
		getParentFragmentManager().setFragmentResultListener(KeyNameDialogFragment.RESULT, this,
				(requestKey, result) -> {
					final String key = result.getString(KeyNameDialogFragment.RESULT_KEY);
					final String name = result.getString(KeyNameDialogFragment.RESULT_NAME);
					createLogSession(key, name);
				});
		rootView.findViewById(R.id.action_create).setOnClickListener(v -> {
			DialogFragment dialog = new KeyNameDialogFragment();
			dialog.show(getParentFragmentManager(), null);
		});

		// Session may have been recreated after rotation change
		boolean sessionOpen = mLogSession != null;

		// Configure controls
		mField = rootView.findViewById(R.id.field_layout);
		mField.setEnabled(sessionOpen);

		mDropDown = rootView.findViewById(R.id.log_level_layout);
		mDropDown.setEnabled(sessionOpen);

		AutoCompleteTextView dropDown = rootView.findViewById(R.id.log_level);
		dropDown.setAdapter(ArrayAdapter.createFromResource(requireContext(),
				R.array.log_levels, R.layout.popup_levels_item));
		dropDown.setOnItemClickListener((parent, view, position, id) -> mSelectedLevel = position);
		dropDown.setText(requireContext().getResources().getTextArray(R.array.log_levels)[mSelectedLevel], false);

		EditText field = rootView.findViewById(R.id.field);
		Button addButton = mAddButton = rootView.findViewById(R.id.action_append);
		addButton.setEnabled(sessionOpen);
		addButton.setOnClickListener(v -> {
			String text = field.getText().toString();
			append(text, position2Level(mSelectedLevel));
		});

		Button openButton = mShowSessionInLoggerButton = rootView.findViewById(R.id.action_open);
		openButton.setEnabled(sessionOpen);
		openButton.setOnClickListener(v -> {
			if (mLogSession != null) {
				// Open the log session in any app that supports nRF Logger log provider, f.e. in nRF Logger
				Intent intent = new Intent(Intent.ACTION_VIEW, mLogSession.getSessionUri());
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			} else {
				// nRF Logger is not installed, open the Google Play
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=no.nordicsemi.android.log"));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		});

		Button openSessionsButton = mShowAllSessionsInLoggerButton = rootView.findViewById(R.id.action_open_sessions);
		openSessionsButton.setEnabled(sessionOpen);
		openSessionsButton.setOnClickListener(v -> {
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
		});

		return rootView;
	}

	@Override
	public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Create the log adapter, initially with null cursor
		mLogAdapter = new LogAdapter(requireContext());
		setListAdapter(mLogAdapter);

		// The mLogSession is not null when it was created before and the orientation changed afterwards
		if (mLogSession != null) {
			LoaderManager.getInstance(this).restartLoader(LOG_REQUEST_ID, null, this);
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
		} catch (final ClassCastException e) {
			// do nothing, nRF Logger is installed
		}
		super.onDestroy();
	}

	/**
	 * Creates the new log session and makes the controls enabled.
	 * If the nRF Logger application is not installed it will change the "Open in nRF Logger"
	 * button to "Download nRF Logger".
	 */
	private void createLogSession(String key, String name) {
		mLogSession = Logger.newSession(requireContext(), key, name);

		// Enable buttons
		mField.setEnabled(true);
		mDropDown.setEnabled(true);
		mAddButton.setEnabled(true);
		mShowSessionInLoggerButton.setEnabled(true);
		mShowAllSessionsInLoggerButton.setEnabled(true);

		// The session is null if nRF Logger is not installed
		if (mLogSession == null) {
			Toast.makeText(getActivity(), R.string.error_no_nrf_logger, Toast.LENGTH_SHORT).show();
			mLogSession = LocalLogSession.newSession(requireContext(), MyLogContentProvider.AUTHORITY_URI, key, name);

			// The button will be used to download the nRF Logger
			mShowAllSessionsInLoggerButton.setText(R.string.action_download);
			mShowSessionInLoggerButton.setEnabled(false);
		}

		LoaderManager.getInstance(this).restartLoader(LOG_REQUEST_ID, null, this);
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(final int id, @Nullable final Bundle args) {
		return new CursorLoader(requireContext(), mLogSession.getSessionEntriesUri(),
				LOG_PROJECTION, null, null, LogContract.Log.TIME);
	}

	@Override
	public void onLoadFinished(@NonNull final Loader<Cursor> loader, @Nullable final Cursor data) {
		mLogAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(@NonNull final Loader<Cursor> loader) {
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
	private void append(@NonNull final String text, final int level) {
		Logger.log(mLogSession, level, text);
	}

	private int position2Level(final int position) {
		switch (position) {
			case 0:
				return Level.DEBUG;
			case 1:
				return Level.VERBOSE;
			case 3:
				return Level.APPLICATION;
			case 4:
				return Level.WARNING;
			case 5:
				return Level.ERROR;
			default:
			case 2:
				return Level.INFO;
		}
	}
}
