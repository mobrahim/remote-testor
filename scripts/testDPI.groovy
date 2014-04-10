
// TODO Simplify... a) convert all File into String (or untyped def?),  b) better Java after all (how about Ant?)

// TODO Rename to testOCS
class testDPI extends WildCatTestCase {
	AntBuilder Ant = new AntBuilder()

	final String ocsVersion = wcProperty("ocs.version")
	final File ocsPackagesRootDir = new File(wcProperty("ocsPackages")); // new File("//oams.com/Software/Incoming Packages/OCS/");
	final File standardOcsPackageName = new File("OCS$ocsVersion" + ".jar")
	// TODO This is all wrong - it should be the standard CD delivery layout, not just packages!
	final File incomingStandardOcsPackage = new File(ocsPackagesRootDir, ocsVersion + "/" + standardOcsPackageName.toString()) // new File(ocsPackagesRootDir, "$ocsVersion/disk3/distribution/OCS$ocsVersion.jar") // "w:/AutoDeployCIPetc/disk3/distribution/$standardOcsPackageName") // "$ocsPackagesRootDir/$ocsVersion/disk3/distribution/OCS$ocsVersions.jar"

	final File oamsHome = new File(wcProperty("wild.oamsHome")) // new File(wcProperty("wild.dir"), "oams.home")

	final String implType = wcProperty("impl.type")
    final String wasType = wcProperty("was.type")
    final String wasVersion = wcProperty("was.version")

	final File installerHome = new File("$oamsHome/ocs-installer")
	final File installerJavaTemp = new File("$installerHome/java-tmp")
	final File copiedStandardOcsPackageName = new File("$installerHome/$standardOcsPackageName")

	// TODO Remove final File builtDPIPackageName = new File("$installerHome/OCS$ocsVersion*-DPI*.jar")

	// NOTE: JUnit will re-initialize member variables in Test classes
	// for each test* method (I think it may actually create a new object instance for each), because tests
	// shouldn't be dependant on each other.  (If not static, it's set in testGeneratingFirstStandardDPIPackage,
	// but null in testInstallingDPIPackage.)
	//
	// If any variables need to be passed between tests, you need to define static member variables here.
	// Later, maybe one test could write some stuff into a property file (re-write THE property file, with new
	// properties?) and the next text could re-read that new property file?  Too complicated?  Only 'per-Job'.

	void testKillAnyLeftoverProcesses() {

	    execSysScript(wcProperty("wild.dir")+"/scripts", "kill-allUsingPath", wcProperty("wild.oamsHome"))
	}

	void testCopyIncomingPackageHere() {
		println "\n\ntestCopyIncomingPackageHere..."
		assertFileExists(incomingStandardOcsPackage)

		Ant.delete(dir: oamsHome)
		Ant.mkdir(dir: oamsHome)
		Ant.mkdir(dir: installerHome)

		Ant.copy(file: incomingStandardOcsPackage, toFile: copiedStandardOcsPackageName, verbose: true)
		writeFileMetadataProperties(incomingStandardOcsPackage, keepDir)
	}
	
	void writeFileMetadataProperties(File f, File destinationDirectory) {
		String lastModified = new Date(f.lastModified()).toLocaleString()
		String length = Long.toString(f.length())
		String absPath = f.getAbsolutePath()

		Properties fProperties = new Properties();
		fProperties.put("file.absoluteFile" , absPath)
		fProperties.put("file.lastModified" , lastModified)
		fProperties.put("file.length" , length)

		OutputStream os = new FileOutputStream(new File(destinationDirectory, f.getName() + ".properties"));
		fProperties.store(os, "Machine readable metadata for $absPath, lastModified $lastModified, $length bytes");
		os.close();
	}

	void testGeneratingFirstStandardDPIPackage() {
		println "\n\ntestGeneratingFirstStandardDPIPackage..."
		assertFileExists(copiedStandardOcsPackageName)

		def binding = ['installerHome': installerHome, 'implType': implType, 'wasType': wasType, 'wasVersion': wasVersion]
		copyWithTokenReplacement("properties/templates/dpi-builder-ant.install.properties", binding, "$installerHome/ant.install.properties")

		execJAR(installerHome, copiedStandardOcsPackageName, "text-auto")

		assert installerHome.list(new DPIJarFilenameFilter()).length == 1
		String dpiPackageName = installerHome.list(new DPIJarFilenameFilter())[0]
		//Keep DPI on remote until steria ticket for disk space on vmcit02
		Ant.copy(file: "$installerHome/" + dpiPackageName, toDir: keepDir, verbose: true)
	}

	void testInstallingDPIPackage() {
		println "\n\ntestInstallingDPIPackage..."
		assert installerHome.list(new DPIJarFilenameFilter()).length == 1
		String dpiPackageName = installerHome.list(new DPIJarFilenameFilter())[0]
		writePropertyFile("$installerHome/all-in-one.dpi.properties")
		execJAR(installerHome, new File(dpiPackageName), "text-auto")
	}

