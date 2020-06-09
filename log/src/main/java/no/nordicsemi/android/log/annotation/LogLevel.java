package no.nordicsemi.android.log.annotation;


import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import no.nordicsemi.android.log.LogContract;

@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
        LogContract.Log.Level.DEBUG,
        LogContract.Log.Level.VERBOSE,
        LogContract.Log.Level.INFO,
        LogContract.Log.Level.APPLICATION,
        LogContract.Log.Level.WARNING,
        LogContract.Log.Level.ERROR
})
public @interface LogLevel {
}
