# Optimal Protocol Assignment (OPA)
This repo contains companinion code for the ACM CCS'19 paper **Efficient MPC via Program Analysis: A Framework for Efficient Optimal Mixing**

## Directory Structure
* **eclipse-project** contains the analysis project. Import it into Eclipse and then run `Tests.java` as JUnit Test to invoke analysis for the specified program (in `Tests.java`), Analysis output is written to `analysis.json`.
* **solver** contains OPA solver MATLAB code. edit `solver.m` to point to the `analysis.json` and run into to get protocol assignment.

### How to Add More Test Programs
Follow the examples in `src/programs` directory in eclipse project directory. Briefly the programs should:

* only contain very simple `if` statements (current support for analyzing `if` is a simple heuristic.)
* loops should have statically known bounds (this is a standard limitation of MPC).
* only contain `public static` functions.
* input and output variables should be marked using function calls to `MPCAnnotation`. This ensures that such variables do not get eliminated as dead code during analysis.

## LICENSE
MIT License. see `LICENSE` for details.

## Troublelshooting
* If running analysis (whether through eclipse or commandline) gives you `unable to load java.lang.CharSequence` (or similar) error. Try running the analysis on a compiled `.class` file instead of java souce. Soot's java frontend is outdated and running it against compiled program fixes many issues.


