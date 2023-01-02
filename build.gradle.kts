import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "2.7.3" apply false
    id("io.spring.dependency-management") version "1.0.13.RELEASE" apply false
    kotlin("jvm") version "1.7.10" apply false
    kotlin("plugin.spring") version "1.7.10" apply false
    kotlin("plugin.jpa") version "1.7.10" apply false
}

group = "es.unizar"
version = "0.2022.1-SNAPSHOT"

var mockitoVersion = "4.0.0"
var bootstrapVersion = "3.4.0"
var jqueryVersion = "3.6.1"
var guavaVersion = "31.1-jre"
var commonsValidatorVersion = "1.6"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
    }
    repositories {
        mavenCentral()
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
        }
    }
    tasks.withType<Test> {
        useJUnitPlatform()
        this.testLogging {
            this.showStandardStreams = true
        }
    }
    dependencies {
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        "implementation"("io.github.g0dkar:qrcode-kotlin-jvm:3.2.0")

        "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:latest.release")
        "implementation"("org.springframework:spring-core:5.3.22")
        "implementation"("org.springframework:spring-web:5.3.22")
        "implementation"("io.github.cdimascio:dotenv-kotlin:6.4.0")
    }
}

project(":core") { }

project(":repositories") {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    dependencies {
        "implementation"(project(":core"))
        "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")

        "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:latest.release")
    }
    tasks.getByName<BootJar>("bootJar") {
        enabled = false
    }
}

project(":delivery") {
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    dependencies {
        "implementation"(project(":core"))
        "implementation"("org.springframework.boot:spring-boot-starter-amqp")
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        // "implementation"("org.springframework.boot:spring-boot-starter-webflux")
        "implementation"("org.springframework.boot:spring-boot-starter-hateoas")
        "implementation"("org.springdoc:springdoc-openapi-data-rest:1.6.0")
        "implementation"("org.springdoc:springdoc-openapi-ui:1.6.4")
        "implementation"("org.springdoc:springdoc-openapi-kotlin:1.6.0")
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "implementation"("commons-validator:commons-validator:$commonsValidatorVersion")
        "implementation"("com.google.guava:guava:$guavaVersion")
        "implementation"("com.google.apis:google-api-services-safebrowsing:v4-rev123-1.25.0")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:$mockitoVersion")

        "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:latest.release")
    }
    tasks.getByName<BootJar>("bootJar") {
        enabled = false
    }
}

project(":app") {
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-amqp")
        "implementation"(project(":core"))
        "implementation"(project(":delivery"))
        "implementation"(project(":repositories"))
        "implementation"("org.springframework.boot:spring-boot-starter")
        "implementation"("org.webjars:bootstrap:$bootstrapVersion")
        "implementation"("org.webjars:jquery:$jqueryVersion")

        "runtimeOnly"("org.hsqldb:hsqldb")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.springframework.boot:spring-boot-starter-web")
        // "testImplementation"("org.springframework.boot:spring-boot-starter-webflux")
        "testImplementation"("org.springframework.boot:spring-boot-starter-jdbc")
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:$mockitoVersion")
        "testImplementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "testImplementation"("org.apache.httpcomponents:httpclient")

        "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:latest.release")
    }
}
