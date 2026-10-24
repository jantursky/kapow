package com.jantursky.kapow.platform

/** Whether a graphics layer can blur here. Android draws render effects from API 31; Skia targets always can. */
internal expect fun blurSupported(): Boolean
