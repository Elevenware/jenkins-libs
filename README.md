# jenkins-libs Documentation 

This project is an alternative approach to the jenkins-jobdsl project for providing configuration-as-code for Jenkins pipelines.
 
[confluence page](https://tc-jira.atlassian.net/wiki/display/SHIP/jenkins-libs)
 ## Preamble
 
 The motivation for this project is a desire to remove our dependence on an old, forked verion of the jobdsl plugin, which was holding us back from upgrading, well, anything really. 
 
 This project leverages the native Jenkins 2 pipeline DSL, which is likely to be the most well-supported approach since it is core Jenkins. 
 
 ## Concepts
 
 ### Job DSL vs Pipeline DSL
 
 Forget what you know about the Jenkins job DSL. This isn't it. Jenkins Job DSL is code that is run in a separate job to generate static configuration for other jobs. The pipeline DSL is not that, it is code that is run *as part of your pipeline*, and it's run every time the pipeline is invoked. This is important as it has an impact on testability. In fact, the biggest challenge from moving from Job DSL to the pipeline DSL is in writing testable code. Job DSL code was easy, we used a class from the DSL plugin to generate the static XML and compared it to a known control XML file. That isn't an option here, as the output of running pipeline DSL code is a pipeline run, not some XML.
 
 To sum up: Jenkins Job DSL is just an alternative way to generate config for some Jenkins jobs, the pipeline DSL is actual code which is executed as part of a pipeline itself.
 
### Repository config

Remember .jenkins.yml? It's gone. Replaced by a Jenkinsfile, which is Groovy code. It's possible to define an entire pipeline inside this file. However, this isn't scalable, and it isn't what we're aiming to do. Instead, this file will be used in conjunction with shared libraries.

### Shared libraries

Shared libraries are repositories of Groovy code of which Jenkins is aware, which provide functionality available to support Jenkinsfiles. This project *is* a shared library. It aims to provide reusable, congigurable pipeline definitions which will be configured by Jenkinsfiles. 

A word on coding style; jenkins-jobdsl had a heady mix of procedural and object-oriented code which generated pipeline DSL code in a number of ways. This was fairly complex and contained a lot of abstractions. Early attempts to mimic this level of abstraction in pipeline DSL failed, because of the underlying paradigm of the pipeline DSL. In short, pipeline DSL code is pumped through a CPS parser, which means it must be written in a particular style, or code must be marked with a @NonCPS annotation to avoid this. This quickly got out of hand and I found that most of the code was being polluted with this annotation. I've dropped this style in favour of having simpler pipelines defined in scripts, and emphasising ways of making these testable. So a large chunk of this libary is aimed at mocking out the underlying mechanism for running these scripts.

### Github Organisations

Another great departure from our old way of working is support for Jenkins to observe entire GitHub organisations. The practical upshot of this is that we don't need to explicitly add new repositories to any configuration like projects.txt. Jenkins simply receives notification of a new repository being added to the organisation, and if it has a Jenkinsfile in it, executes it.

## Simple pipeline tutorial

Let's build a simple pipeline, with tests. Our pipeline will consist of two stages, a build and a deploy. Nothing too complex. The pipeline definition is in the source folder, and test suite is in the package 'com.elevenware.jenkins.demo' in the test folder.

First, we want a test to ensure that both of these stages exist in the correct order. Write a test that looks like this.

```groovy

import static testableScript
import com.elevenware.jenkins.recording.dsl.PipelineRecording
import com.elevenware.jenkins.pipelines.PipelineContext

    @Test
    void correctStagesExist() {

        String appName = 'Foo Application'
        PipelineContext ctx = new PipelineContext("")
        ctx.appName = appName

        SimplePipelineDefinition pipeline = testableScript(SimplePipelineDefinition)
        pipeline.run(ctx)

        PipelineRecording recording = pipeline.recording

        assertEquals(2, recording.stages.size())

        Iterator iter = recording.stages.entrySet().iterator()

        assertThat(iter.next().value.name, isString("build"))
        assertThat(iter.next().value.name, isString("deploy"))

    }
```

The *testableScript* method is a static import from the test framework provided in this library. It wraps a DSL script in some plumbing to allow us to mock out the DSL and perform assertions on it.

SimplePipelineDefintion is a Groovy *script* (not *class*) which defines our pipeline. The use of scripts rather than classes is a coding style imposed by the DSL plugins which apply a paradigm known as CPS (continuation passing style) to DSL code. It *is* possible to avoid, but some Groovy code is not legal in the CPS style, and this would need annotating as such, which pollutes the code to a huge degree and makes it unreadable. Simplicity being key, we ran with Groovy scripts.

PipelineRecording is a framework class which represents the interactions between our DSL code and our mocked DSL.

This test, of course, will not pass, as we have not yet written the pipeline definition. It's TDD, after all. Let's add the simple pipeline.

Quick note: we've used assertThat/isString, a utility provided by the framework, rather than mere isEqual as a workaround for a Groovy/Junit quirk: isEqual will fail if you try to compare a Java String to a Groovy GString, which is what we end up with if we use interpolation.

```groovy
def build(Map config) {
    stage('build') {
    }
    stage('deploy') {
    }
}
```

This is incredibly simple. The stages do not do anything, they merely echo what they *would* do. Fine for now. Our test should now pass.

Obviously, this is so simple it isn't worth even bothering to test. So let's add a little logic so that it *is*.

Stage names - build and deploy in this example - are actually displayed in the Jenkins UI. Since this pipeline, as with all of ours, are intended to be reusable across applications, let's [make it so](https://s-media-cache-ak0.pinimg.com/736x/dc/df/5b/dcdf5b42acb95fa3551faa23a2f1e9e4.jpg).

Change the test to look like this

```groovy
    @Test
    void correctStagesExist() {

        String appName = 'Foo Application'
        PipelineContext ctx = new PipelineContext("")
        ctx.appName = appName

        SimplePipelineDefinition pipeline = testableScript(SimplePipelineDefinition)
        pipeline.run(ctx)

        PipelineRecording recording = pipeline.recording

        assertEquals(2, recording.stages.size())

        Iterator iter = recording.stages.entrySet().iterator()

        assertThat(iter.next().value.name, isString("build Foo Application"))
        assertThat(iter.next().value.name, isString("deploy $appName"))

    }
```

See how we are passing an application name into our pipeline. The test fails, naturally. So TDD up boys, let's go green. Our script now looks like this

```groovy
def build(Map config) {
    String appName = config.appName
    stage("build $appName") {
    }
    stage("deploy $appName") {
    }
}
```

So we know that the correct stages exist. Now let's see if they're doing what they're supposed to. This is the real gravy of the test framework.

This test actually examines the interactions in the build stage. Currently, this wil be just to echo something to the effect that we are building the app.

```groovy

import static com.elevenware.jenkins.matchers.DslMatchers.hadInvocation

    @Test
    void buildStageActsAsExpected() {

        String appName = 'Foo Application'
        PipelineContext ctx = new PipelineContext("")
        ctx.appName = appName

        SimplePipelineDefinition pipeline = testableScript(SimplePipelineDefinition)

        pipeline.run(ctx)

        PipelineRecording recording = pipeline.recording

        StageModel buildStage = recording.getStage("build $appName")

        assertThat(buildStage.codeBlock, hadInvocation("echo", "Running build stage for $appName"))

    }
```

This introduces the *hadInvocation* method, another part of the test framework. The test, of course, fails, so we now implement the step.


```groovy
def build(Map config) {
    String appName = config.appName
    stage("build $appName") {
        echo "Running build stage for ${context.appName}"
    }
    stage("deploy $appName") {
    }
}
```

Simple. As an exercise, write a failing test for the deploy stage, then make it pass.

## Let us actually use this pipeline now

All well and good to have some passing tests. But how do we use this pipeline in a project? Well, that's where the Jenkinsfile comes in. This is similar to the .jenkins.yml file used in our legacy CD framework, although it is Groovy code rather than YAML. While it's possible to simply define a pipeline right in Jenkinsfile, as previously discussed our approach will be to use the file to configure a pre-defined pipeline contained in this library. Let's write a simple Jenkinsfile, with tests first, of course.

### Test for the Jenkinsfile

Here is a simple test for a Jenkinsfile which conforms to our proposed framework.

```groovy
    @Test
    void jenkinsFileConfiguresCorrectly() {

        JenkinsfileDelegate delegate = DslTestHelper.testableJenkinsfile("Jenkinsfile")

        PipelineContext ctx = delegate.context

        assertThat(delegate.pipelineDefinition, instanceOf(SimplePipelineDefinition))
        assertThat(ctx.appName, equalTo('basic-app'))
        assertThat(ctx.role, equalTo('basic'))
        assertThat(ctx.platform, equalTo('java'))
        assertThat(ctx.cookbookName, equalTo('tc-basic'))

    }
```

This loads a file from the test/resources folder called 'Jenkinsfile', wraps it up in a testable delegate and then performs some assertions on it. The Jenkinsfile in question looks like this:

```groovy
  runPipeline('simplePipeline') {
  appName = 'basic-app'
  role = 'basic'
  platform = 'java'
  cookbookName = 'tc-basic'
}
```

How this fits into a real Jenkins ecosystem is like this:

 * the Jenkinsfile lives in the root of a github project
 * runPipeline is the name of a Groovy script which lives in the *vars* folder of this library
 * the name 'simplePipeline' is mapped in the class PipelineRegistry to a Groovy script which is the definition of that pipeline
 * when the pipeline is run, the runPipeline script populates a PipelineContext object and passes it to the run() method of the pipeline definition, thus executing the pipeline

## The test framework

The test framework is still very much a work in progress. It will expand as needed. Part of the job of writing new pipelines and tests for them will often be to add mocking for new parts of the DSL.

### The basics of the framework explained

The test framework provides some classes which allow us to mock out the underlying Jenkins pipeline DSL, to record interactions between our scripts and the DSL, to perform assertions on those interactions and to stub out responses to various library calls. We can mock out the DSL for an entire script, as we have done in our previous examples, or we can mock out just a Jenkinsfile, again, as in our previous examples. Or we can mock out DSL closures right inside our tests, which can be useful when adding extensions to the mocks. Let's examine some simple cases. 

First of all, let's write a test for the *echo* component.

```groovy
    @Test
    void echoCallWithHardArgs() {

        def block = testableSnippet {
            echo "hello"
        }

        assertThat(block, hadInvocation("echo", "hello"))

    }
```

Here, we use the testableSnippet method, which accepts a closure as an argument, and wraps it in a CodeBlock instance which we can then perform assertions on. Here, we are simply asserting that the *echo* command was called, and passed the argument "hello". Under the covers, two things are happening here: we intercept *any* method called inside our DSL code, and record the name of the method and the arguments passed; we then pass the invocation on to a Mockito instance in case we want to stub out any responses. The initial plan was to use Mockito to record invocations as well, but this proved problematic when using nested closures, which are inevitable in all but the simplest of examples. A side effet of this is that any calls we want to mock **must** be present on an interface called DslStub. This serves as something for Mockito to proxy, and also as a check for typos. So any method calls from the real DSL we want to add to our mocked DSL **must** be present on that interface.

Let's mock out a response to the Jenkins library call *libraryResource*, which lets us look up text files in our library code.

```@Test
   void stubbingResponses() {
   
       when(DslStub.INSTANCE.libraryResource("foo")).thenReturn("bar")
   
       def block = testableSnippet {
           def foo = libraryResource('foo')
           echo foo
       }
   
       assertThat(block, hadInvocation("echo", 'bar'))
   
   }
 ```
   
This should be pretty self-explanatory. We tell the Dsl stub that when libraryResource is called with the argument 'foo', it should return the string 'bar'.

Earlier we mentioned nested closures. A lot of interactions in the DSL involve these. We can mock them out, and examine them, too.

```groovy
    @Test
    void closureCall() {

        def block = testableSnippet {
            withMaven {
                echo "A maven step"
            }
        }

        assertThat(block, hadInvocation("withMaven", isA(Closure)))
        Invocation withMaven = block.getInvocation("withMaven")

        block = testableSnippet(withMaven.args.first())

        assertThat(block, hadInvocation("echo", "A maven step"))

    }
```

Take a moment to digest what's going on here. We pass a closure, which in turn has another closure inside it. Then we pull that closure out, wrap it up and call assertions on it. 

We can also pass arguments inline, along with a closure. That's a very common pattern in the pipeline DSL. In fact, withMaven is a prime example.
