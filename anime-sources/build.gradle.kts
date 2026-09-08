import java.io.File

/*
 * :anime-sources
 *
 * Compiles the Aniyomi extension sources (Kohi-den/extensions-source, Apache 2.0) straight
 * into the app instead of loading them as extension APKs. `upstream/` is a sparse checkout
 * of that repository (src/es, lib, lib-multisrc). This module provides the real runtime for
 * the `extensions-lib` API the sources are written against (network, JS engine, models).
 */
plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.serialization)
}
val upstream = layout.projectDirectory.dir("upstream")
val languages = listOf("es") // add "en", "all"… here to compile more sources

/** Sources to skip (folder names under upstream/src/<lang>) if one of them stops compiling. */
val excludedSources = setOf<String>()

fun sourceDirs(): List<File> = languages.flatMap { lang ->
	upstream.dir("src/$lang").asFile.listFiles()
		.orEmpty()
		.filter { it.isDirectory && it.name !in excludedSources && File(it, "build.gradle").exists() }
}

fun libDirs(): List<File> = upstream.dir("lib").asFile.listFiles().orEmpty().filter { it.isDirectory }
fun multisrcDirs(): List<File> = upstream.dir("lib-multisrc").asFile.listFiles().orEmpty().filter { it.isDirectory }

val generatedDir = layout.buildDirectory.dir("generated/sourceRegistry/kotlin")

/** Files that clash with the shared libs when everything is compiled into one module (deleted before compiling). */
val duplicatedUpstreamFiles = listOf(
	"src/es/katanime/src/eu/kanade/tachiyomi/animeextension/es/katanime/CryptoAES.kt",
)
android {
	namespace = "com.watanuki.sources"
	compileSdk = 36

	defaultConfig {
		minSdk = 26
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
		isCoreLibraryDesugaringEnabled = true
	}

	kotlinOptions {
		jvmTarget = "17"
		freeCompilerArgs += listOf(
			"-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
			"-Xskip-metadata-version-check",
		)
	}

	sourceSets {
		getByName("main") {
			java.srcDir("src/main/kotlin")
			java.srcDir(generatedDir.get().asFile)
			// every vendored source, shared extractor and multisrc theme becomes part of this module
			sourceDirs().forEach { java.srcDir(File(it, "src")) }
			libDirs().forEach { java.srcDir(File(it, "src/main/java")) }
			multisrcDirs().forEach { java.srcDir(File(it, "src")) }
			// a couple of extractors ship WebAssembly / JS blobs as assets
			libDirs().forEach { lib ->
				val assets = File(lib, "src/main/assets")
				if (assets.isDirectory) this.assets.srcDir(assets)
			}
		}
	}
	buildFeatures {
		buildConfig = false
	}

	lint {
		abortOnError = false
		checkReleaseBuilds = false
	}
}

/**
 * Generates `GeneratedSources.kt`: one entry per vendored source with the fully-qualified
 * class of its `AnimeSource` / `AnimeSourceFactory`, read from the upstream `build.gradle`.
 */
val generateSourceRegistry by tasks.registering {
	// capture plain values: the configuration cache cannot serialize script references
	val buildFiles = sourceDirs().map { File(it, "build.gradle") }
	val duplicates = duplicatedUpstreamFiles.map { File(upstream.asFile, it) }
	val outFile = generatedDir.get().file("com/watanuki/sources/GeneratedSources.kt").asFile
	inputs.files(buildFiles)
	outputs.dir(generatedDir)
	doLast {
		// upstream duplicates: some sources vendor a copy of a shared lib under the same package
		duplicates.forEach { it.delete() }
		val entries = buildFiles.mapNotNull { gradleFile ->
			val dir = gradleFile.parentFile
			val gradle = gradleFile.readText()
			val extClass = Regex("""extClass\s*=\s*'([^']+)'""").find(gradle)?.groupValues?.get(1) ?: return@mapNotNull null
			val extName = Regex("""extName\s*=\s*'([^']+)'""").find(gradle)?.groupValues?.get(1) ?: dir.name
			val nsfw = Regex("""isNsfw\s*=\s*true""").containsMatchIn(gradle)
			val lang = dir.parentFile.name
			val pkg = "eu.kanade.tachiyomi.animeextension.$lang.${dir.name}"
			val fqcn = if (extClass.startsWith(".")) pkg + extClass else extClass
			listOf(extName, lang, fqcn, nsfw.toString())
		}
		outFile.parentFile.mkdirs()
		outFile.writeText(buildString {
			appendLine("package com.watanuki.sources")
			appendLine()
			appendLine("/** Generated from upstream/src/<lang>/<source>/build.gradle. Do not edit. */")
			appendLine("internal object GeneratedSources {")
			appendLine("	val entries: List<SourceEntry> = listOf(")
			entries.sortedBy { it[0].lowercase() }.forEach { (name, lang, fqcn, nsfw) ->
				appendLine("		SourceEntry(name = \"${name.replace("\"", "\\\"")}\", lang = \"$lang\", className = \"$fqcn\", isNsfw = $nsfw),")
			}
			appendLine("	)")
			appendLine("}")
		})
		println("Source registry: ${entries.size} sources")
	}
}

tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach {
	dependsOn(generateSourceRegistry)
}
tasks.matching { it.name.startsWith("compile") && it.name.contains("Java") }.configureEach {
	dependsOn(generateSourceRegistry)
}

dependencies {
	coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

	api(libs.kotlin.stdlib)
	api(libs.coroutines.core)
	api(libs.coroutines.android)
	api(libs.serialization.json)
	api(libs.serialization.json.okio)
	api(libs.serialization.protobuf)
	api(libs.okhttp)
	implementation(libs.okhttp.dnsoverhttps)
	implementation(libs.okhttp.brotli)
	implementation(libs.okhttp.logging)
	api(libs.jsoup)
	api(libs.rxjava)
	api(libs.injekt)
	implementation(libs.quickjs)
	implementation(libs.jsunpacker) {
		exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
	}
	implementation(libs.chicory.runtime)
	implementation(libs.chicory.wasm)
	implementation(libs.nanohttpd)
	implementation(libs.rhino)
	api(libs.androidx.preference)
	implementation(libs.androidx.core)
	implementation(libs.androidx.webkit)
}
