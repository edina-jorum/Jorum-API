Jorum API
=========

About
=====

A Groovy on Grails web application to provide an API to Jorum's modified version of DSpace v1.5.2 https://github.com/edina-jorum/Jorum-DSpace.

Prerequisites
=============

* Grails http://www.grails.org/ version 1.3.5 - Known to work with earlier versions of Grails (back to 1.3.1) but not tested with newer versions, though should work ok
* A working installation of Jorum DSpace 2.1 https://github.com/edina-jorum/Jorum-DSpace

Pre-run
=======

* Download the project source files https://github.com/edina-jorum/Jorum-API
* Copy etc/HOSTNAME-config.groovy.sample to etc/YOUR_HOSTNAME-config.groovy where YOUR_HOSTNAME is the fully qualified hostname of the server where you intend to run the Jorum API application, e.g. api.jorum.ac.uk-config.groovy
* Customise your newly created YOUR_HOSTNAME-config.groovy file for your environment
* Copy etc/HOSTNAME_build.properties.sample to etc/YOUR_HOSTNAME_build.properties where YOUR_HOSTNAME is the fully qualified hostname of the server where you intend to run the Jorum API application, e.g. api.jorum.ac.uk_build.properties
* Customise your newly created YOUR_HOSTNAME_build.properties file for your environment
* Copy the following required jar files from your Jorum DSpace install into the Jorum API lib/ directory:
	- dspace-api-1.5.2.jar
	- dspace-stats-api-1.5.2.jar
	- icu4j-3.4.4.jar
	- jorum-utils-1.0.jar
	- log4j-1.2.14.jar
	- postgresql-8.1-408.jdbc3.jar
* Copy, or create a symlink of, your Jorum DSpace conf/dspace.cfg file into the Jorum API grails-app/conf/ directory

Running
=======

Make sure you are in the root directory of the project then issue the following command: 

grails run-app