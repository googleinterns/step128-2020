pipeline {
    stages {
    ...
        stage('Testing...') {
            steps {
                ...
            }
            post {
                success {
                    script {
                        // if we are in a PR
                        if (env.CHANGE_ID) {
                            publishCoverageGithub(filepath:'target/site/jacoco/jacoco.xml', coverageXmlType: 'jacoco', comparisonOption: [ value: 'optionFixedCoverage', fixedCoverage: '0.65' ], coverageRateType: 'Line')
                        }
                    }
                }
            }
        }
    ...
}
