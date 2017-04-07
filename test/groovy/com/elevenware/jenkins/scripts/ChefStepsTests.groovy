package com.elevenware.jenkins.scripts

import com.elevenware.jenkins.pipelines.PipelineContext
import com.elevenware.jenkins.pipelines.helpers.ChefSteps
import com.elevenware.jenkins.pipelines.helpers.KnifeCommands
import com.elevenware.jenkins.recording.dsl.DslStub
import com.elevenware.jenkins.recording.FailedStepException
import com.elevenware.jenkins.recording.dsl.PipelineRecording
import com.elevenware.jenkins.recording.dsl.StageModel
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

import static com.elevenware.jenkins.matchers.DslMatchers.hadInvocation
import static com.elevenware.jenkins.recording.CommonMocks.mockCurrentAppSpec
import static com.elevenware.jenkins.recording.DslTestHelper.testableScript
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.Matchers.not
import static org.junit.Assert.assertThat
import static org.mockito.Mockito.when

class ChefStepsTests {

    @Test( expected = FailedStepException)
    void environmentPinDoesNotHappenIfEnvironmentDoesntExist() {

       ChefSteps chefSteps = testableScript(ChefSteps)

        PipelineRecording recording = chefSteps.recording

        PipelineContext context = new PipelineContext()
        context.platformImplementation = new Object() { String getVersion() {
            '1.0.1-0894-1abbbbae'
        }}

        when(DslStub.INSTANCE.sh(KnifeCommands.checkEnvExists('integration'))).thenThrow(new FailedStepException("cannot proceed"))

        context.appName = 'foo-app'
        context.appVersion = '1.0.1-0894-1abbbbae'
        context.cookbookDir = 'site-cookbooks/tc-canary'

        chefSteps.environmentPin(context, 'integration')

        assertThat(recording.defaultStage(), hadInvocation("echo").withArgs("Pinning foo-app to version foo-app@1.0.1-0894-1abbbbae in environment integration"))
        assertThat(recording.defaultStage(), not(hadInvocation("echo").withArgs("Now let's pin")))

    }

    @Test
    void environmentPinDoesWorks() {

        ChefSteps chefSteps = testableScript(ChefSteps)

        PipelineRecording recording = chefSteps.recording

        PipelineContext context = new PipelineContext()
        context.platformImplementation = new Object() { String getVersion() {
            '1.0.1-0894-1abbbbae'
        }}

        context.appName = 'foo-app'
        context.appVersion = '1.0.1-0894-1abbbbae'
        context.cookbookDir = 'site-cookbooks/tc-canary'

        chefSteps.environmentPin(context, 'integration')

        StageModel stageModel = recording.defaultStage()

        assertThat(stageModel, hadInvocation("echo").withArgs("Pinning foo-app to version foo-app@1.0.1-0894-1abbbbae in environment integration"))
        assertThat(stageModel, hadInvocation("sh").withArgs(KnifeCommands.checkEnvExists('integration')))
        assertThat(stageModel, hadInvocation("sh").withArgs(KnifeCommands.pinEnvironment(context, 'integration')))

    }

    @Test
    void currentVersionWorks() {

       ChefSteps chefSteps = testableScript(ChefSteps)

       PipelineContext ctx = new PipelineContext()
       ctx.appName = 'foo-app'
       ctx.cookbookName = 'tc-foo'

       def currentVersion = chefSteps.grabCurrentVersion(ctx, 'integration')

       assertThat(currentVersion, equalTo("= 1.2.3"))

    }


    @Test
    void verifyPinSatisfiedWhenSuccessful() {



    }

    @Before
    void setup() {

        Mockito.reset(DslStub.INSTANCE)
        mockCurrentAppSpec('tc-foo', '= 1.2.3', 'integration')

    }


}
