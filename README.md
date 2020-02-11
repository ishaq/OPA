# Optimal Protocol Assignment (OPA)
This repo contains companinion code for the ACM CCS'19 paper **Efficient MPC via Program Analysis: A Framework for Efficient Optimal Mixing**

## Directory Structure
* **analysis-project** contains a gradle java project for analysis (you need to have gradle installed to build/run analysis project). 
* **solver** contains OPA solver MATLAB code.

### How to Run
1. Analysis project needs to know the classpath (option `-cp`) and class name (option `-c`) of the program to generate anlaysis file for. It uses defaults for  the path of `rt.jar` (option `-r`) and `jce.jar` (option `-j`). If those default values are wrong, it will complain and you'll have to specify those paths on command line too.

	To generate analysis for, for exmaple, the gcd benchmark, run:
	```bash
	./gradlew run --args='-c P -cp ./src/tests/resources/programs/gcd' 
	```

	You can change the `-c` and `-cp` params to other benchmarks to test them. Analysis output is written to `analysis.json`.

2. Edit `solver.m` (in solver directory) to point to the `analysis.json` and run it to get protocol assignment.

### How to Generate Eclipse Project

You can run `./gradlew eclipse` to generate eclipse project and then import it into eclipse as an *Existing Gradle Project*.

Once the project is open in Eclipse, right-click `./src/test/java/MainTest.java` and
choose *Debug As -> Gradle Test* to debug it (with breakpoints and other debugging aids).
### How to Add More Test Programs
Follow the examples in `src/test/resources/programs` directory in the analysis project directory. Briefly the programs should:

* only contain very simple `if` statements (current support for analyzing `if` is a simple heuristic.)
* loops should have statically known bounds (this is a standard limitation of MPC).
* only contain `public static` functions.
* input and output variables should be marked using function calls to `MPCAnnotation`. This ensures that such variables do not get eliminated as dead code during analysis.

## LICENSE
MIT License. see `LICENSE` for details.

## Troublelshooting
* If running analysis (whether through eclipse or commandline) gives you `unable to load java.lang.CharSequence` (or similar) error. Try running the analysis on a compiled `.class` file instead of java souce. Soot's java frontend is outdated and running it against compiled program fixes many issues.


