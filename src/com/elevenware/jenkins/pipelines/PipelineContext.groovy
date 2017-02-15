package com.elevenware.jenkins.pipelines

import com.cloudbees.groovy.cps.NonCPS

class PipelineContext implements Serializable{

    String appName
    String role
    String platform
    String cookbookName
    String chefRepoUri
    String chefRepoCredentials
    String cookbookDir
    def platformImplementation

    @NonCPS
    void chefRepo(Closure chefClosure) {
        chefClosure.setDelegate(new ChefRepoDelegate(this))
        chefClosure.setResolveStrategy(Closure.DELEGATE_FIRST)
        chefClosure.call()
    }

    @NonCPS
    void dump(PrintStream out) {
        out.println "App Name: $appName"
        out.println "Role: $role"
        out.println "Platform: $platform"
        out.println "Cookbook: $cookbookName"
        out.flush()
    }

    private static class ChefRepoDelegate {

        private PipelineContext owner

        ChefRepoDelegate(PipelineContext owner) {
            this.owner = owner
        }

        void setUri(String uri) {
            owner.chefRepoUri = uri
        }

        void setCredentials(String credentials) {
            owner.chefRepoCredentials = credentials
        }

    }

}
