
import groovy.util.GroovyTestCase

// abstract, by not making it so that the WildCatTestCaseTest can easily test it
class WildCatTestCase extends GroovyTestCase {
	
	private final String testPropertiesFile;
	private final Properties testProperties;
	protected final File keepDir = new File('workToKeep');
	
	WildCatTestCase() {
		testPropertiesFile = getPropertySafely(System.getProperties(), "wild.testProperties");
		
		InputStream is = new FileInputStream(testPropertiesFile);
		testProperties = new Properties();
		testProperties.load(is);
		is.close();
	}
	
	/**
	 * Write (copy) the property file specified by the 'wild.testProperties' system property
	 * to propertyFileToWrite.  The properties are expanded - which allows to use ${property}
	 * placeholders in the .properties files in SVN, even though OCS installer doesn't
	 * understand property expansion.
	 */
	protected void writePropertyFile(String propertyFileToWrite) {
		OutputStream os = new FileOutputStream(propertyFileToWrite);
		testProperties.store(os, "From " + testPropertiesFile);
		os.close();
		println("Copied (with property expansion) the $testPropertiesFile to $propertyFileToWrite")
	}
	
	// This weired stuff is needed because of how Groovy works...
	// In Plain Text, if we have only a String getProperty(String propertyName) method then e.g.
	// assertEquals("thePropertyValue", getProperty("someArbitraryProperty")) won't work,
	// because that expects an Object not a String, so Groovy's dynamic lookup tries to
	// find a method returning Object, not String.  Maybe it's also simply a bug in G.
	Object wcProperty(String propertyName) {
		return getPropertyPrivate(propertyName)
	}
	String wcProperty(String propertyName) {
		return getPropertyPrivate(propertyName)
	}
	private String getPropertyPrivate(String propertyName) {
		return getPropertySafely(testProperties, propertyName)
	}
	
	private String getPropertySafely(Properties properties, String propertyName) {
		if (properties.getProperty(propertyName) == null 
		 || properties.getProperty(propertyName).length() == 0) {
			fail("Property '$propertyName' is not set!")
		}
		else {
			String value = properties.getProperty(propertyName)
			return value.trim()
		}
	}
}