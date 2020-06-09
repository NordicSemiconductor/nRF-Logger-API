package no.nordicsemi.android.log.annotation;


import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import no.nordicsemi.android.log.Logger;

@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
        Logger.MARK_CLEAR,
        Logger.MARK_STAR_YELLOW,
        Logger.MARK_STAR_BLUE,
        Logger.MARK_STAR_RED,
        Logger.MARK_FLAG_YELLOW,
        Logger.MARK_FLAG_BLUE,
        Logger.MARK_FLAG_RED,
})
public @interface LogMark {
}
