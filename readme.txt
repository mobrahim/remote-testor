ReadMe
==============

Small infrastructure project to easily write fully automated tests to install/deploy full applications.

The focus is on the automation of distribution to scale up to a high number of target environment combination,
and a unified "once-place" view of the results of the results of these distributed tests.

One phrase summary: A combination of a central build server and some tools written in Ant & Groovy
to regularly copy scripts maintained in the CVS to a number of environments, run JUnit tests from system
installation up to front-end web tests there, and fetch results back.

How it works - high-level
-------------------------

build-central.xml is called on a central machine (Hudson/Jenkins build server)

build-central.xml simply delegates to remoteExecutor.groovy.  
The only point of this is simple and easy integration into Hudson or Bamboo.
We could also have kicked it off by called "GROOVY_HOME/bin/remoteExecutor.groovy" in theory.

remoteExecutor.groovy then does some sftp to a remote machine, and will then run bin/ant -f build-remote.xml.

build-remote.xml will run JUnit Test Cases, on the remote machines.
For local development and testing, it is perfectly possible to run build-remote.xml,
or more easily directly the Groovy Tests (via Eclipse plug-in) on the local Windows development workstations.
