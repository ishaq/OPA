package mixedProtocolsAnalysis;

import org.junit.Test;

import junit.framework.TestCase;

/* A JUnit test that configures the argument array, then launches Soot and RTAAnalysis. 
 * For more information on basic Soot command-line arguments check this site:
 * https://github.com/Sable/soot/wiki/Introduction:-Soot-as-a-command-line-tool
*/

public class Tests extends TestCase {
	
	// jce.jar is required when doing whole program optimization (we run static inliner, which is a whole program pack)
	private static String RT_HOME = "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar";

	@Test
	public void test2() {
		// TODO: if optimization line is commented, this should be decremented
		String[] args = new String[19];
		int largeValue = 1000000;
		// -app causes Soot to run in "application mode", i.e., analysis scope is application 
		// classes only, no JDK classes. For now, consider this unsound application-only analysis. 
		// Later we will include java.* classes in the analysis. 
		int i = 0;
		//args[i++] = "-app"; // passing -app causes soot to look at all the referenced classes (except those in java.* and com.sun.*), we want it to look at classes passed in arguments only
		// NOTE: intentionally running whole program JIMPLE (-w) instead of whole program SHIMPLE (-ws) because static inliner (wjop.si) does not
		// work with shimple
		args[i++] = "-w";
		args[i++] = "-p";
		args[i++] = "wjop";
		args[i++] = "on";
		args[i++] = "-p";
		args[i++] = "wjop.si";
		args[i++] = "expansion-factor:" + largeValue;
		args[i++] = "-p";
		args[i++] = "wjop.si";
		args[i++] = "max-container-size:" + largeValue;
		args[i++] = "-p";
		args[i++] = "wjop.si";
		args[i++] = "max-inlinee-size:" + largeValue;
		// -f J causes Soot to write out .jimple files. The default output directory is sootOutput
		// which is usually located in the same directory as src.
		args[i++] = "-f";
		//args[3] = "J";
		args[i++] = "shimple";
		// TODO: comment the following line?
		args[i++] = "-O"; // Optmization causes Def/Uses to throw NullPointerException in case a PhiNode gets collapsed, therefore turning it off
		// -cp specifies the class path. Must include a path to the application classes, and the rt.jar
		args[i++] = "-cp";
		args[i++] = "./src/programs/psi/:"+RT_HOME;
		// specifies the class that contains the "main" method
		args[i++] = "P";
 		Main.main(args);
	
	}
}
