package com.elevenware.jenkins.pipelines.platforms

import com.elevenware.jenkins.pipelines.PipelineContext
import com.elevenware.jenkins.pipelines.helpers.ChefSteps

def build(PipelineContext context) {
    echo "building ${context.appName}"
    sh "cd ${context.appName}"
    sh "cd ${context.appName} && dotnet restore"
    sh "cd ${context.appName} && dotnet publish --output ${workspace}/app --configuration Release"
    sh "cd ${context.appName} && dotnet pack --output ${workspace}/package --configuration Release"
}

def test(PipelineContext context) {
    echo "add tests for ${context.appName}"
}

def publish(PipelineContext context) {
    zip([
	  'zipFile': "${context.appName}-1.0.0.zip",
	 'archive': true,
	 'glob' : 'app/'
	])
}

def deploy(PipelineContext context, String env) {
    echo "deploy ${context.appName}"
    echo "Deploying ${appName} to $env"
    def chefUri = context.chefRepoUri
    def credentials = context.chefRepoCredentials
    def cookbookDir = context.cookbookDir
    git url: chefUri, credentialsId: credentials
    ChefSteps chefSteps = context.chefSteps
    chefSteps.installChefDependencies(context)
    chefSteps.environmentPin(context, env)
    chefSteps.runChefClient(context, env)
}

def getVersion() {
    "1.0.2"
}
