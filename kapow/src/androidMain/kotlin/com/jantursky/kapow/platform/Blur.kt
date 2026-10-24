package com.jantursky.kapow.platform

import android.os.Build

internal actual fun blurSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
