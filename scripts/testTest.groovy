/**
 * This is a very simple hard-coded fixed test.
 * It is used to test the underlying distribution etc. infrastructure.
 */
class testTest extends WildCatTestCase {
	AntBuilder Ant = new AntBuilder()

	void testHappyWorld() {
		System.out.println('testHappyWorld, on STDIN')
		System.err.println('testHappyWorld, on STDERR')
		assertTrue("OK", true)
		Ant.echo(message: "new AntBuilder().echo visible?")
		
		println System.getProperties()
	}

	void testPropertyStuff() {
		assertEquals("thePropertyValue", wcProperty("someArbitraryProperty"))
	}
	
	void testWorkingDirectory() {
		File dir = new File (wcProperty("wild.dir"))
		
		assertTrue dir.exists()
		assertTrue dir.isDirectory()
		assertFalse dir.isHidden()
		assertTrue dir.canRead()
		assertTrue dir.isAbsolute()
	}
/*	
	void testGroovyFileRegexp() {
		// This from http://groovy.codehaus.org/Recipes+For+File lead to a weired 
		// "java.lang.NoSuchMethodError: org.codehaus.groovy.runtime.ScriptBytecodeAdapter.bitNegate(Ljava/lang/Object;)Ljava/lang/Object;"
		// with groovy-1.1-rc-2.jar which went away when I downgraded to 1.0 stable.  Make sure this works before upgrading to 1.1 final!
		def p = ~/.*groovy/
		new File( 'w:\\AutoDeployCIPetc' ).eachFileMatch(p) {
		    f ->
		    println f
		}
	}
*/
}

