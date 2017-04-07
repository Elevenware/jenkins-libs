package com.elevenware.jenkins.pipelines.helpers

import com.cloudbees.groovy.cps.NonCPS
import com.elevenware.jenkins.pipelines.PipelineContext
import com.elevenware.jenkins.pipelines.helpers.KnifeCommands

def installChefDependencies(PipelineContext ctx) {
    echo'Installing cookbook dependencies'
    sh 'ls'
    dir("${ctx.cookbookDir}") {
        sh KnifeCommands.CommonShellCommands.GEM_INSTALL.code
    }
}

def environmentPin(PipelineContext ctx, String targetEnvironment) {
    echo "Pinning ${ctx.appName} to version ${ctx.appSpec} in environment ${targetEnvironment}"
    def metadata = String.format("%04d-%s", ctx.buildNumber, ctx.shortCommit)

    sh KnifeCommands.checkEnvExists(targetEnvironment)
    ctx.priorAppSpec = grabCurrentVersion(ctx, targetEnvironment)
    sh KnifeCommands.pinEnvironment(ctx, targetEnvironment)

}

def runChefClient(PipelineContext ctx, String targetEnvironment) {

    echo "Running Chef client on all nodes with role ${ctx.role} in environment ${targetEnvironment}"

    def nodesList = sh(returnStdout: true, script: KnifeCommands.lookUpNodes(ctx.role, targetEnvironment))

    def nodes = parseJson(nodesList)

    if(nodes.results == 0) {
        echo "No nodes with role ${ctx.role} exist in environment ${targetEnvironment} -  continuing"
        return
    }

    sh KnifeCommands.runChefClient(ctx.role, targetEnvironment)

}

def grabCurrentVersion(PipelineContext ctx, String targetEnvironment) {

    StringBuilder buf = new StringBuilder().append("bundle exec knife environment show ${targetEnvironment} ")
                .append("--attribute \"cookbook_versions.${ctx.cookbookName}\" --format json")

    def ret = sh(returnStdout: true, script: buf.toString())
    def data = parseJson(ret)
    def currentVersion = data[targetEnvironment]["cookbook_versions.${ctx.cookbookName}"]
    echo "current version is '${currentVersion}'"

    return currentVersion

}

@NonCPS
def parseJson(String text) {
    new groovy.json.JsonSlurperClassic().parseText(text)
}