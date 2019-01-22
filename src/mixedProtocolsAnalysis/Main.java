package mixedProtocolsAnalysis;


import java.util.Iterator;

import soot.Pack;
import soot.PackManager;
import soot.Transform;


public class Main {
	
	/* A main driver for RTA analysis. */
	public static void main(String[] args) {		
        long startTime = System.currentTimeMillis();
		
        // Code hooks the Analysis then launches Soot, which traverses 
        PackManager pm = PackManager.v();
        Pack p = pm.getPack("stp");
        
//        Iterator<Transform> iterator = stp.iterator();
//        Transform last = null;
//        while(iterator.hasNext()) {
//        	last = iterator.next();
//        }
        
//        String phaseName = last.getPhaseName();
        Analysis analysis = new Analysis(); 
        Transform t = new Transform("stp.mixedprotocols", analysis);
        
		//p.insertAfter(t, phaseName);
        //p.insertAfter(t, "sop.cpf");
		p.add(t);
        soot.Main.main(args);

//        String outputDir = SourceLocator.v().getOutputDir();
        
        analysis.showResult();
        
        long endTime   = System.currentTimeMillis();
        // FIXME: this really should be a logger.info statement instead of System.out.println that echos "INFO"
        System.out.println("INFO: Total running time: " + ((float)(endTime - startTime) / 1000) + " sec");

	}
}
