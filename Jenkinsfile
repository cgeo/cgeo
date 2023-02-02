/*
 * cgeo Jenkins build script. This file is checked out again for every build, so it can be version controlled with the code.
 * This is work in progress and currently only used for the experimental pipeline builds but not for the default PR and commit builds (non-pipeline).
 */

// restrict to agents with an emulator
node('has-emulator') {

    // display timestamps in the console output
    timestamps {

        // make sure notifications are sent also for failing builds using try-finally for the inner build step
        try {
            // group steps by stages for better monitoring in the Jenkins pipeline stage view
            stage('Checkout from GitHub') {

                // set timeouts for every step instead of for the complete job. default unit are minutes
                timeout(5) {

                    // checkout the exact same version that triggered this pipeline script instead of specifying a repo and commit again
                    checkout scm
                }
            }

            stage('Prepare build environment') {
                timeout(5) {
                    // Restart the emulator to be sure of its state
                    sh '[ -x "$HOME"/restart-emulator.sh ] && "$HOME"/restart-emulator.sh'

                    // Ensure proper cleaning
                    sh './gradlew --no-daemon --scan  clean'
                    sh 'rm -fr build build-cache main/build || true'

                    // Accept Android licenses. these are the MD5 hashes of the license texts, so they may need to be adapted on license changes
                    // The following line does make the build fail, needs to be investigated
                    // sh '[ ! -d "/opt/android-sdk-linux/licenses" ] && mkdir "/opt/android-sdk-linux/licenses"'
                    sh 'echo -e "\n8933bad161af4178b1185d1a37fbf41ea5269c55\nd56f5187479451eabf01fb78af6dfcb131a6481e\n24333f8a63b6825ea9c5514f83c2829b004d1fee" > "/opt/android-sdk-linux/licenses/android-sdk-license"'
                    sh 'echo -e "\nd975f751698a77b662f1254ddbeed3901e976f5a" > "/opt/android-sdk-linux/licenses/intel-android-extra-license"'

                    // gradle needs the sdk.dir in local.properties. Not sure if a template exists on the server, therefore we create it on demand.
                    sh 'echo "sdk.dir=/opt/android-sdk-linux" > local.properties'

                    // get all the necessary keys
                    sh 'cp /srv/private.properties .'

                    // set the emulator preferences
                    sh 'cp /srv/cgeo.geocaching_preferences.xml main/cgeo.geocaching_preferences.xml'
                }
            }
            try {
                timeout(15) {
                    // Build sequentially instead of in one command as this made problems in the past
                    stage ('Run emulator tests') {
                        sh './gradlew --no-daemon --scan  testDebug -Pandroid.testInstrumentationRunnerArguments.notAnnotation=cgeo.geocaching.test.NotForIntegrationTests'
                    }
                    stage ('Run Checkstyle tests') {
                        sh './gradlew --no-daemon --scan  checkstyle'
                    }
                   stage ('Run Lint tests') {
                        sh './gradlew --no-daemon --scan  lintBasicDebug'
                    }
                }
            }
            finally {
                stage ('Cleanup environment') {
                    //shutdown emulator
                    sh '[ -x "$HOME"/restart-emulator.sh ] && "$HOME"/restart-emulator.sh --stop'

                    // file cleanup
                    sh '[ -f private.properties ] && rm private.properties'
                    sh '[ -f main/cgeo.geocaching_preferences.xml ] && rm main/cgeo.geocaching_preferences.xml'
                    sh '[ -f main/src/main/res/values/keys.xml ] && rm main/src/main/res/values/keys.xml'
                 }
             }

            stage('Collect results') {
                timeout(2) {
                    // archive the debug build
                    archive '*/build/outputs/apk/cgeo*debug.apk'

                    // publish JUnit results
                    junit '**/build/test-results/**/*.xml'

                    // publish checkstyle results
                    checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/reports/checkstyle/checkstyle.xml', unHealthy: ''

                    // publish Android Lint results
                    androidLint canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/lint-results-basicDebug.xml', unHealthy: ''

                    // show a pragmatical programmer tip
                    pragprog displayLanguageCode: 'en', indicateBuildResult: false
                }
            }
        }

        finally {
            // taken over from original script 2017, not yet checked whether it is working and whether it could be extended
            stage ('Verify results') {
                // check if emulator is out of date
                if (manager.logContains(".*Your emulator is out of date.*")) {
                    manager.addWarningBadge("Emulator out of date.")
                    manager.createSummary("warning.png").appendText('Emulator out of date, see <a href="./console">console</a>.', false, false, false, "red")
                }

                // present the gradle build scan link on the build summary page
                matcher = manager.getLogMatcher('.*(https://gradle\\S+).*')
                if (matcher != null) {
                    url = matcher.group(1)
                    matcher = null // groovy cannot serialize Matcher for saving pipeline state, therefore explicitly null it out
                    manager.createSummary("monitor.png").appendText('Gradle build scan result available at <a href="' + url + '">' + url + '</a>', false, false, false, "black")
                }
                matcher = null
            }
        }
    }
}
