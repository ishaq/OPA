package mixedProtocolsAnalysis;

public interface MPCAnnotation {
    // is used to mark output variables
    public void OUT(int x);
    
    // used to mark input variables (input vars should be assigned the return value)
    // it's a convenient method to shutup the compiler when it complains that variables 
    // are not initialized. it also helps in recognizing  which variables are 
    // input variables when one is looking at shimple code.
    public int IN();
}