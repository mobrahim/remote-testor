// TODO Maybe it would actually be easier and more understandable to others = more maintanable
// if I simply wrote as Ant into build-central.xml?  No real advantage in using Groovy for this particular bit, after all.

new remoteExector(hostname: getAntProperty('wild.hostname'), 
		uid: getAntProperty('wild.uid'), pwd: getAntProperty('wild.pwd'), 
		remoteBaseDir: getAntProperty('wild.dir'),
		remoteJDK: getAntProperty('wild.jdk'),
		testPropertyFileName: getAntProperty('wild.testProperties'),
		useSFTP: Boolean.toBoolean(getAntProperty('wild.useSFTP'))).go()

// For rapid local execution inside Eclipse without build.xml, uncomment:
// new remoteExector(hostname: 'laurs55a', uid: 'ocs', pwd: 'ocs0ocs', 
//		remoteBaseDir: '/users/ocs/ocs/laurs55a/MVO-wildCat/',
//		remoteJDK: '/soft/ibm/Was6/AppServer/java').go()

/** 
 * Copies the entire project to a remote server, runs something there, and gets transfers some files back locally.
 */
class remoteExector {
	// Ant is a new AntBuilder... not the one that called this script (or not, if this is run directly)
	AntBuilder Ant = new AntBuilder()
	// TODO Is it possible to put this at the end?
	String hostname;
	String uid;
	String pwd;
	String remoteBaseDir; // Careful, don't screw this up - everything in here will be deleted!
	String remoteJDK;
	String testPropertyFileName;
	boolean useSFTP;
	
	// This TAR file is created (and it's name hard-coded!) in build-remote.xml
	final remoteWorkToKeepFile = 'workToKeep.tar.gz';				
	final localWorkToKeepDir = 'workToKeep.fromRemoteSystem';
	
	void go() {
		// First, remote copy:
		sshexec("rm -rf $remoteBaseDir; mkdir -p $remoteBaseDir", false)
		
		// I had quite some trouble with the scp task, based on jsch.  When copying to Solaris,
		// it would get stuck at the end (waiting for something), only if a fileset was transfered. 
		// Maybe this is because of that "menu"; but it works fine when transfering only one file.
		// Easiest work-around, faster anyway: TAR everything up and send just that!

		final String tarName = "wildCat.tar.gz"
		Ant.delete(file: tarName, failonerror: true, verbose: true)
		Ant.tar(destfile: tarName, compression: "gzip", longfile: "fail") {
			fileset(dir: '.') {
				exclude(name: ".settings/")  // Problematic during development, from Eclipse work-space (file lock)
				exclude(name: "bin.groovyc/") // Shouldn't be in SVN, but if it is, build.xml shouldn't depend on this!
				exclude(name: "bin-groovy/") // Shouldn't be in SVN, but exists when testing locally - don't send!
				exclude(name: "JUnitReports/") // Shouldn't be in SVN, but may exist if testing locally - don't send!
				exclude(name: "$localWorkToKeepDir/") // Shouldn't be in SVN, but may exist if testing locally - don't send!
				exclude(name: "wildCatJUnitReports/") // Shouldn't be in SVN, but may exist if testing locally - don't send!
				exclude(name: ".*")
			}
		}
		String protocol = useSFTP ? "SFTP" : "SCP";
		println "Going to $protocol a TAR ball of local directory . to remote directory '$remoteBaseDir' on host $hostname as user '$uid' now..."
		// A "Caused by: java.io.IOException: inputstream is closed" means you should use sftp="false" for scp instead sftp!
		Ant.scp(remoteTodir: "$uid:$pwd@$hostname:$remoteBaseDir", file: tarName,
				trust: true, verbose: true, sftp: useSFTP);
 
		// Next remoteExcecute:
		Ant.mkdir(dir: localWorkToKeepDir)
		sshexec("cd $remoteBaseDir; gunzip -c wildCat.tar.gz | tar xvf -; chmod +x bin/*; chmod +x scripts/*; export JAVA_HOME=$remoteJDK; bin/ant -Dwild.testProperties=$testPropertyFileName -f build-remote.xml", true)

		// Lastly, fetch back some results... like the JUnit XML reports, maybe DPI package?
		// MAI Commented out due to "No space left on device" ... pending Steria ticket
		Ant.scp(file: "$uid:$pwd@$hostname:$remoteBaseDir/$remoteWorkToKeepFile", todir: localWorkToKeepDir, trust: true, verbose: true, sftp: useSFTP)
		println("Retrieved Work To Keep (JUnit Test reports, DPI, etc.) into local directory '$localWorkToKeepDir'")
		Ant.untar(src: "$localWorkToKeepDir/$remoteWorkToKeepFile", dest: localWorkToKeepDir, compression: 'gzip')
		// To save disk space, we remove the tar.gz file once unzipped; no need to keep it
		Ant.delete(file: "$localWorkToKeepDir/$remoteWorkToKeepFile", failonerror: true, verbose: true)
		
		// Finally Remove remote Dir.
		//sshexec("rm -rf $remoteBaseDir", false)
	}

	void sshexec(commandLine, logIt) {
		println "Going to run command '$commandLine' on host $hostname as user '$uid' now..."
		def timeout =  4*60*60*1000 /* 4h */
		if (!logIt) {
			Ant.sshexec(host: hostname, username: uid, password: pwd, trust: true, timeout: timeout, command: commandLine, failonerror: false)
		} else {
			Ant.sshexec(host: hostname, username: uid, password: pwd, trust: true, timeout: timeout, command: commandLine, failonerror: false,
					output: "$localWorkToKeepDir/sshexec.log")
		}
	}
}

String getAntProperty(String propertyName) {
	if (ant.project.properties."$propertyName" == null 
	 || ant.project.properties."$propertyName".length() == 0) {
		println "Property '$propertyName' (Ant property) is not set!"
		assert false
	}
	else {
		return ant.project.properties."$propertyName".trim()
	}
}
