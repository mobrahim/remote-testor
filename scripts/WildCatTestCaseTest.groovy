/**
 * Tests that property stuff in the WildCatTestCase.
 */
class WildCatTestCaseTest extends GroovyTestCase {
	
	void testPropertyThing() {
		System.setProperty("wild.testProperties", "properties/WildCatTestCaseTest.properties")
		WildCatTestCase someTest = new WildCatTestCase()
		assertEquals("thePropertyValue", someTest.wcProperty("some.Arbitrary.Property"))
	}
	
	// TODO Make this work.. ;-) Implement it by stealing some code from Ant's Property class
	void testPropertyExpansion() {
		System.setProperty("wild.testProperties", "properties/WildCatTestCaseTest.properties")
		WildCatTestCase someTest = new WildCatTestCase()
		assertEquals("START thePropertyValue END", 
				someTest.wcProperty("some.Arbitrary.PropertyReferencingAnotherOne"))
	}
}