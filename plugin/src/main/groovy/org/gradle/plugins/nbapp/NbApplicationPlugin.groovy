package org.gradle.plugins.nbapp

import groovy.transform.Memoized
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip

class NbApplicationPlugin implements Plugin<Project> {
    def prefix
    
    void apply(final Project project) {
        project.plugins.apply 'java'
        project.ext.clusterName = 'my-cluster'
        
        project.task([
                type: NbHomeTask
            ],
            'netbeanshome').init(project)
        
        project.task([
                dependsOn: project.tasks.netbeanshome
            ],
            'runApplication') doLast {
            println "beta"
            project.exec {
                commandLine project.tasks.netbeanshome.getLauncherArgs(project.tasks.netbeanshome.getClusterPaths() + project.tasks.netbeanshome.getNetbeansModulePaths())
            }
        }
        
        project.task([
                dependsOn: project.tasks.netbeanshome
            ],
            'debugApplication') doLast {
            project.exec {
                commandLine project.tasks.netbeanshome.getLauncherArgs(project.tasks.netbeanshome.getClusterPaths() + project.tasks.netbeanshome.getNetbeansModulePaths(), true)
            }
        }
        
//task zipmini(dependsOn: tasks.netbeanshome) << {
//
//	mkdir(rootProject.buildDir)
//
//	task('zipNetbeansNoGUIAndModules', type: Zip) {
//
//		appendix = 'mini'
//
//		ext.prefix = rootProject.name
//
//		configure zipCommonConfiguration
//
//		// used by launchers generator
//		from("$tasks.netbeanshome.dir/groovy/modules/ext/groovy-all.jar") {
//			into("$prefix/groovy/modules/ext")
//		}
//
//		tasks.netbeanshome.modules(false).findAll { module ->
//			module.nbm.cluster == 'my-cluster'
//		}.each { module ->
//			from(module.tasks.netbeans.moduleBuildDir) {
//				into("$prefix/$module.nbm.cluster")
//			}
//		}
//
//		from("$tasks.netbeanshome.dir/platform") {
//			into("$prefix/platform")
//		}
//	}.execute()
//}
        project.task([
                dependsOn: project.tasks.netbeanshome
            ], 'zipmini') {
            project.file("${project.buildDir}/distApplication").mkdirs()
            prefix = project.name
            
            project.task('zipNetbeansNoGUIAndModules', type: Zip) {
                appendix = 'mini'
		configure zipCommonConfiguration
                
                // used by launchers generator
                from("${project.tasks.netbeanshome.platformDir}/groovy/modules/ext/groovy-all.jar") {
                    into("$prefix/groovy/modules/ext")
                }

                project.
                    tasks.
                    netbeanshome.
                    getNetbeansModuleProjects(false).
                    findAll{moduleProject -> moduleProject.nbm.cluster == clusterName}.
                    each{moduleProject ->
                        from(moduleProject.tasks.netbeans.moduleBuildDir) {
                            into prefix + '/' + moduleProject.nbm.cluster
                        }
                    }

                from("${project.tasks.netbeanshome.platformDir}/platform") {
                    into prefix + '/platform'
                }
            }.execute()
        }
        
//task zip(dependsOn: tasks.netbeanshome) << {
//	mkdir(rootProject.buildDir)
//
//	task('zipNetbeansAndModules', type: Zip) {
//		ext.prefix = rootProject.name
//
//		configure zipCommonConfiguration
//
//		tasks.netbeanshome.modules().each { module ->
//			from(module.tasks.netbeans.moduleBuildDir) {
//				into("$prefix/$module.nbm.cluster")
//			}
//		}
//
//		tasks.netbeanshome.clusterDirs(true).each { cluster ->
//			from(cluster) {
//				into("$prefix/$cluster.name")
//			}
//		}
//	}.execute()
//}
        project.task([
                dependsOn: project.tasks.netbeanshome
            ], 'zipPlatformAddition') {
            prefix = project.name
            project.task('zipNetbeansAndModules', type: Zip) {
                appendix = 'platformAdditions'
		configure zipCommonConfiguration
                
                project.
                    tasks.
                    netbeanshome.
                    getNetbeansModuleProjects(false).
                    each{moduleProject ->
                        from(moduleProject.tasks.netbeans.moduleBuildDir) {
                            into prefix + '/' + moduleProject.nbm.cluster
                        }
                    }

                project.
                    tasks.
                    netbeanshome.
                    getClusterPaths(true).
                    each{clusterDir ->
                from(clusterDir) {
                    into prefix + '/' + clusterDir.name
                }
            }.execute()
        }
        
        project.dependencies {
            runtime project.fileTree(dir: "${project.tasks.netbeanshome.platformDir}/platform/lib", include: ['*.jar'])
            runtime project.fileTree(dir: "${project.tasks.netbeanshome.platformDir}/platform/lib/locale", include: ['*.jar'])
            runtime project.fileTree(dir: "${jdkhome()}/lib", include: ['tools.jar', 'dt.jar'])
        }
    }
    
    @Memoized
    def jdkhome() {
        def javahome = new File(System.getProperty('java.home'))
        javahome.name == 'jre' ? javahome.parent : javahome
    }
    
    def zipCommonConfiguration = {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	from(project.tasks.netbeanshome.platformDir) {
            into prefix + '/'
            include '**/*.sh'
            fileMode = 0744
	}
	from(project.tasks.netbeanshome.platformDir) {
            into prefix + '/'
            exclude '**/*.sh'
            exclude 'userdir'
            exclude 'netbeans'
            exclude 'netbeans.version'
	}
	from(project.tasks.netbeanshome.platformDir) {
            include 'userdir/**'
	}
    }
}

//buildscript {
//	ext {
//		netbeansVersion = 'RELEASE802'
//	}
//}
//
//File resolveSingleDependency(dependency) {
//	return rootProject.configurations.detachedConfiguration(rootProject.dependencies.create(dependency)).resolvedConfiguration.firstLevelModuleDependencies.first().moduleArtifacts.first().file
//}
//
//configure(subprojects) {
//	apply plugin: 'cz.kubacki.nbm'
//
//	nbm.cluster = 'my-cluster'
//  
//	afterEvaluate { project ->
//		if (configurations.hasProperty('ship')) {
//			task ship {
//				doFirst {
//					copy {
//						configurations.ship.allDependencies.withType(FileCollectionDependency.class)*.resolve()*.each { file -> from zipTree(file) }
//						configurations.ship.resolvedConfiguration.firstLevelModuleDependencies*.moduleArtifacts*.each { art -> from zipTree(art.file) }
//						into buildDir
//					}
//				}
//				doLast {
//					fileTree("$buildDir/netbeans") {
//						include '**/*.pack.gz'
//					}.each {
//						def path = it.canonicalPath
//						exec { commandLine jdkhome() + '/bin/unpack200', '-r', path, path[0..-9] }
//					}
//				}
//				doLast { thisTask ->
//					fileTree("$buildDir/netbeans") {
//						include '**/*.external'
//					}.each { external ->
//						Map<String, String> urls = external.readLines().findAll { it.startsWith('URL:') }
//								.collect { it[4..-1] }.collectEntries { it.split(':/', 2) as List }
//						copy {
//							if (urls.containsKey('m2')) {
//								def str = urls['m2']
//								def i = str.lastIndexOf(':')
//								from resolveSingleDependency(str[0..i - 1] + '@' + str[i + 1..-1])
//							} else if (urls.containsKey('http'))
//								from file('http:/' + url['http'])
//							else
//								throw new RuntimeException("Unknown protocol in $external")
//							into external.parent
//						}
//						delete external
//					}
//				}
//				doLast {
//					ant.move(file: "$buildDir/netbeans", tofile: tasks.netbeans.moduleBuildDir)
//				}
//				tasks.netbeans.dependsOn delegate
//			}
//		}
//	}
//}
//
//
//
//
//
//def batify(strings) {
//	strings*.toString().collect {
//		it.contains(' ') ? '"' + it + '"' : it
//	}.join(' ') + " %*\n"
//}
//
//task bat(dependsOn: tasks.netbeanshome) << {
//	def dirs = tasks.netbeanshome.clusterDirs() + tasks.netbeanshome.moduleDirs()
//	file('run.bat').setText(batify(tasks.netbeanshome.launchArgs(dirs)))
//	file('debug.bat').setText(batify(tasks.netbeanshome.launchArgs(dirs, true)))
//	dirs = files("$tasks.netbeanshome.dir/platform") + tasks.netbeanshome.moduleDirs(false)
//	file('run-mini.bat').setText(batify(tasks.netbeanshome.launchArgs(dirs)))
//	file('debug-mini.bat').setText(batify(tasks.netbeanshome.launchArgs(dirs, true)))
//}
//
//
//task auc(dependsOn: tasks.netbeanshome) << {
//	ant {
//		taskdef(
//				name: 'makeupdatedesc',
//				classname: 'org.netbeans.nbbuild.MakeUpdateDesc',
//				classpath: tasks.netbeanshome.modules().first().nbm.harnessConfiguration.asPath)
//
//		makeupdatedesc(
//				desc: './updates.xml',
//				distbase: '.',
//				automaticgrouping: 'true',
//				uselicenseurl: 'false',
////				notificationmessage: '',
////				notificationurl: '',
////				contentdescription: '',
////				contentdescriptionurl: '',
//		) {
//			fileset(dir: rootDir) {
//				include name: '**/*.nbm'
//			}
//		}
//	}
//}
//
//
//
//def zipCommonConfiguration = {
//
//	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//
//	from("$rootDir/distribution") {
//		into "$prefix/"
//		include '**/*.sh'
//		fileMode = 0744
//	}
//	from("$rootDir/distribution") {
//		into "$prefix/"
//		exclude '**/*.sh'
//		exclude 'userdir'
//		exclude 'netbeans'
//		exclude 'netbeans.version'
//	}
//	from("$rootDir/distribution") {
//		include 'userdir/**'
//	}
//}
//
//task zip(dependsOn: tasks.netbeanshome) << {
//	mkdir(rootProject.buildDir)
//
//	task('zipNetbeansAndModules', type: Zip) {
//
//		ext.prefix = rootProject.name
//
//		configure zipCommonConfiguration
//
//		tasks.netbeanshome.modules().each { module ->
//			from(module.tasks.netbeans.moduleBuildDir) {
//				into("$prefix/$module.nbm.cluster")
//			}
//		}
//
//		tasks.netbeanshome.clusterDirs(true).each { cluster ->
//			from(cluster) {
//				into("$prefix/$cluster.name")
//			}
//		}
//	}.execute()
//}
//
//
//task zipmini(dependsOn: tasks.netbeanshome) << {
//
//	mkdir(rootProject.buildDir)
//
//	task('zipNetbeansNoGUIAndModules', type: Zip) {
//
//		appendix = 'mini'
//
//		ext.prefix = rootProject.name
//
//		configure zipCommonConfiguration
//
//		// used by launchers generator
//		from("$tasks.netbeanshome.dir/groovy/modules/ext/groovy-all.jar") {
//			into("$prefix/groovy/modules/ext")
//		}
//
//		tasks.netbeanshome.modules(false).findAll { module ->
//			module.nbm.cluster == 'my-cluster'
//		}.each { module ->
//			from(module.tasks.netbeans.moduleBuildDir) {
//				into("$prefix/$module.nbm.cluster")
//			}
//		}
//
//		from("$tasks.netbeanshome.dir/platform") {
//			into("$prefix/platform")
//		}
//	}.execute()
//}
//
//task minizip(dependsOn: zipmini) //alias
