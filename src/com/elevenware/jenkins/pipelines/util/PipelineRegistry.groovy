package com.elevenware.jenkins.pipelines.util

import com.elevenware.jenkins.pipelines.definitions.GithubPipelineDefinition
import com.elevenware.jenkins.pipelines.definitions.SimplePipelineDefinition
import com.elevenware.jenkins.pipelines.definitions.ChefPipelineDefinition

class PipelineRegistry implements Serializable {

    private static PipelineRegistry INSTANCE = new PipelineRegistry()

    private registry = [:]

    private PipelineRegistry() {
        registry['simplePipeline'] = SimplePipelineDefinition
        registry['githubflow'] = GithubPipelineDefinition
        registry['chefflow'] = ChefPipelineDefinition
    }

    static PipelineRegistry getInstance() {
        INSTANCE
    }

    def create(String name) {
        if(!registry.containsKey(name)) {
            throw new RuntimeException("Cannot find pipeline type '${name}' in '${registry.keySet().join(' ')}'")
        }
        Class pipelineClass = registry[name]
        pipelineClass.newInstance()
    }

}
