package com.elevenware.jenkins.pipelines.platforms

import com.elevenware.jenkins.pipelines.PipelineContext

def build(PipelineContext context) {
    // for dryrun we should customize this step
    checkout scm

    // get commit information
    gitCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
    // short SHA, possibly better for chat notifications, etc.
    shortCommit = gitCommit.take(8)
    echo "commit - ${gitCommit}"
    echo "sha - ${shortCommit}"
    //    config['version'] = "${currentBuild.number}-${shortCommit}"

    //  berks will create Berksfile.lock during converge, but let it be until
    //  we dont have working kitchen on slaves
    sh "berks install"

    // copy RS config and append cookbook name
    sh "sed 's/test-kitchen-cdaas/test-kitchen-cdaas-${context.cookbookName}/' < ~/.kitchen/rackspace.yml > .kitchen.local.yml"

    // add platform specified settings to .kitchen.local file
    sh "printf '\nplatforms:\n' >> .kitchen.local.yml"
    sh "printf '  - name: centos-7\n' >> .kitchen.local.yml" //only if kitchen.yml have centos
//    sh "printf '  - name: debian-8\n' >> .kitchen.local.yml" //only if kitchen.yml have debian
}

def test(PipelineContext context) {
    echo "test ${context.appName}"
    echo "Unit tests"
    sh "chef exec rspec spec"

    echo "Linting test"

    // Exclude foodcritic fules during check
    sh "printf '~FC003\n' > .foodcritic" // deprecated for Chef > 12.11
    sh "printf '~FC064\n' >> .foodcritic" // ussues_url not used for us
    sh "printf '~FC065\n' > .foodcritic" // source_url not used for us

    foodcriticResult = sh(returnStdout: true, script: "foodcritic .").trim()
    if (foodcriticResult.contains("FC")) {
        error 'cookbook didnt pass foodcritic check'
    }

    // fails only on Errors and checks only linting rules
    sh "cookstyle --except Style/TrailingCommaInLiteral --display-cop-names -f s --fail-level E ."

    echo "kitchen test"
//    sh "kitchen test"
}

def publish(PipelineContext context) {
    echo "upload cookbook ${context.appName} to chef-server"
    knifeUploadResult = sh(returnStdout: true, script: "berks upload").trim()
    // stdout produce string "Skipping cookbookName (0.1.0) (frozen)" need check it below with regexp
    if (knifeUploadResult.contains("Skipping ${context.cookbookName}")) {
        error 'Cookbook is frozen. Version was not bumped.'
    }
}

def deploy(PipelineContext context) {
    // this method will be used in application cookbooks
    echo "pin version of cookbook on env"
    echo "run chef-client on role env"
}

def getVersion() {
    '1.0.1'
}
