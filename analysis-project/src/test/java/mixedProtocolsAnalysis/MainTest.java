/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package mixedProtocolsAnalysis;

import org.junit.Test;

import mixedProtocolsAnalysis.Main;

public class MainTest {
    @Test public void testSanity()  {
		Main m = new Main();
		m.performAnalysis("./src/test/resources/programs/playground/", "P");
	}
}
