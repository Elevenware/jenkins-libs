package com.elevenware.jenkins.pipelines

import com.elevenware.jenkins.pipelines.elements.PipelineElement

abstract class Pipeline implements Serializable {

    private static Map TYPES = [github: GithubPipeline]

    @NonCPS
    void generate(steps) {

        getElements().each { PipelineElement element ->
            println "HELLO"
            element.generate(steps)
        }

    }

    @NonCPS
    abstract List getElements()

    static Pipeline forType(String type) {
        Class pipelineClass = TYPES[type]
        if(!pipelineClass) {
            throw new RuntimeException("No flow for '${type}' defined")
        }
        pipelineClass.newInstance()
    }

}
