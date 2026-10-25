package com.jantursky.kapow.samples.gallery

/**
 * The arguments for a transition's code sample. A null entry is left out, so an option that still has its default
 * does not clutter the call: pass `("softEdge" to value).takeIf { value > 0f }`.
 */
internal fun transitionArgs(vararg args: Pair<String, Any>?): Array<Pair<String, Any>> =
    args.filterNotNull().toTypedArray()
