import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "com.evincism"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")

    // google docs
    implementation("com.google.api-client:google-api-client:1.31.1")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220709-1.32.1")
    implementation("com.google.apis:google-api-services-docs:v1-rev20220609-1.32.1")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20220620-1.32.1")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.8.1")


    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}