	// FYI: Deliberately separate test methods (=Tests) for AS engines etc., instead of startAll -> more individual green and red lights... clearer.

	// TODO: But start-all also needs to be tested... how, separately?  With this, in theory start-ocsAll could be broken and we wouldn't notice

	// TODO: reactivate once http://rd.oams.com/browse/OCS-16642 solved...
	void nok_testSetupVersion() {
		println "\n\ntestSetupVersion..."
		execSysScript("$oamsHome/install/setup", "setup", "version")
	}

	void testOTFShellXMLValidateWebDomainRecursively() {
		println "\n\ntestOTFShellXMLValidateWebDomainRecursively..."
		execSysScript("$oamsHome/admin", "otf-shell", "xmlvalidate -r -f $oamsHome/webDomain/")
	}

	// TODO Anything else useful to test with OTFShell?

	void testStartOCS_AS() {
		println "\n\ntestStartOCS_AS..."
		checkStartWithStatus("application-server")
		retrievePatchedSitemap()

	}

	void retrievePatchedSitemap()
	{
		def scanner = ant.fileScanner {
			fileset(dir: "$oamsHome") {
                include(name: '**/patched-sitemap.xmap')
            }
        }
        for (file in scanner) {
            Ant.copy(file: file, toDir: keepDir, verbose: true)
        }
	}

	void checkStartWithStatus(String serverType) {
		println "\n\ncheckStartWithStatus..."
		// This is 'blocking' - it will wait for AS startup
		execSysScript("$oamsHome/admin", "start-ocs", serverType)
		// TODO Check if when 'DOWN' is printed it returns non-0, and ant exec and thus JUnit fail the Test?
		// Or do we need to parse the output from exec, similarly as done in https://lausvn.oams.com/cgi-bin/viewvc.cgi/Tools/CustoTools/trunk/sdk/BASE/tools/deployer/source/com/odcgroup/custo/Cbuilder2Deployer.java?revision=16846&root=root&view=markup
		// Or do we need to loop and keep trying every 5s?  The start command seems to be 'blocking', so not needed?
		execSysScript("$oamsHome/admin", "check-status", serverType)
	}

	// TODO This is CDM-only I think... will fail for non-CDM implementation type; need to improve this later!  How to know if impl.type includes CDM without hard-coding anything here?
	void testBASetup() {
		println "\n\ntestBASetup..."
		execSysScript("$oamsHome/admin", "basetup")
	}

	void testComment() {
		println "\n\nHello World Test (DUMMY)..."
	}

	// TODO post-install ?

	// TODO import_infra_cdm_data.sh, later ??

	void nok_testStartEngines() {
		println "\n\ntestStartEngines..."
		checkStartWithStatus("heart-engine")
		checkStartWithStatus("otf-subscriber")
		checkStartWithStatus("otf-scheduler")
		checkStartWithStatus("otf-audit")
	}

	// Now everything should be up and running
	void nok_testCheckStatusAll() {
		println "\n\ntestCheckStatusAll..."
		execSysScript("$oamsHome/admin", "check-statusAll")
	}

	// TODO Run check-ocsAll.sh &  check-ocs-engine.sh - some unfinished scripts we know are failing today?

	// At the end of a WildCat test battery, the environment is intentionally put DOWN...
	// because WildCat is NOT an "investation environment", but intended to be a fully
	// automated nightly smoke test environment.  If something does need to be
	// investigated, somebody can always manually restart.
	void testStopOCSAll() {
		println "\n\ntestStopOCSAll..."
		execSysScript("$oamsHome/admin", "stop-ocsAll")
		// TODO Ok to NOT force here (as in the beginning) ... if a normal/non-force stop doesn't work, it's considered a test failure!
	}

	// Maybe the previous tests/job left something running (or somebody restarted manually),
	// so before going further we need to make sure nothing is running...
	void testStopOCSAll_IfPreviousJobLeftSomethingRunning() {
		println "\n\ntestStopOCSAll_IfPreviousJobLeftSomethingRunning..."
		execSysScript("$oamsHome/admin", "stop-ocsAll", "force", false)
		// TODO This should't make a test fail, "force"0
		// This ALWAYS ensure nothing is running, it force kill any remaining PID
	}

	void testXSPCompile() {
		println "\n\ntestXSPCompile... admin/classes script"
		execSysScript("$oamsHome/admin", "classes")
		println "\ntestXSPCompile... admin/xsp-compile script"
		execSysScript("$oamsHome/admin", "xsp-compile")
	}


