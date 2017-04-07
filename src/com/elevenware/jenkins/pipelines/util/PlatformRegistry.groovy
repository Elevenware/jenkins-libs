package com.elevenware.jenkins.pipelines.util

import com.elevenware.jenkins.pipelines.platforms.DotNETPlatform
import com.elevenware.jenkins.pipelines.platforms.NodeJSPlatform
import com.elevenware.jenkins.pipelines.platforms.PHPPlatform
import com.elevenware.jenkins.pipelines.platforms.SimplePlatform
import com.elevenware.jenkins.pipelines.platforms.MavenPlatform
import com.elevenware.jenkins.pipelines.platforms.ChefPlatform

class PlatformRegistry implements Serializable {

    private static PlatformRegistry INSTANCE = new PlatformRegistry()

    private def registry = [:]

    private PlatformRegistry() {
        registry['simple'] = SimplePlatform
        registry['maven'] = MavenPlatform
        registry['nodejs'] = NodeJSPlatform
        registry['php'] = PHPPlatform
        registry['dotnet'] = DotNETPlatform
        registry['chef'] = ChefPlatform
    }

    static PlatformRegistry getInstance() {
        INSTANCE
    }

    def create(String name) {
        if(!registry.containsKey(name)) {
            throw new RuntimeException("Cannot find platform type '${name}' in '${registry.keySet().join(' ')}'")
        }
        Class platformClass = registry[name]
        platformClass.newInstance()
    }

}
