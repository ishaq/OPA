package mixedProtocolsAnalysis;


import soot.PackManager;
import soot.Transform;


public class Main {
	
	/* A main driver for RTA analysis. */
	public static void main(String[] args) {		
        long startTime = System.currentTimeMillis();
		
        // Code hooks the Analysis then launches Soot, which traverses 
        Analysis analysis = new Analysis(); 
        PackManager.v().getPack("stp").add(new Transform("stp.mixedprotocols", analysis));
		soot.Main.main(args);

//        String outputDir = SourceLocator.v().getOutputDir();
        
        analysis.showResult();
        
        long endTime   = System.currentTimeMillis();
        // FIXME: this really should be a logger.info statement instead of System.out.println that echos "INFO"
        System.out.println("INFO: Total running time: " + ((float)(endTime - startTime) / 1000) + " sec");

	}
}
