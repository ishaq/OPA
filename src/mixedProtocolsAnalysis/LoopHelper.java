package mixedProtocolsAnalysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.AddExpr;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.DivExpr;
import soot.jimple.DoubleConstant;
import soot.jimple.EqExpr;
import soot.jimple.FloatConstant;
import soot.jimple.GeExpr;
import soot.jimple.GtExpr;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.LeExpr;
import soot.jimple.LongConstant;
import soot.jimple.LtExpr;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.NegExpr;
import soot.jimple.RemExpr;
import soot.jimple.Stmt;
import soot.jimple.SubExpr;
import soot.jimple.UnopExpr;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.scalar.Evaluator;
import soot.shimple.PhiExpr;
import soot.shimple.ShimpleBody;
import soot.shimple.toolkits.scalar.ShimpleLocalDefs;
import soot.toolkits.graph.LoopNestTree;
import soot.toolkits.scalar.ValueUnitPair;

public class LoopHelper {
	protected static int DEFAULT_LOOP_ITERATIONS = Integer.MAX_VALUE;
	protected LoopNestTree loopsTree = null;
	ShimpleLocalDefs localDefs = null;
	protected Set<IfStmt> ifStmtOwnedByLoops = new HashSet<IfStmt>();
	
	LoopHelper(Body b) throws UnsupportedFeatureException {
		if(!(b instanceof ShimpleBody)) {
			throw new UnsupportedFeatureException("Not a shimple body");
		}
		this.loopsTree = new LoopNestTree(b);
		this.localDefs = new ShimpleLocalDefs((ShimpleBody) b);
	}
	
	/*
	 * returns;
	 * 		- if l1 and l2 are the same loop, l1
	 * 		- if l1 and l2 are different loops contained in a bigger loop, their immediate ancestor
	 * 		- if l1 and l2 are disjoint loops, returns null
	 */
	public Loop getCommonAncestor(Loop l1, Loop l2) {
		if(l1 == null || l2 == null) {
			return null;
		}
		
		// same loop
		if(l1.getLoopStatements().equals(l2.getLoopStatements())) {
			return l1;
		}
		// FIXME: see comment after the for loop
		boolean l1Found = false;
		boolean l2Found = false;
		for(Loop l: loopsTree) {
			
			if(l.getLoopStatements().equals(l1.getLoopStatements())) {
				l1Found = true;
			}
			if(l.getLoopStatements().equals(l2.getLoopStatements())) {
				l2Found = true;
			}
			if(l1Found && l2Found) {
				break;
			}
		}
		assert(l1Found && l2Found); // the assertion below fails in some situations. Perhaps loopsTree.contains is buggy?
		// assert(loopsTree.contains(l1) && loopsTree.contains(l2)); // if our loopsTree doesn't contain those loops, we can't find ancestor
		Loop outer = l1;
		Loop inner = l2;
		if(loopsTree.headSet(l1).size() < loopsTree.headSet(l2).size()) {
			outer = l2;
			inner = l1;
		}
		
		while(true) {
			Collection<Stmt> outerStatements = outer.getLoopStatements();
			Collection<Stmt> innerStatements = inner.getLoopStatements();
			
			if(outerStatements.containsAll(innerStatements)) {
				return outer;
			}
			
			SortedSet<Loop> outerTailSet = loopsTree.tailSet(outer, false);
			if(outerTailSet.size() == 0) {
				break;
			}
			
			// Essentially takes the parent of outer
			outer = outerTailSet.first();
			//outerTailSet = loopsTree.tailSet(outer, false);
		}
		
		return null;
	}
	
	public void setDefUseWeights(Node def, Node use) throws UnsupportedFeatureException, RuntimeException {
		Loop defLoop = getImmediateParentLoop(def.id);
		def.weight = guessLoopIterationsIncludingParentLoops(defLoop);
		
		Loop useLoop = getImmediateParentLoop(use.id);
		use.weight = guessLoopIterationsIncludingParentLoops(useLoop);
		
		Loop ancestor = getCommonAncestor(defLoop, useLoop);
		Loop secondOldestAncestorOfUse = getSecondOldestAncestor(use.id, ancestor);
		if(secondOldestAncestorOfUse == null) {
			use.setConversionPoint(use.id);
		}
		else {
			use.setConversionPoint(secondOldestAncestorOfUse.getHead());
		}
		
		use.conversionWeight = guessLoopIterationsIncludingParentLoops(ancestor);				
	}
	
	public Set<IfStmt> getIfStmtsOwnedByLoops() {
		return this.ifStmtOwnedByLoops;
	}
	
	public Loop getImmediateParentLoop(Stmt stmt) {
		for(Loop l: loopsTree) {
			Collection<Stmt> loopStmts = l.getLoopStatements();
			if(loopStmts.contains(stmt)) {
				return l;
			}
		}
		return null;
	}
	
	public Loop getCommonAncestor(Stmt s1, Stmt s2) {
		return getCommonAncestor(getImmediateParentLoop(s1), getImmediateParentLoop(s2));
	}
	
