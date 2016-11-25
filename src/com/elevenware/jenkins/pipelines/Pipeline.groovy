package com.elevenware.jenkins.pipelines

import com.cloudbees.groovy.cps.NonCPS
import com.elevenware.jenkins.pipelines.definitions.BasicDefinitions
import com.elevenware.jenkins.pipelines.deploy.DeployStrategy
import com.elevenware.jenkins.pipelines.elements.PipelineElement

abstract class Pipeline implements Serializable {

    private static Map TYPES = [github: GithubPipeline]

    protected Platform platform
    protected DeployStrategy deployStrategy

    Pipeline(Platform platform, DeployStrategy deployStrategy) {
        this.platform = platform
        this.deployStrategy = deployStrategy
    }

    @NonCPS
    void generate() {

        BasicDefinitions basicDefinitions = new BasicDefinitions()
        List elements = getElements()
        for(PipelineElement element: elements) {
            basicDefinitions.inStage("pre $element") {

            }
//            element.generate(this.platform)
        }

//        getElements().each { PipelineElement element ->
//            element.generate(this.platform)
//        }

    }

    @NonCPS
    abstract List getElements()

    static Class<Pipeline> forType(String type) {
        Class pipelineClass = TYPES[type]
        if(!pipelineClass) {
            throw new RuntimeException("No flow for '${type}' defined")
        }
        pipelineClass
    }

}
