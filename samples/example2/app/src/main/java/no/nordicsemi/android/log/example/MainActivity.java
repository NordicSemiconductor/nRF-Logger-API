package no.nordicsemi.android.log.example;

import android.content.ContentProviderClient;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.example.fragment.MainFragment;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Show information if nRF Logger is not installed
		if (!logProviderExists()) {
			Toast.makeText(this, R.string.error_no_nrf_logger, Toast.LENGTH_SHORT).show();
		}

		// Show the main fragment
		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.add(R.id.container, new MainFragment())
					.commit();
		}
	}

	private boolean logProviderExists() {
		// The method below requires API 16
		final ContentProviderClient unstableClient = getContentResolver()
				.acquireUnstableContentProviderClient(LogContract.AUTHORITY);
		if (unstableClient == null)
			return false;

		unstableClient.release();
		return true;
	}

}
