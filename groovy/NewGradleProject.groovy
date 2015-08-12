// NewGradleProject.groovy
// Author: Patrick Gallagher

// TODO:
// 1. Look at Mr Haki posts
// 2. README on, from the command-line, using Gradle
//    - how to build source / distributable
//      * ./gradlew assemble
//    - how to run tests / how to view test reports (./build/reports/tests/com.gorkana.CalculatorSpec.html)
//      * ./gradlew test
//    - how to run distributable (either using JavaExec or as an application using Application plugin)
//      * will need to specify a main class in the build.gradle file
//    - info about Gradle itself (see Martin's README from LB)
//    How to import into an Eclipse distribution with Groovy-Eclipse plugin installed (eg. GGTS or STS if not groovy project)
//      * ./gradlew eclipse
// 3. Gradle Wrapper tasks
// 4. Add Spock to Groovy project:
//       testCompile "org.spockframework:spock-core:0.7-groovy-1.8"
//    Should i upgrade to groovy-2.x and use:
//     0.7-groovy-2.0
//    If i write Spock tests for Groovy project, the person who runs
//    the distributable will also need Groovy installed?? Correct?
//    * Maybe not as I think it will be compiled by the Gradle wrapper and tests run by the Gradle wrapper
//      which as the project already has groovy on it's dependencies should be able to compile/run Groovy.
// 5. Want an application where:
//    i. there is a distributable
//   ii. the user can generate a distributable from build script (Gradle Wrapper)
//  iii. be able to run tests
//   iv. be able to run the application / distributable
//    v. create javadoc / groovydoc
// 6. Add plugins:
//    i. Findbugs
//   ii. Checkstyle (?)
// 7. What about Groovy versions (Spock brings it's own groovy & junit dependencies)?
//    Should i align them exactly (ie. groovy-all-1.8.8) or try and remove the transitive
//    dependencies from Spock?
// 8. Always bring a copy of this file to programming tests. May need to think about having groovy
//    available - is there a groovy wrapper like gradle / grails wrapper?
// 9. Add Mockito to dependencies list

def GROOVY = "groovy"
def JAVA = "java"
def APPLICATION = "application"

def plugins = [GROOVY, JAVA, APPLICATION]

def projectName
def projectType

def console = System.console()
if (console) {
	projectName = console.readLine('> Please enter the project name: ')
	projectType = console.readLine("> Please enter the project type (eg. ${plugins.join(', ')}): ")
	if (!plugins.contains(projectType)) {
		println "Unknown project type '$projectType'. Exiting."
		System.exit 1
	}
} else {
	println "Cannot get console. Exiting."
	System.exit 1
}

def dependencies = { out ->
	if (projectType == GROOVY) out << "groovy 'org.codehaus.groovy:groovy-all:1.8.8'"
	out << 
			""" 
			compile files(fileTree(dir: 'lib', includes: ['*.jar'])),
        			'joda-time:joda-time:2.1',
	    			'com.google.guava:guava:12.0'
	    	testCompile 'junit:junit:4.10'		
			"""
	if (projectType == GROOVY) { 
		out << 
			""" 
			testCompile('org.spockframework:spock-core:0.7-groovy-1.8') {
				exclude module: 'groovy-all'
				exclude module: 'hamcrest-core'
				exclude module: 'junit-dep'
			}	
			"""
	}
	out << "runtime 'com.h2database:h2:1.3.170'"
}

println "Creating directories & files for new ${projectType.capitalize()} Gradle project ..."

// "mkdir $projectName".execute()
println "Making directory $projectName"
new File("$projectName").mkdir()

new File("$projectName/lib").mkdir()
new File("$projectName/docs").mkdir()

new File("$projectName/.gitignore").withPrintWriter{ w ->
     "build,bin,.gradle,.settings,.classpath,.project".split(",").each{ 
         w.println(it)
     }
}

new File("$projectName/build.gradle") << """\
apply plugin: '$projectType'
apply plugin: 'eclipse'
 
repositories {
    mavenCentral()
}
 
dependencies {
	$dependencies
}
 
task createSourceDirs {
	doLast {
    	sourceSets*.allSource.srcDirs.flatten().each { File sourceDirectory ->        
        	if (!sourceDirectory.exists()) {
            	println "Making \$sourceDirectory"
            	sourceDirectory.mkdirs()
        	}
    	}
	}
}

task wrapper(type: Wrapper) {
    gradleVersion = '1.4'
}

"""

// Use Ant to execute gradle and git commands

def ant = new AntBuilder()
// "cmd /c gradle createSourceDirs eclipse".execute()
ant.exec(executable: "gradle", dir: "$projectName") {
	arg(value: "createSourceDirs")
	arg(value: "eclipse")
}

// "cmd /c git init".execute()		
ant.exec(executable: "git", dir: "$projectName") {
	arg(value: "init")
}

Thread.start {
	sleep 5000 // allow time for all files to be created
	new File("$projectName").eachFile {
		println it
	}
}