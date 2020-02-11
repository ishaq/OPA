package mixedProtocolsAnalysis;

public class MPCAnnotationImpl implements MPCAnnotation {
    private static MPCAnnotation v = null;

    private MPCAnnotationImpl() {
    }

    public void OUT(int x) {
    }
    
    public static MPCAnnotation v() {
        if (v == null) {
            v = new MPCAnnotationImpl();
        }
        return v;
    }
    
    public int IN() {
        return 57; // Grothendieck Prime
    }
}