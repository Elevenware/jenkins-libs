package com.elevenware.jenkins.matchers

import com.elevenware.jenkins.recording.dsl.CodeBlock
import com.elevenware.jenkins.recording.dsl.DslMethodInvocationHandler
import com.elevenware.jenkins.recording.dsl.StageModel
import org.hamcrest.BaseDescription
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.junit.Test

import static com.elevenware.jenkins.matchers.DslMatchers.captureTo
import static com.elevenware.jenkins.matchers.DslMatchers.hadInvocation
import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertFalse

class HadInvocationMatcherTests {

    @Test
    void explicitAboutStageNotExisting() {

        HadInvocationMatcher matcher = new HadInvocationMatcher("foo")

        StringBuilder buf = new StringBuilder()

        assertFalse(matcher.matches(null))

        Description description = new BaseDescription() {
            @Override
            protected void append(char c) {
                buf.append c
            }
        }

        matcher.describeMismatch(null, description)
        assertThat(buf.readLines().get(0), equalTo("No CodeBlock or StageModel was passed at all"))

    }

    @Test
    void hadInvocationFailsToMatchIfNoMatchExists() {

        CodeBlock model = new CodeBlock()

        assertThat(model, not(hadInvocation("foo")))

    }

    @Test
    void failureProvidesCorrectExpectedValue() {

        CodeBlock model = new CodeBlock()

        StringBuilder buf = new StringBuilder()

        HadInvocationMatcher matcher = new HadInvocationMatcher("foo").withArgs("bar", "baz")

        assertFalse(matcher.matches(model))

        Description description = new BaseDescription() {
            @Override
            protected void append(char c) {
                buf.append c
            }
        }

        matcher.describeTo(description)
        assertThat(buf.toString(), equalTo("an invocation of 'foo' with arguments [bar, baz]"))

    }

    @Test
    void failureProvidesCorrectActualValue() {

        CodeBlock model = new CodeBlock()

        StringBuilder buf = new StringBuilder()

        HadInvocationMatcher matcher = new HadInvocationMatcher("foo")

        matcher.matches(model)

        Description description = new BaseDescription() {
            @Override
            protected void append(char c) {
                buf.append c
            }
        }

        matcher.describeMismatch(model, description)

        assertThat(buf.toString(), equalTo("foo was not found"))

    }

    @Test
    void commandButWrongArgs() {

        CodeBlock model = new CodeBlock()

        model.foo("baz")

        StringBuilder buf = new StringBuilder()

        HadInvocationMatcher matcher = new HadInvocationMatcher("foo").withArgs("bar")

        matcher.matches(model)

        Description description = new BaseDescription() {
            @Override
            protected void append(char c) {
                buf.append c
            }
        }

        matcher.describeMismatch(model, description)

        def errorMessage = buf.readLines()

        assertThat(errorMessage.get(0), equalTo("foo was found, but not with the arguments [bar]"))
        assertThat(errorMessage.get(1), equalTo("Did you mean: foo [baz]?"))

    }

    @Test
    void hadInvocationPassesIfMatchExists() {

        CodeBlock model = new CodeBlock()
        model.foo()

        assertThat(model, hadInvocation("foo"))

    }

    @Test
    void hadInvocationPassessIfArgsNotRequested() {

        CodeBlock model = new CodeBlock()
        model.foo("bar", "baz")

        assertThat(model, hadInvocation("foo"))

    }

    @Test
    void hadInvocationPassessIfArgsMactch() {

        CodeBlock model = new CodeBlock()
        model.foo("bar", "baz")

        assertThat(model, hadInvocation("foo").withArgs("bar", "baz"))

    }

    @Test
    void hadInvocationFailssIfArgsDontMactch() {

        CodeBlock model = new CodeBlock()
        model.foo("bar", "baz")

        assertThat(model, not(hadInvocation("foo").withArgs("bar", "wibble")))

    }

    @Test
    void hadInvocationPassesWithWildcard() {

        CodeBlock model = new CodeBlock()
        model.foo("bar", "baz")

        assertThat(model, hadInvocation("foo").withArgs("bar", isA(String)))

    }

    @Test
    void multipleInvocations() {

        CodeBlock model = new CodeBlock()
        model.foo("bar")
        model.foo("baz")

        assertThat(model, hadInvocation("foo").withArgs("bar"))
        assertThat(model, hadInvocation("foo").withArgs("baz"))

    }

    @Test
    void hadInvocationAcceptsStageModel() {

        StageModel stageModel = new StageModel("stage", DslMethodInvocationHandler.forDsl())
        CodeBlock codeBlock = stageModel.codeBlock
        codeBlock.foo()

        assertThat(stageModel, hadInvocation("foo"))

    }

    @Test
    void capturingArguments() {

        CodeBlock block = new CodeBlock()
        block.doSomething(arg1: 'foo', arg2: 'bar')

        ArgumentCapture capture = new ArgumentCapture(Map)
        assertThat(block, hadInvocation("doSomething").withArgs(captureTo(capture)))

        Map args = capture.value()

        assertThat(args['arg1'], equalTo('foo'))
        assertThat(args['arg2'], equalTo('bar'))

    }

    @Test
    void notSpecifyingTimesWorks() {

        CodeBlock model = new CodeBlock()
        model.foo()
        model.foo()
        model.foo()

        assertThat(model, hadInvocation("foo"))

    }

}