	public Map<Loop, Set<Local>> getLoopCounterVariables(Map<Stmt, DefUse> defUses) throws UnsupportedFeatureException {
		Map<Loop, Set<Local>> loopCounterVars = new HashMap<Loop, Set<Local>>();
		Iterator<Loop> iter = loopsTree.iterator();
		while(iter.hasNext()) {
			Loop l = iter.next();
			Set<Local> thisLoopCounterVars = getLoopCounterVariables(l, defUses);
			Collection<Stmt> thisLoopStatements = l.getLoopStatements();
			for(Loop prevLoop: loopCounterVars.keySet()) {
				Collection<Stmt> prevLoopStatements = prevLoop.getLoopStatements();
				if(thisLoopStatements.containsAll(prevLoopStatements)) {
					Set<Local> prevLoopVars = loopCounterVars.get(prevLoop);
					thisLoopCounterVars.addAll(prevLoopVars);
				}
			}
			
			loopCounterVars.put(l, thisLoopCounterVars);
		}
		return loopCounterVars;
	}
	
	public Set<Local> getLoopCounterVariables(Loop l, Map<Stmt, DefUse> defUses) throws UnsupportedFeatureException {
		Set<Local> thisLoopCounterVars = new HashSet<Local>();
		Iterator<Stmt> iter = l.getLoopStatements().iterator();
		while(iter.hasNext()) {
			Stmt s = iter.next();
			if(s instanceof IfStmt) {
				IfStmt ifStmt = (IfStmt)s;
				ConditionExpr cond = (ConditionExpr) ifStmt.getCondition();
				Set<Local> condVariables = Util.getVariables(cond);
				thisLoopCounterVars.addAll(condVariables);
				break;
			}
		}
		
		thisLoopCounterVars = Util.updateVariablesToIgnore(thisLoopCounterVars, defUses, l);
		return thisLoopCounterVars;
	}
	
	public LoopNestTree getLoopsTree() {
		return loopsTree;
	}
	
	private Loop getSecondOldestAncestor(Stmt stmt, Loop oldestAncestor) {
		Loop secondOldestAncestor = null;
		for(Loop l: loopsTree) {
			Collection<Stmt> loopStmts = l.getLoopStatements();
			if(loopStmts.contains(stmt)) {
				if(oldestAncestor != null) {
					if(oldestAncestor.getLoopStatements().equals(l.getLoopStatements())) {
						break;
					}
				}
				
				secondOldestAncestor = l;
			}
		}
		return secondOldestAncestor;
	}
	
	/**
	 * tries to figure out number of iterations of the passed loop. if the loop is
	 * nested inside other loops, it multiples parents' iterations too
	 * 
	 * @param l the loop to guess iterations of, may be null
	 * @return guessed (total) iterations
	 * @throws UnsupportedFeatureException, RuntimeException
	 */
	private int guessLoopIterationsIncludingParentLoops(Loop l) throws UnsupportedFeatureException, RuntimeException {
		int guessedIterations = 1;
		while(l != null) {
			int x = guessLoopIterations(l);
			guessedIterations = multiplyLoopIterations(guessedIterations, x);
			
			if(!(loopsTree.tailSet(l, false).size() > 0)) {
				break;
			}
			Collection<Stmt> childLoopStatements = l.getLoopStatements();
			// get parent
			l = loopsTree.tailSet(l, false).first();
			Collection<Stmt> parentLoopStatements = l.getLoopStatements();
			if(!parentLoopStatements.containsAll(childLoopStatements)) {
				break;
			}
		}
		
		return guessedIterations;
	}
	
	/**
	 * tries to figure out number of iterations of the passed loop 
	 * 
	 * NOTE: that it only tries to guess iterations of the passed loop itself, 
	 * it does not try to figure out if the loop is nested inside another one and multiply
	 * those iterations as well.
	 * 
	 * @param l the loop to guess iterations of. should not be NULL
	 * @return guessed iterations of the loop
	 * @throws UnsupportedFeatureException, RuntimeException
	 */
	private int guessLoopIterations(Loop l) throws UnsupportedFeatureException, RuntimeException {
		int guessedIterationsCount = DEFAULT_LOOP_ITERATIONS;
		
		//System.out.println("Head: " + l.getHead());
		Iterator<Stmt> stmtIt = l.getLoopStatements().iterator();
		while(stmtIt.hasNext()) {
			Stmt s = stmtIt.next();
			if(s instanceof IfStmt) {
				IfStmt ifStmt = (IfStmt) s;
				this.ifStmtOwnedByLoops.add(ifStmt);
				ConditionExpr cond = (ConditionExpr) ifStmt.getCondition();
				Value op1 = cond.getOp1();
				Value op2 = cond.getOp2();
				
				if(cond instanceof LeExpr || cond instanceof LtExpr || cond instanceof GeExpr || cond instanceof GtExpr) {
					int val1 = guessLoopUpperBoundValue(op1);
					int val2 = guessLoopUpperBoundValue(op2);
					int retval = 0;
					if(val1 > val2) {
						retval = val1;
					}
					else {
						retval = val2;
					}
					
					if(cond instanceof LtExpr && val2 == 0) {
						retval = retval + 1;
					}
					return retval;
				}
				else {
					throw new UnsupportedFeatureException("cannot handle loop condition: " + cond);
				}
			}
		}
		return guessedIterationsCount;
	}
	
