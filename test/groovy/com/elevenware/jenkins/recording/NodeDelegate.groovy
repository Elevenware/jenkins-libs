package com.elevenware.jenkins.recording

import org.mockito.Mockito

class NodeDelegate {

    void stage(Closure closure) {
        closure.setDelegate(this)
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
        closure.call()
    }

    def methodMissing(String name, args) {
        DslStub.INSTANCE.invokeMethod(name, args)
    }

}
