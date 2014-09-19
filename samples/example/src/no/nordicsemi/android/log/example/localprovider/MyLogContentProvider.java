package no.nordicsemi.android.log.example.localprovider;

import no.nordicsemi.android.log.localprovider.LocalLogContentProvider;
import android.net.Uri;

public class MyLogContentProvider extends LocalLogContentProvider {

	@Override
	protected Uri getAuthorityUri() {
		return LocalLogContract.AUTHORITY_URI;
	}

}