	private int guessLoopUpperBoundValue(Value upperBound) throws UnsupportedFeatureException, RuntimeException {
		return guessLoopUpperBoundValue(upperBound, new HashSet<Local>());
	}
	
	private int guessLoopUpperBoundValue(Value upperBound, Set<Local> seenLocals) throws UnsupportedFeatureException, RuntimeException {
		if(Evaluator.isValueConstantValued(upperBound)) {
			Value v = Evaluator.getConstantValueOf(upperBound);
			if(v instanceof IntConstant) {
				return ((IntConstant)v).value;
			}
			else if(v instanceof LongConstant) {
				// TODO: this might truncate Long to an int
				// TODO: configure a logger and log a warning here
				return (int) ((LongConstant)v).value;
			}
			else if(v instanceof FloatConstant) {
				return (int)((FloatConstant)v).value;
			}
			else if(v instanceof DoubleConstant) {
				return (int)((DoubleConstant)v).value;
			}
		}
		else if(upperBound instanceof Local) {
			Local var = (Local)upperBound;
			if(seenLocals.contains(var)) {
				// a cycle, that means the first value of this variable comes from some other place
				// hence, this variable can't affect the upper bound anyway
				return Integer.MIN_VALUE;
			}
			seenLocals.add(var);
			
			Unit def = this.localDefs.getDefsOf(var).get(0);
			if(!(def instanceof AssignStmt)) {
				throw new UnsupportedFeatureException("definition statement can only be assign statement" +
			"if all function calls were inlined. An identity statement shouldn't appear here: " + def);
			}
			AssignStmt assignment = (AssignStmt)def;
			Value rightOp = assignment.getRightOp();
			
			if(rightOp instanceof UnopExpr) {
				Value op = ((UnopExpr)rightOp).getOp();
				int val = guessLoopUpperBoundValue(op, seenLocals);
				if(rightOp instanceof NegExpr) {
					return -val;
				}
				else {
					throw new UnsupportedFeatureException("unrecoginized unary operator: " + rightOp);
				}
			}
			else if(rightOp instanceof BinopExpr) {
				BinopExpr op = (BinopExpr)rightOp;
				Value op1 = op.getOp1();
				Value op2 = op.getOp2();
				int val1 = guessLoopUpperBoundValue(op1, seenLocals);
				int val2 = guessLoopUpperBoundValue(op2, seenLocals);
				if(val1 == Integer.MAX_VALUE || val2 == Integer.MAX_VALUE) { return Integer.MAX_VALUE; }
				if(val1 == Integer.MIN_VALUE || val2 == Integer.MIN_VALUE) { return Integer.MIN_VALUE; }
				if(op instanceof AddExpr) {
					
					return val1 + val2;
				}
				else if(op instanceof SubExpr) {
					return val1 + val2;
				}
				else if(op instanceof MulExpr) {
					return val1 * val2;
				}
				else if(op instanceof DivExpr) {
					if(val2 == 0) {
						throw new RuntimeException("Possible division by 0: " + op);
					}
					return val1 / val2;
				}
				else if(op instanceof RemExpr) {
					return val1 % val2;
				}
				else if(op instanceof EqExpr) {
					return val1 == val2 ? 1 : 0;
				}
				else if(op instanceof NeExpr) {
					return val1 != val2 ? 1 : 0;
				}
				throw new UnsupportedFeatureException("Unsupported Binary Operation: " + op);
			}
			else if(rightOp instanceof PhiExpr) {
				
				PhiExpr phi = (PhiExpr)rightOp;
				if(phi.getArgCount() != 2) {
					throw new UnsupportedFeatureException("PhiExpr has " + phi.getArgCount() + " arguments");
				}
				
				List<ValueUnitPair> args =  phi.getArgs();
				Value arg1 = args.get(0).getValue();
				Value arg2 = args.get(1).getValue();
				int val1 = guessLoopUpperBoundValue(arg1, seenLocals);
				int val2 = guessLoopUpperBoundValue(arg2, seenLocals);
				if(val1 > val2) {
					return val1;
				}
				return val2;
			}
			else {
				// may be right op is a constant, or a local, so we make recursive call
				return guessLoopUpperBoundValue(rightOp, seenLocals);
			}
			
		}
		throw new UnsupportedFeatureException("Couldn't guess number of iterations for " + upperBound + ", you might want to check the loop");
//		System.out.println("WARNING!!! Couldn't guess number of iterations for " + upperBound + ", you might want to check the loop");
//		return DEFAULT_LOOP_ITERATIONS;	
	}
	
	private static int multiplyLoopIterations(int l1, int l2) {
		if(l1 == Integer.MAX_VALUE || l2 == Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return l1 * l2;
	}
}
