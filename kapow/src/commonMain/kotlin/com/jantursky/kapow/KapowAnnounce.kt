package com.jantursky.kapow

/** Whether a text composable tells screen readers when its text changes. */
public enum class KapowAnnounce {
    /** The text is exposed to assistive technology but not announced when it changes. */
    Off,

    /**
     * The text is a polite live region: a screen reader reads the new text when it has finished what it is saying.
     * [KapowCounter] announces its value only after the value has stopped changing for half a second;
     * [KapowTypewriter] announces the full text at once, never the letters as they appear.
     */
    Polite,
}

/** How long a [KapowCounter] value must stay unchanged before it is announced. */
internal const val ANNOUNCE_SETTLE_MILLIS: Long = 500L
