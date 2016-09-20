package org.gradle.plugins.nbapp

import groovy.transform.Memoized
import java.nio.file.Path
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class NbHomeTask extends DefaultTask {
    
    @OutputDirectory
    File platformDir
    File platformUserDir
    
    void init(Project project) {
        platformDir = project.file("${project.buildDir}/nbplatform/netbeans")
        platformUserDir = project.file("${project.buildDir}/nbplatform/userdir")
    }
    
    File resolveSingleDependency(dependency) {
	return rootProject.
            configurations.
            detachedConfiguration(rootProject.dependencies.create(dependency)).
            resolvedConfiguration.
            firstLevelModuleDependencies.
            each{println it}.
            first().
            moduleArtifacts.
            each{println it}.
            first().
            file
    }

    @TaskAction
    void create() {
        def nbPlatform
        
        if (project.hasProperty('netbeansPlatform')) {
            nbPlatform = project.netbeansPlatform
        } else if (project.ext.hasProperty('netbeansPlatform')) {
            nbPlatform = project.ext.netbeansPlatform
        } else {
            println project.name
            throw new IllegalStateException('The property netBeansExecutable is not specified, you should define it in ~/.gradle/gradle.properties or define in project.ext.netBeansExecutable')
        }
        
        if (nbPlatform instanceof String) {
            nbPlatform = project.file(nbPlatform)
        }
        
        project.copy {
            into platformDir
            
            if (nbPlatform.isFile()) {
                from project.zipTree(nbPlatform)
            } else {
                from project.fileTree(nbPlatform)
            }
        }
        
        if (project.logger.isEnabled(org.gradle.api.logging.LogLevel.DEBUG)) {
            project.fileTree(platformDir).
                files.
                collect{it.path.substring(1 + platformDir.path.length())}.
                sort().
                each{project.logger.debug it}
        }
    }
    
    @Memoized(maxCacheSize = 2)
    List<String> getLauncherArgs(clusters, final boolean debug = false) {
        if (!didWork) {
            execute()
        }
        
        String executable = "${platformDir}/bin/netbeans"
        
        if (System.getProperty('os.name').toLowerCase().contains('windows')) {
            if (System.getProperty('os.arch').contains('64')) {
                executable += '64'
            }
            
            executable += '.exe'
        }
        
        def result = [executable, '--userdir', platformUserDir.path, '--clusters', clusters.asPath]
        
        if (debug) {
            result << '-J-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5009'
        }
        
        return result
    }
    
    @Memoized(maxCacheSize = 2)
    List<String> getClusterNames(final boolean withPlatformCluster = false) {
        if (!didWork) {
            execute()
        }
        
        def clusterNames = new File("${platformDir}/etc/netbeans.clusters").
            readLines().
            collect{it.trim()}.
            findAll{!(it.isEmpty() || it.startsWith('#'))}.
            findAll{project.file("${platformDir}/${it}").exists()}
        
        return withPlatformCluster ? clusterNames : clusterNames - 'platform'
    }
    
    @Memoized(maxCacheSize = 2)
    FileCollection getClusterPaths(final boolean withPlatformCluster = false) {
        if (!didWork) {
            execute()
        }
        
        return project.
            files(getClusterNames(withPlatformCluster).collect{"${platformDir}/${it}"})
    }
    
    @Memoized(maxCacheSize = 2)
    Collection<Project> getNetbeansModuleProjects(final boolean ide = true) {
        if (!didWork) {
            execute()
        }

        return project.
            getConfigurations().
            getByName('runtime').
            getAllDependencies().
            withType(ProjectDependency).
            findAll{ide ? true : it.getDependencyProject().getName() =~ project.getName() + '-' + /(suite|branding)$/}
        
    }
    
    @Memoized(maxCacheSize = 2)
    FileCollection getNetbeansModulePaths(final boolean ide = true) {
        if (!didWork) {
            execute()
        }
        
        return project.
            files(getNetbeansModuleProjects(ide).collect{"${it.buildDir}/module"})
    }
}
