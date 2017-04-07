package com.elevenware.jenkins.pipelines.definitions

import com.elevenware.jenkins.pipelines.PipelineContext
import com.elevenware.jenkins.pipelines.helpers.ChefSteps

def run(PipelineContext context) {
    String appName = context.appName
    def platform = context.platformImplementation
    node {
        stage("Build \n$appName") {
            platform.build(context)
        }

        stage("Test \n$appName") {
            platform.test(context)
        }

        if (!context.pr) {
            stage("Publish \n$appName") {
                platform.publish(context)
            }
            deploy(context, "integration")
            deploy(context, "qa", true)
            deploy(context, "staging", true)
            deploy(context, "production", true)
        }
    }

}


def deploy(PipelineContext context, String env, boolean manuallyPromote = false) {
    def appName = context.appName

    stage("Promote ${appName} to ${env}") {
        if (manuallyPromote) {
            def userInput = input(
                    id: 'userInput', message: 'Let\'s promote?', parameters: [
                    [$class: 'TextParameterDefinition', defaultValue: 'uat', description: 'Environment', name: 'env'],
                    [$gclass: 'TextParameterDefinition', defaultValue: 'uat1', description: 'Target', name: 'target']
            ])
        }
        platform.deploy(context, env)
    }

}

