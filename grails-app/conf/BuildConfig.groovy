/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : BuildConfig.groovy
 *  Author              : ianfieldhouse
 *  Approver            : Ian Fieldhouse 
 * 
 *  Notes               :
 *
 *
 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 */

println "******************************************"
println "In BuildConfig.groovy"

/* 
	NOTE
	----
	
	Config.groovy has the ability to support custom config files via grails.config.locations. These are then loads and 
	merged with the config. Unfortunately not every Grails variable can be specified in Config.groovy or a custom
	config file. The config vars which are set as part of the BuildConfig (ie this script), are available in the
	hash buildProps - see "_GrailsSettings.groovy" in the Grails install dir and closure getPropertyValue.
	BuildConfig.groovy is parsed *BEFORE* Config.groovy and as such settign build vars in Config.groovy serves no 
	purpose.
	An important example of a var which cannot be set in Cong.groovy is 'grails.server.port.http' the HTTP server port.
	To allow custom setting of all vars in a custom config file, the below code will load the same
	custom config file as the Config.groovy script does but pulls out variables which should be set in the 
	buildProps hash and sets them accordingly. This has the drawback of making the startup time slightly slower as 
	the same file is parsed twice but has the advantage of making these variables easilly configurable.

*/
import java.net.InetAddress
currentLocalHostName = InetAddress.getLocalHost().getHostName()
println "Parsing config file file:${basedir}/etc/${currentLocalHostName}-config.groovy"
def fLocation = "file:${basedir}/etc/${currentLocalHostName}-config.groovy"
def customConfig = new ConfigSlurper().parse(fLocation.toURL())

// Pull out the props to set in the hash buildProps
grails.server.port.http = customConfig.grails.server.port.http
grails.server.port.https = customConfig.grails.server.port.https
grails.server.host = customConfig.grails.server.host
grails.index.targetMap=customConfig.grails.index.targetMap

println "******************************************"

grails.project.work.dir = "build"

grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir	= "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits( "global" ) {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {        
        grailsPlugins()
        grailsHome()
        grailsCentral()

        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenLocal()
        //mavenCentral()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        // runtime 'mysql:mysql-connector-java:5.1.5'
    }

}
