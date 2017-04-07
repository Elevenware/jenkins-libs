package com.elevenware.jenkins.recording

import com.elevenware.jenkins.pipelines.helpers.ChefSteps
import com.elevenware.jenkins.pipelines.util.PlatformRegistry
import com.elevenware.jenkins.recording.dsl.CodeBlock
import com.elevenware.jenkins.recording.dsl.DslDelegate
import com.elevenware.jenkins.recording.dsl.PipelineRecording
import org.codehaus.groovy.control.CompilerConfiguration

class DslTestHelper {

    static def testableScript(Class scriptClass) {
        def script = scriptClass.newInstance()
        script.metaClass {
            mixin DslDelegate
        }
        return script
    }

    static CodeBlock testableSnippet(Map args = [:], Closure closure) {
        DslDelegate delegate = new DslDelegate()
        closure.setDelegate(delegate)
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
        closure.call()
        delegate.recording.defaultStage().codeBlock
    }

    static def testableJenkinsfile(String pathToJenkinsFile) {
        String jenkinsFile = new File("test/resources/$pathToJenkinsFile").text

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration()
        compilerConfiguration.scriptBaseClass = DelegatingScript.canonicalName

        def shell = new GroovyShell(getClass().classLoader, new Binding(), compilerConfiguration)

        DelegatingScript script = shell.parse(jenkinsFile)
        def delegate = new JenkinsfileDelegate()
        script.setDelegate delegate
        script.run()
        delegate
    }

    static def testableJenkinsfileClosure(PipelineRecording recording = null, Closure closure) {
        JenkinsfileDelegate delegate = new JenkinsfileDelegate()
        closure.setDelegate(delegate)
        closure.setResolveStrategy(Closure.DELEGATE_ONLY)
        closure.call()
        def platformName = delegate.context.platform
        def platform = PlatformRegistry.instance.create(platformName)
        wrapUp(platform, recording)
        delegate.context.setPlatformImplementation(platform)
        ChefSteps chefSteps = new ChefSteps()
        delegate.context.chefSteps = chefSteps
        wrapUp(chefSteps, recording)
        delegate
    }

    static void wrapUp(parent, PipelineRecording recording) {
        parent.metaClass {
            mixin DslDelegate
        }
        if(recording) {
            parent.setRecording(recording)
        }
    }


}
