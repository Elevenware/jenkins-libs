package com.elevenware.jenkins.pipelines.platforms

import com.elevenware.jenkins.pipelines.PipelineContext

def build(PipelineContext context) {
    echo "building ${context.appName} on simple platform"
}

def getVersion() {

    "1.0.1"

}