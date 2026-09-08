package com.watanuki.sources

/** One vendored source as declared in its upstream `build.gradle`. */
data class SourceEntry(
	val name: String,
	val lang: String,
	val className: String,
	val isNsfw: Boolean,
)
