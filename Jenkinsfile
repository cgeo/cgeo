/*
 * cgeo Jenkins build script. This file is checked out again for every build, so it can be version controlled with the code.
 */

// restrict to agents with an emulator
node('has-emulator') {

    // display timestamps in the console output
    timestamps {

        // make sure notifications are sent also for failing builds using try-finally for the inner build step
        try {

            // group steps by stages for better monitoring in the Jenkins pipeline stage view
            stage('Checkout') {

                // set timeouts for every step instead of for the complete job. default unit are minutes
                timeout(10) {

                    // checkout the exact same version that triggered this pipeline script instead of specifying a repo and commit again
                    checkout scm
                }
            }

            stage('Build') {
                timeout(15) {
                    try {
                        // restart the emulator to be sure of its state
                        sh '[ -x "$HOME"/restart-emulator.sh ] && "$HOME"/restart-emulator.sh'

                        // accept Android licenses. these are the MD5 hashes of the license texts, so they may need to be adapted on license changes
                        sh '[ ! -d "/opt/android-sdk-linux/licenses" ] && mkdir "/opt/android-sdk-linux/licenses"'
                        sh 'echo -e "\\n8933bad161af4178b1185d1a37fbf41ea5269c55" > "/opt/android-sdk-linux/licenses/android-sdk-license"'

                        // gradle needs the sdk.dir in local.properties. Not sure if a template exists on the server, therefore we create it on demand.
                        sh 'echo "sdk.dir=/opt/android-sdk-linux" > local.properties'

                        // get all the necessary keys
                        sh 'cp /srv/private.properties .'

                        // set the emulator preferences
                        sh 'cp /srv/cgeo.geocaching_preferences.xml main/cgeo.geocaching_preferences.xml'

                        // build everything
                        // re-added the clean target, since builds fail after upgrading the unmock or java plugin of gradle
                        sh './gradlew --no-daemon --stacktrace clean testDebug createBasicDebugCoverageReport checkstyle lintBasicDebug -Pandroid.testInstrumentationRunnerArguments.notAnnotation=cgeo.geocaching.test.NotForIntegrationTests --scan || false'

                        // shutdown emulator to minimize memory usage on the agent
                        sh '[ -x "$HOME"/restart-emulator.sh ] && "$HOME"/restart-emulator.sh --stop'
                    }
                    finally {
                        // cleanup
                        sh '[ -f private.properties ] && rm private.properties'
                        sh '[ -f main/cgeo.geocaching_preferences.xml ] && rm main/cgeo.geocaching_preferences.xml'
                        sh '[ -f main/res/values/keys.xml ] && rm main/res/values/keys.xml'
                    }
                }
            }

            stage('Results') {
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
            stage ('Notify') {
                // there is no pipeline version of ircNotify yet. therefore use a shell script and a non persistent connection to just post the build result in the channel
                sh "MSG='" + env.JOB_NAME + ' ' + currentBuild.displayName + ": " + currentBuild.currentResult + "'" +
                   '''
                    SERVER=irc.freenode.net
                    CHANNEL=#cgeo
                    USER=cgeo-ci2

                    (
                    echo NICK $USER
                    echo USER $USER 8 * : $USER
                    sleep 5
                    #echo PASS $USER:$MYPASSWORD
                    echo "NOTICE $CHANNEL" :$MSG
                    echo QUIT
                    ) | nc $SERVER 6667

                '''
            }

            stage ('Verification') {
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
