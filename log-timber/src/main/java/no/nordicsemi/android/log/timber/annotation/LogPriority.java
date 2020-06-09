package no.nordicsemi.android.log.timber.annotation;

import android.util.Log;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
        Log.VERBOSE,
        Log.DEBUG,
        Log.INFO,
        Log.WARN,
        Log.ERROR,
        Log.ASSERT,
})
public @interface LogPriority {
}
