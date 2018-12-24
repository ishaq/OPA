package mixedProtocolsAnalysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.ConditionExpr;
import soot.jimple.GeExpr;
import soot.jimple.GtExpr;
import soot.jimple.IfStmt;
import soot.jimple.LeExpr;
import soot.jimple.LtExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.shimple.ShimpleBody;
import soot.shimple.toolkits.scalar.ShimpleLocalDefs;
import soot.toolkits.graph.LoopNestTree;

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
	
	public void setConversionPoint(Node def, Node use) throws UnsupportedFeatureException, RuntimeException {
		Loop defLoop = getImmediateParentLoop(def.id);
		//def.weight = guessLoopIterationsIncludingParentLoops(defLoop);
		
		Loop useLoop = getImmediateParentLoop(use.id);
		//use.weight = guessLoopIterationsIncludingParentLoops(useLoop);
		
		Loop ancestor = getCommonAncestor(defLoop, useLoop);
		Loop secondOldestAncestorOfUse = getSecondOldestAncestor(use.id, ancestor);
		if(secondOldestAncestorOfUse == null) {
			use.setConversionPoint(use.id);
		}
		else {
			use.setConversionPoint(secondOldestAncestorOfUse.getHead());
		}
		
		//use.conversionWeight = guessLoopIterationsIncludingParentLoops(ancestor);				
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
	
	public Loop getImmediateParentLoop(Loop l) {
		Loop currentLoop = l;
		while(true) {
			SortedSet<Loop> tailSet = loopsTree.tailSet(currentLoop, false);
			if(!(tailSet.size() > 0)) {
				return null;
			}
			
			Collection<Stmt> childLoopStatements = currentLoop.getLoopStatements();
			// get potential parent
			Loop potentialParent = tailSet.first();
			Collection<Stmt> parentLoopStatements = potentialParent.getLoopStatements();
			if(parentLoopStatements.containsAll(childLoopStatements)) {
				return potentialParent;
			}
			// else
			currentLoop = potentialParent;
		}
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
				
				for(Local l1: condVariables) {
					Unit u = this.localDefs.getDefsOf(l1).get(0);
					AssignStmt defStatement = (AssignStmt)u;
					Set<Local> defVariables = Util.getVariables(defStatement.getRightOp());
					thisLoopCounterVars.addAll(defVariables);
				}
				break;
			}
		}
		
		thisLoopCounterVars = Util.updateVariablesToIgnore(thisLoopCounterVars, defUses, l);
		return thisLoopCounterVars;
	}
	
	public Set<Stmt> getAllLoopCounterVariableDefStmts(Map<Stmt, DefUse> defUses)  throws UnsupportedFeatureException {
		Set<Stmt> loopCounterVarDefs = new HashSet<Stmt>();
		for(Loop l: this.loopsTree) {
			Set<Local> vars = getLoopCounterVariables(l, defUses);
			for(Local v: vars) {
				Unit u = localDefs.getDefsOf(v).get(0);
				loopCounterVarDefs.add((Stmt)u);
			}
		}
		return loopCounterVarDefs;
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
			
			l = getImmediateParentLoop(l);
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
	public int guessLoopIterations(Loop l) throws UnsupportedFeatureException, RuntimeException {
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
		
		if(guessedIterationsCount == DEFAULT_LOOP_ITERATIONS) {
			throw new UnsupportedFeatureException("loop count either too large or can't be guessed");
		}
		
		return guessedIterationsCount;
	}
	
	private int guessLoopUpperBoundValue(Value upperBound) throws UnsupportedFeatureException, RuntimeException {
		return Util.guessConcreteValue(upperBound, this.localDefs);
	}
	
	
	
	private static int multiplyLoopIterations(int l1, int l2) {
		if(l1 == Integer.MAX_VALUE || l2 == Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return l1 * l2;
	}
}
