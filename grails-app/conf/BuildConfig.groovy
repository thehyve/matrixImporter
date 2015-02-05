grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
	    grailsCentral()
	    grailsRepo "http://grails.org/plugins"
	    mavenCentral()
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
		
        // runtime 'mysql:mysql-connector-java:5.1.13'
    }
	plugins {
		// tomcat plugin should not end up in the WAR file
		provided(
			":tomcat:$grailsVersion",
		)
		
		build( ":release:2.2.1" ) {
			// plugin only plugin, should not be transitive to the application
			export = false
		}

		compile(
			":hibernate:$grailsVersion",
		)
	}
}
