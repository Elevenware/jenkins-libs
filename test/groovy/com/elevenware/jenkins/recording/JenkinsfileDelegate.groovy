package com.elevenware.jenkins.recording

import com.elevenware.jenkins.pipelines.PipelineContext
import com.elevenware.jenkins.pipelines.definitions.PipelineRegistry

class JenkinsfileDelegate {

    PipelineContext context
    def pipelineDefinition

    def runPipeline(String pipelineName, Closure body) {
        pipelineDefinition = PipelineRegistry.instance.create(pipelineName)

        context = new PipelineContext(pipelineName)
        body.delegate = context
        body.resolveStrategy = Closure.DELEGATE_ONLY
        body()
    }

}