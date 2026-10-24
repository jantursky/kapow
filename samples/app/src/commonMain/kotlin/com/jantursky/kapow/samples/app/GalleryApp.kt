package com.jantursky.kapow.samples.app

import androidx.compose.runtime.Composable
import com.jantursky.kapow.multihaptic.rememberMultihapticFeedback
import com.jantursky.kapow.samples.gallery.MobileGallery
import com.jantursky.kapow.samples.gallery.entryById

/**
 * The gallery app on a phone or tablet, with the device's vibrator behind the Haptics switch. [entryId] opens
 * that catalog entry first (null or an unknown id for the overview) and [dark] picks the starting theme (null
 * follows the system), so a test run can open any page directly. [onDarkChange] reports the theme in use.
 */
@Composable
fun GalleryApp(
    entryId: String? = null,
    dark: Boolean? = null,
    onDarkChange: (Boolean) -> Unit = {},
) {
    MobileGallery(
        initialEntry = entryId?.let(::entryById),
        initialDark = dark,
        haptics = rememberMultihapticFeedback(),
        onDarkChange = onDarkChange,
    )
}