	protected void execJAR(File workingDirectory, File jarFileName, String argsLine) {
		execJAR(workingDirectory.toString(), jarFileName.toString(), argsLine)
	}
	protected void execJAR(String workingDirectory, String jarFileName, String argsLine) {
		// TODO Fine 'java' from , in case it's not on the PATH ??
		// String cmd = wcProperty("wild.jdk") + "/jre/bin/java $jarFileName $argsLine"

		// TODO REmove the installerJavaTemp/java.io.tmpdir stuff once the installer does this automatically
		Ant.mkdir(dir: installerJavaTemp)
		println("     [exec] Going to run command 'java -Djava.io.tmpdir=$installerJavaTemp -jar $jarFileName $argsLine' in '$workingDirectory' now...")
		Ant.exec(executable: "java", dir: workingDirectory, failonerror: true) {
			arg(line: "-Djava.io.tmpdir=$installerJavaTemp -jar $jarFileName $argsLine")
		}

		// TODO I'm going to change this to do exec(java -jar) instead of Ant.java... to be more "realistic" to what a real TC would do
//		println("     [java] Going to test running '$jarFileName' in '$workingDirectory' now... (with arguments '$argsLine' passed on command line)")
//		Ant.java(fork: true, jar: jarFileName, dir: workingDirectory, failonerror: true) {
//			arg(line: argsLine)
//			/* TODO DOESN'T WORK, weired error - needed?? sysproperty(key: "java.io.tmpdir", "$installerJavaTemp") */
//		}

		// TODO Parse Out!  Ant.java can write it into a file... do I still see it on stdout then??
		// Scan for "Build Failed" and even "Warning" (?) messages, and assert/fail (JUnit) if found.
	}

	// TODO Move all of this to superclass...
	/**
	 * Execute system "script" (.bat or .sh etc.) command.
     * Do not specify .sh/.bat extension in the executable arg;
     * we will apend .bat or .sh depending on the $os.name that we're on.
	 */
	protected void execSysScript(String workingDirectory, String executable, boolean failonerror = true) {
		String osExecutable = "$executable." + getSysScriptExtension()
		println("     [exec] Going to run command '$osExecutable' in '$workingDirectory' now...")
		Ant.exec(executable: "$workingDirectory/$osExecutable", dir: workingDirectory, failonerror: failonerror)
	}
	protected void execSysScript(String workingDirectory, String executable, String argsLine, boolean failonerror = true) {
		String osExecutable = "$executable." + getSysScriptExtension()
		println("     [exec] Going to run command '$osExecutable $argsLine' in '$workingDirectory' now...")
		Ant.exec(executable: "$workingDirectory/$osExecutable", dir: workingDirectory, failonerror: failonerror) {
			arg(line: argsLine)
		}
	}
    private String getSysScriptExtension() {
		if (System.getProperty("os.name").startsWith("Windows")) {
			return "bat"
		} else {
			return "sh"
		}
	}

	// TODO execSH -- launch UNIX shell scripts.
	// Specify e.g. ".sh" ending.  Do not use exec if you know it's a UNIX shell script (only).
	// On Windows, we'll make sure we launch this under Cygwin..


	/**
	 * Ant of course offers this as well...
	 * but this is using the Groovy template feature - could do much more in a template than just token replacement here.
	 */
	void copyWithTokenReplacement(String srcTemplate, Map bindings, String destination) {
	   String templateText = new File(srcTemplate).getText()
	   def templateEngine = new groovy.text.SimpleTemplateEngine()
	   def catalogTemplate = templateEngine.createTemplate(templateText)
	   def catalog = catalogTemplate.make(bindings)
	   new File(destination).withWriter{ it << catalog }
	}


/*
	// runJavaMain(jarFile: standardOcsPackage, systemParameters: ["java.io.tmpdir": "$installerJavaTemp"], commandLine: "text-auto")

	void runJavaMain(//Map namedArgs,
			String jarFile, String commandLine, Map systemParameters) {
		//String jarFile = args.'jarFile'
		//String commandLine = args.'commandLine'
		// Map systemParameters = args.'systemParameters'

		// TODO Remove 3 lines if using jar instead classname works!
		// Either read META-INF/MANIFEST.MF, or use Ant.exec instead of Ant.java (need to know JDK though then)
		// classname: "org.tp23.antinstaller.selfextract.SelfExtractor"
		Ant.java(fork: true, jar: jarFile, dir: installerHome, failonerror: true) {
			arg(line: commandLine) // not better value: commandLine, is it?
			// TODO Iterate over systemParameters... how??
			sysproperty(key: systemParameters[0].key, value: systemParameters[0].value)
		}

		// TODO Create a new classpath for jarFile and run the main method in it, without running a new Java VM... ;-)
		// Actually, Ant's java with fork=false would do exactly that!  For now, play it safe with fork=true.
	}
*/

	// TODO assert for isDirectory, isWritable etc. needed?  Best would be to do this as a new Class!  A File subclass EFile / EDir that does these checks!

	/**
	 * Asserts that the file passed in argument exists,
	 * and is really a file and not just a directory, and is readable.
	 */
	void assertFileExists(String f) {
		assertFileExists(new File(f))
	}
	void assertFileExists(File f) {
		assertTrue("$f does not exist?!", f.exists())
		assertTrue("$f exists but is directory instead of a file?!  Something's wrong.", f.isFile())
		assertTrue("$f exists and is a file but can not be read?!", f.canRead())
	}

}
