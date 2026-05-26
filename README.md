# Wi De Ya Android App

Wi De Ya is an Android app for Sierra Leone school management and attendance monitoring, with the
primary user intended to be the school leader (or administrative head) of each school. The app aims
to help school leaders capture and manage information on classes, teachers, learners and their guardians
and report daily attendance at the school, using biometrics for teachers (fingerprint matching and photos).

The app interfaces with the [Wi De Ya Laravel Web App](https://github.com/CGATechnologies/wideya-laravel), 
which is the central system and database for user management, data synchronisation and data analysis.

This Android app can operate completely offline, and performs two-way synchronisation with the 
central system whenever network is available. Though the initial login and sync must of course be 
done online, since requires authenticating the user and syncing the appropriate data according to 
their school permissions.

Extra details beyond the scope of this ReadMe are available in the 
[GitHub Wiki that is associated with this repo](https://github.com/CGATechnologies/wideya-android/wiki).

Tickets are managed via a Trello Board (request access from a CGA Admin).

The app is written in the Kotlin programming language, and leverages many Android JetPack libraries 
amongst others.

# App Development & Rollout Phases
* 5-School Pilot (complete)
* 60-School Pilot (complete)
* 300-School (ongoing as at 2023-06)
* 1500-School (TBC)
* 4000-School (TBC)

# Compatible Devices

The app has been tested on Android 8, 10, 11 and 13, but is compatible with 
Android 6+ (API Level 23+).

To capture fingerprints, Secugen fingerprint scanners are supported.  The app has been tested with 
Secugen reader [Hamster Pro 20](https://secugen.com/products/hamster-pro-20/).

# Build Variants

Different build variants exist, relating to different servers they interface with, and small differences
in appearance and functionality.

The two production builds are:
* __Release:__ Has no tag and is the production version used by school leaders, interfacing 
with [wideya.org](https://wideya.org)
* __Demo:__ Has -demo tag and is used for training and app demonstrations.  Has been modified to be 
multi-tenant with a single universal login that downloads a pre-prepared dataset to simulate the 
setting up of a new school. This variant spoofs data uploads (so that the pre-prepared dataset is 
not affected centrally), but does still upload files to S3 for simulation. 
Interfaces with [demo.wideya.org](https://demo.wideya.org)

The various dev/testing builds are:
* __Staging:__ Has -staging tag and is identical to the production version but used for final testing,
interfacing with [staging.wideya.org](https://staging.wideya.org)
* __Load:__ Has -load tag and is used for testing large scale recordsets, 
interfacing with [load.wideya.org](https://load.wideya.org)
* __Dev:__ Has -dev tag and is the primary development build for the development team to test with, 
interfacing with [dev.wideya.org](https://dev.wideya.org)
* __Debug:__ Has -debug tag and is the debug build of the release, 
interfacing with [wideya.org](https://wideya.org). This build is rarely used since `dev` build is 
favoured due to it being sandboxed entirely from the propuction environment.

For the production builds, there are a few architecture variants.  One for ARM64-only to minimise 
filesize (suitable for practically all mobile devices), but also an Universal build mainly for 
Windows emulator support.

# Licenses

License to use and redevelop this code are reserved to Teaching Service Commission Sierra Leone 
and CGA Technologies only.

Various existing packages are leveraged in the Wi De Ya Android App:

* Secugen SDK is closed-source JNI and can be obtained [using this form](https://secugen.com/request-free-software/)
* Highcharts is open-source commercial licensed software, see [license details](https://github.com/highcharts/highcharts/blob/master/license.txt)

* SQLCipher community version is open-source and covered by [BSD-style license](https://www.zetetic.net/sqlcipher/license/)
* AWS SDK is covered by [Apache 2.0](https://github.com/aws-amplify/aws-sdk-android/blob/main/LICENSE.txt)

* Android and Kotlin Libraries are licensed with [Apache 2.0](https://source.android.com/docs/setup/about/licenses)
* SourceAFIS is open-source and is used to encode fingerprints to templates, licensed with [Apache 2.0](https://github.com/robertvazan/sourceafis-java/blob/master/LICENSE)
* LocationManager wrapper is covered by [Apache 2.0](https://github.com/yayaa/LocationManager#license)
* AKPermission wrapper is covered by [Apache 2.0](https://github.com/li-yu/AKPermission/blob/master/LICENSE)
* Android Remote Stack Trace is covered by [MIT License](https://code.google.com/archive/p/android-remote-stacktrace/)
* Network Response Adapter wrapper is covered by [Apache 2.0](https://github.com/haroldadmin/NetworkResponseAdapter/blob/master/LICENSE)
* Java UUID Generator is covered by [Apache 2.0](https://mvnrepository.com/artifact/com.fasterxml.uuid/java-uuid-generator)
* Hidden Secrets for Gradle is covered by [MIT License](https://github.com/klaxit/hidden-secrets-gradle-plugin/blob/master/LICENSE)
