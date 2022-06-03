import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.7.0"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
	id("com.google.protobuf") version "0.8.18"
	id("io.gitlab.arturbosch.detekt") version "1.19.0"
}

group = "com.munoon.heartbeatlive.server"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
	implementation("org.springframework.boot:spring-boot-starter-graphql")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

	implementation("com.google.firebase:firebase-admin:8.1.0")
	implementation("com.google.protobuf:protobuf-kotlin:3.20.1")

	implementation("org.cache2k:cache2k-api:2.6.1.Final")
	implementation("org.cache2k:cache2k-addon:2.6.1.Final")
	runtimeOnly("org.cache2k:cache2k-core:2.6.1.Final")

	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(module = "mockito-core")
	}
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.springframework.graphql:spring-graphql-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("com.ninja-squad:springmockk:3.1.1")
	testImplementation("org.testcontainers:testcontainers:1.17.2")
	testImplementation("org.testcontainers:junit-jupiter:1.17.1")
}

sourceSets {
	main {
		proto {
			srcDir("src/main/protobuf")
		}
		java {
			srcDir("build/generated/source/proto/main/java")
		}
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
		allWarningsAsErrors = true
	}
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:3.6.1"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

detekt {
	buildUponDefaultConfig = true
	allRules = false
	config = files("$projectDir/detekt.yml")
}

tasks.withType<Detekt>().configureEach {
	jvmTarget = "17"
	reports {
		html.required.set(true)
		sarif.required.set(true)
	}
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
	jvmTarget = "17"
}