// Opt-in Gradle init script: route Maven Central through Google's mirror.
//
// For machines where repo.maven.apache.org answers 429 Too Many Requests (shared cloud
// runners, sandboxed containers). Nothing in the build uses it unless it is passed:
//
//   ./gradlew --init-script tools/maven-mirror.init.gradle.kts --no-configuration-cache test
//
// The same artifacts, byte for byte: the mirror is a copy of Maven Central. Google's own
// repository (google()) is untouched. Robolectric downloads android-all from Maven Central
// at test RUN time, outside Gradle's resolution, so its repository is redirected too.

val mirror = "https://maven-central.storage-download.googleapis.com/maven2/"

fun RepositoryHandler.useMirror() {
    all {
        if (this is MavenArtifactRepository && url.toString().startsWith("https://repo.maven.apache.org")) {
            setUrl(mirror)
        }
    }
}

settingsEvaluated {
    pluginManagement.repositories.useMirror()
    dependencyResolutionManagement.repositories.useMirror()
}

allprojects {
    buildscript.repositories.useMirror()
    repositories.useMirror()
    tasks.withType<Test>().configureEach { systemProperty("robolectric.dependency.repo.url", mirror) }
}
