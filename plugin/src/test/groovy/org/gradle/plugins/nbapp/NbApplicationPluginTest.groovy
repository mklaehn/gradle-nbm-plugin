package org.gradle.plugins.nbapp

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import static org.junit.Assert.*

class NbApplicationPluginTest {
    
    @Test
    public void iggy() {
        Project project = ProjectBuilder.
            builder().
            build()
        project.ext.netbeansPlatform = '/Users/martin/_data/tools/netbeans_small'
        project.project.plugins.apply(NbApplicationPlugin)
        def task = project.tasks.runApplication
        try {
            task.execute()
        } catch (final Throwable t) {
            task.state.printStackTrace()
            task.state.rethrowFailure()
        }
    }
}
