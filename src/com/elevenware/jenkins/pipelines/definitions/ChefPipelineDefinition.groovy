package com.elevenware.jenkins.pipelines.definitions

import com.elevenware.jenkins.pipelines.PipelineContext

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
        }
    }
}
