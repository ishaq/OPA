package mixedProtocolsAnalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.jimple.AddExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.DivExpr;
import soot.jimple.DoubleConstant;
import soot.jimple.EqExpr;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.LongConstant;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.NegExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NumericConstant;
import soot.jimple.RemExpr;
import soot.jimple.Stmt;
import soot.jimple.SubExpr;
import soot.jimple.UnopExpr;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.scalar.Evaluator;
import soot.shimple.PhiExpr;
import soot.shimple.toolkits.scalar.ShimpleLocalDefs;
import soot.toolkits.scalar.ValueUnitPair;


public class Util {
	public static int guessConcreteValue(Value symbolicValue, ShimpleLocalDefs localDefs) throws UnsupportedFeatureException, RuntimeException{
		return guessConcreteValue(symbolicValue, new HashSet<Local>(), localDefs);
	}
	private static int guessConcreteValue(Value symbolicValue, Set<Local> seenLocals, ShimpleLocalDefs localDefs) throws UnsupportedFeatureException, RuntimeException {
		if(Evaluator.isValueConstantValued(symbolicValue)) {
			Value v = Evaluator.getConstantValueOf(symbolicValue);
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
		else if(symbolicValue instanceof Local) {
			Local var = (Local)symbolicValue;
			if(seenLocals.contains(var)) {
				// a cycle, that means the first value of this variable comes from some other place
				// hence, this variable can't affect the upper bound anyway
				//throw new UnsupportedFeatureException("can't figure out value for " + var);
				return Integer.MIN_VALUE;
			}
			seenLocals.add(var);
			
			Unit def = localDefs.getDefsOf(var).get(0);
			if(!(def instanceof AssignStmt)) {
				throw new UnsupportedFeatureException("definition statement can only be assign statement" +
			"if all function calls were inlined. An identity statement shouldn't appear here: " + def);
			}
			AssignStmt assignment = (AssignStmt)def;
			Value rightOp = assignment.getRightOp();
			
			if(rightOp instanceof UnopExpr) {
				Value op = ((UnopExpr)rightOp).getOp();
				int val = guessConcreteValue(op, seenLocals, localDefs);
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
				int val1 = guessConcreteValue(op1, seenLocals, localDefs);
				int val2;
				if(op1.equals(op2)) {
					val2 = val1;
				}
				else {
					val2 = guessConcreteValue(op2, seenLocals, localDefs);
				}
				if(val1 == Integer.MAX_VALUE || val2 == Integer.MAX_VALUE) 
				{ 
					return Integer.MAX_VALUE; 
				}
				if(val1 == Integer.MIN_VALUE || val2 == Integer.MIN_VALUE) { 
					return Integer.MIN_VALUE; 
				}
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
				int val1 = guessConcreteValue(arg1, seenLocals, localDefs);
				int val2 = guessConcreteValue(arg2, seenLocals, localDefs);
				if(val1 > val2) {
					return val1;
				}
				return val2;
			}
			else {
				// may be right op is a constant, or a local, so we make recursive call
				return guessConcreteValue(rightOp, seenLocals, localDefs);
			}
			
		}
		throw new UnsupportedFeatureException("Couldn't guess concrete value for: " + symbolicValue);
//		System.out.println("WARNING!!! Couldn't guess number of iterations for " + upperBound + ", you might want to check the loop");
//		return DEFAULT_LOOP_ITERATIONS;	
	}
	public static ArrayList<Integer> getArraySizes(Unit s, ShimpleLocalDefs localDefs)
			throws UnsupportedFeatureException, RuntimeException {
		ArrayList<Integer> sizes = getArraySizes(s, localDefs, false);
		for(int d : sizes) {
			if(d < 1) {
				throw new UnsupportedFeatureException("can't figure out array sizes for " + s.toString());
			}
		}
		return sizes;
	}
	public static ArrayList<Integer> getArraySizes(Unit s, ShimpleLocalDefs localDefs, boolean internalCall)
			throws UnsupportedFeatureException, RuntimeException {
		AssignStmt assign = (AssignStmt)s;
		if(assign.getRightOp() instanceof NewArrayExpr) {
			NewArrayExpr newArrayExpr = (NewArrayExpr)((AssignStmt)s).getRightOp();
			Value sizeVal = newArrayExpr.getSize();
			int sizeInt = guessConcreteValue(sizeVal, localDefs);
			ArrayList<Integer> sizes = new ArrayList<Integer>(1);
			sizes.add(sizeInt);
			return sizes;
		}
		else if(assign.getRightOp() instanceof NewMultiArrayExpr) {
			NewMultiArrayExpr newArrayExpr = (NewMultiArrayExpr)((AssignStmt)s).getRightOp();
			List<Value> sizeVals = newArrayExpr.getSizes();
			ArrayList<Integer> sizeInts = new ArrayList<Integer>(sizeVals.size());
			for(int i = 0; i < sizeVals.size(); i++) {
				sizeInts.add(guessConcreteValue(sizeVals.get(i), localDefs));
			}
			return sizeInts;
		}
		else if(assign.getRightOp() instanceof ArrayRef) {
			ArrayRef arrayRef = (ArrayRef)((AssignStmt)s).getRightOp();
			Local l = (Local) arrayRef.getBase();
			Unit def = localDefs.getDefsOf(l).get(0);
			ArrayList<Integer> sizes = getArraySizes(def, localDefs, true);
			// since this array is skipping one dimension, we should also skip one dimension
			sizes.remove(0);
			return sizes;
		}
		else if(assign.getLeftOp() instanceof ArrayRef) {
			// re-defn of an array, array dimension doesn't change
			ArrayRef arrayRef = (ArrayRef)((AssignStmt)s).getLeftOp();
			Local l = (Local) arrayRef.getBase();
			Unit def = localDefs.getDefsOf(l).get(0);
			ArrayList<Integer> sizes = getArraySizes(def, localDefs, true);
			return sizes;
		}
		// NOTE: array copy statement should never occur from an outside call (i.e. the check for
		// internal call) because the array def-use collection code should take care of it internally.
		// if an array copy slips through array def-use collection, it will never 
		// go away (the copyPropagation code which does simple local variable matching
		// cannot handle it properly). e.g. consider this
		//  r1 = new array (int)[10];   (1)
		//  ...
		//  r1[$i1] = $i2               (2)
		//  ...
		//
		//  r2 = r1
		//
		// the simple copyPropagation assumes r2 is a copy of r1 def (1)
		// but this is semantically wrong, r1 has two definitions (1) and (2)
		// it is a copy of the 2nd one.
		
		else if(internalCall == true
				&& assign.getLeftOp() instanceof Local // array copy statement
				&& assign.getLeftOp().getType() instanceof ArrayType
				&& assign.getRightOp() instanceof Local
				&& assign.getRightOp().getType() instanceof ArrayType) {
			Local l = (Local)assign.getRightOp();
			Unit def = localDefs.getDefsOf(l).get(0);
			ArrayList<Integer> sizes = getArraySizes(def, localDefs, true);
			return sizes;
		}
		
		throw new UnsupportedFeatureException("Unknown array init statement: " + s);
	}
	public static boolean isArrayDefStatement(Stmt stmt) {
		if((stmt instanceof AssignStmt) == false) {
			return false;
		}
		AssignStmt assign = (AssignStmt)stmt;		
		if(assign.getLeftOp() instanceof ArrayRef) {
			return true;
		}
		
		if(assign.getLeftOp().getType() instanceof ArrayType) {
			return true;
		}
	
		return false;
	}
	
	public static Local getLocalCorrespondingToArrayDefStmt(Stmt stmt) {
		if(Util.isArrayDefStatement(stmt) == false) {
			return null;
		}
		
		AssignStmt assign = (AssignStmt)stmt;
		if(assign.getLeftOp() instanceof Local) {
			Local l = (Local) assign.getLeftOp();
			return l;
		}
		else if(assign.getLeftOp() instanceof ArrayRef) {
			ArrayRef lhs = (ArrayRef)assign.getLeftOp();
			Local l = (Local)lhs.getBase();
			return l;
		}
		else {
			throw new IllegalArgumentException("Unrecognized Def Stmt: " + stmt);
		}	
	}
	
	public static boolean isArrayUseStatement(Stmt stmt) {
		if((stmt instanceof AssignStmt) == false) {
			return false;
		}
		AssignStmt assign = (AssignStmt)stmt;
		if(assign.getRightOp() instanceof ArrayRef) {
			return true;
		}
		else if(assign.getRightOp() instanceof Local && 
				assign.getRightOp().getType() instanceof ArrayType) {
			return true;
		}
		
		return false;
	}
	
	public static boolean isArrayCopyStatment(Stmt stmt) {
		if((stmt instanceof AssignStmt) == false) {
			return false;
		}
		
		AssignStmt assign = (AssignStmt)stmt;
		if(assign.getLeftOp() instanceof Local &&
			assign.getLeftOp().getType() instanceof ArrayType &&
			assign.getRightOp() instanceof Local &&
			assign.getRightOp().getType() instanceof ArrayType) {
			return true;
		}
		return false;
	}
	
	public static Local getLocalCorrespondingToArrayUseStmt(Stmt stmt) {
		if(Util.isArrayUseStatement(stmt) == false) {
			return null;
		}
		
		AssignStmt assign = (AssignStmt)stmt;
		if(assign.getRightOp() instanceof ArrayRef) {
			ArrayRef rhs = (ArrayRef)assign.getRightOp();
			Local l = (Local)rhs.getBase();
			return l;
		}
		else if(assign.getRightOp() instanceof Local && 
				assign.getRightOp().getType() instanceof ArrayType) {
			Local l = (Local)assign.getRightOp();
			return l;
		}
		else {
			throw new IllegalArgumentException("Unrecognized Def Stmt: " + stmt);
		}	
	}
		
	public static Set<Stmt> getTransitiveClosureForDef(DefUse du, Map<Stmt, DefUse> defUses, Loop parentLoop) {
		
		// corner case, when def and use are the same statement
		if(du.uses.size() == 1) {
			Stmt useStmt = du.uses.iterator().next().id;
			if(du.def.id.equals(useStmt)) {
				return new HashSet<Stmt>();
			}
		}
		
		Collection<Stmt> loopStatements = parentLoop.getLoopStatements();
		Set<Stmt> transitiveClosure = new HashSet<Stmt>();
		Queue<Node> worklist = new LinkedList<Node>();
		Set<Node> processedWorklist = new HashSet<Node>();
		worklist.addAll(du.uses);
		while(!worklist.isEmpty()) {
			Node item = worklist.poll();
			processedWorklist.add(item);
			if(!loopStatements.contains(item.id)) {
				continue;
			}
			
			DefUse currentDefUse = defUses.get(item.id);
			if(currentDefUse == null) {
				// this use is not a def
				continue;
			}
			
			transitiveClosure.add(currentDefUse.def.id);
			transitiveClosure.addAll(currentDefUse.copies);
			
			for(Node use: currentDefUse.uses) {
				if(!processedWorklist.contains(use)) {
					worklist.add(use);
				}
			}
		}
		return transitiveClosure;
	}
	
	public static Set<Local> getVariables(Value v) throws UnsupportedFeatureException {
		if(v instanceof NumericConstant) {
			return new HashSet<Local>();
		}
		else if(v instanceof Local) {
			Set<Local> vars = new HashSet<Local>();
			vars.add((Local)v);
			return vars;
		}
		else if(v instanceof ArrayRef) {
			ArrayRef ref = (ArrayRef)v;
			Set<Local> vars = new HashSet<Local>();
			vars.addAll(getVariables(ref.getBase()));
			// NOTE: For array ref, we only care about the base variable since read/write anywhere
			// in the array is considered defining/using the entire array in our analysis
//			vars.addAll(getVariables(ref.getIndex()));
			return vars;
		}
		else if(v instanceof UnopExpr) {
			if(v instanceof NegExpr) {
				Value op = ((UnopExpr)v).getOp();
				return getVariables(op);
			}
			else {
				throw new UnsupportedFeatureException("unrecoginized unary operator: " + v);
			}
		}
		else if(v instanceof BinopExpr) {
			BinopExpr op = (BinopExpr)v;
			Value op1 = op.getOp1();
			Value op2 = op.getOp2();
			Set<Local> vars = new HashSet<Local>();
			vars.addAll(getVariables(op1));
			vars.addAll(getVariables(op2));
			return vars;
		}
		else if(v instanceof PhiExpr) {
			
			PhiExpr phi = (PhiExpr)v;
			if(phi.getArgCount() != 2) {
				throw new UnsupportedFeatureException("PhiExpr has " + phi.getArgCount() + " arguments");
			}
			
			List<ValueUnitPair> args =  phi.getArgs();
			Value arg1 = args.get(0).getValue();
			Value arg2 = args.get(1).getValue();
			Set<Local> vars = new HashSet<Local>();
			vars.addAll(getVariables(arg1));
			vars.addAll(getVariables(arg2));
			return vars;
		}
		else if(v instanceof NewArrayExpr) {
			NewArrayExpr expr = (NewArrayExpr)v;
			return getVariables(expr.getSize());
		}
		// TODO: the only reason we need this case is because we are currently using
		// Invoke Expressions for MPC Annotations. Once we start doing annotations through some
		// annotation/attribute framework, this wouldn't be needed
		else if(v instanceof InvokeExpr) {
			Set<Local> vars = new HashSet<Local>();
			InvokeExpr invoke = (InvokeExpr)v;
			for(Value arg: invoke.getArgs()) {
				vars.addAll(getVariables(arg));
			}
			return vars;
		}
		throw new UnsupportedFeatureException("Can't figure out variables for " + v);
	}
	
//	public static Set<Stmt> getDefsForVariables(Set<Local> vars, ShimpleLocalDefs localDefs) {
//		Set<Stmt> defs = new HashSet<Stmt>();
//		for(Local l: vars) {
//			Unit u = localDefs.getDefsOf(l).get(0);
//			defs.add((Stmt)u);
//		}
//		return defs;
//	}
	
	
	public static Map<Local, Set<DefUse>> createDefUseMapKeyedWithLocal(Map<Stmt, DefUse> defUses) 
			throws UnsupportedFeatureException {
		Map<Local, Set<DefUse>> defUsesKeyedWithLocal = new HashMap<Local, Set<DefUse>>();
		for(Stmt key: defUses.keySet()) {
			DefUse du = defUses.get(key);
			Set<DefUse> duSet = defUsesKeyedWithLocal.get(du.var);
			if(duSet == null) {
				duSet = new HashSet<DefUse>();
			}
			duSet.add(du);
			defUsesKeyedWithLocal.put(du.var, duSet);
			
			for(Stmt copy: du.copies) {
				Set<Local> allLocals = getVariables(((AssignStmt)copy).getLeftOp());
				assert(allLocals.isEmpty() == false);
				Local copyLocal = allLocals.iterator().next();
				Set<DefUse> copyDUSet = defUsesKeyedWithLocal.get(copyLocal);
				if(copyDUSet == null) {
					copyDUSet = new HashSet<DefUse>();
				}
				copyDUSet.add(du);
				defUsesKeyedWithLocal.put(copyLocal, copyDUSet);
			}
		}
		return defUsesKeyedWithLocal;
	}
	
	// why don't I just use ShimpleLocalDefs here? because I need a definition corresponding to the passed useStmt,
	// in case the use is of an array, I need the array definition that corresponds to this use 
	// (remember, array vars have multiple defs)
	public static Stmt getDefThatCorrespondsToUse(Local local, Stmt useStmt, Map<Local, Set<DefUse>> defUseMapKeyedWithLocal) {
		Set<DefUse> defUses = defUseMapKeyedWithLocal.get(local);
		assert(defUses.isEmpty() == false);
		for(DefUse du: defUses) {
			for(Node use: du.uses) {
				if(use.id.equals(useStmt)) {
					return du.def.id;
				}
			}
		}
		return null;
	}
	
	public static Set<Stmt> getDefsThatCorrespondToUse(Set<Local> locals, Stmt useStmt, 
			Map<Local, Set<DefUse>> defUseKeyedWithLocal) {
		Set<Stmt> defs = new HashSet<Stmt>();
		for(Local l : locals) {
			Stmt du = getDefThatCorrespondsToUse(l, useStmt, defUseKeyedWithLocal);
			assert(du != null);
			defs.add(du);
		}
		return defs;
	}
	
	public static Set<Local> updateVariablesToIgnore(Set<Local> variablesToIgnore, 
			Map<Stmt, DefUse> defUses, Loop parentLoop) throws UnsupportedFeatureException {
		
		// create local to def-use mapping, it would be helpful later
		Map<Local, Set<DefUse>> defUsesKeyedWithLocal = createDefUseMapKeyedWithLocal(defUses);
		
		// now update ignored variables
		Collection<Stmt> loopStatements = parentLoop.getLoopStatements();
		Set<Local> newVariables = new HashSet<Local>();
		do {
			newVariables.clear();
			for(Local v: variablesToIgnore) {
				for(DefUse du: defUsesKeyedWithLocal.get(v)) {
					if(!loopStatements.contains(du.def.id)) {
						continue;
					}
					for(Node use: du.uses) {
						if(!loopStatements.contains(use.id)) {
							continue;
						}
						
						// is this use actually another def?
						if((use.id instanceof AssignStmt) == false) {
							// no, we don't want to keep looking at it
							continue;
						}
						
						AssignStmt assign = (AssignStmt)use.id;
						Set<Local> rightOpVars = getVariables(assign.getRightOp());
						boolean allRightOpVarsShouldBeIgnored = true;
						for(Local rightOpVar: rightOpVars) {
							
							if(!variablesToIgnore.contains(rightOpVar)) {
								allRightOpVarsShouldBeIgnored = false;
								break;
							}
						}
						
						if(allRightOpVarsShouldBeIgnored) {
							// then the lhs should also be ignored
							Set<Local> leftOpVars = getVariables(assign.getLeftOp());
							if(!variablesToIgnore.containsAll(leftOpVars)) {
								newVariables.addAll(leftOpVars);
							}
						}
					}
				}
			}
			variablesToIgnore.addAll(newVariables);
		} while(newVariables.size() > 0);
		
		return variablesToIgnore;
	}
	
	public static Set<Stmt> updateDefsToIgnore(Set<Stmt> defsToIgnore, Map<Stmt, DefUse> defUses, 
			Loop parentLoop) throws UnsupportedFeatureException {
		
		// create local to def-use mapping, it becomes handy in case of arrays, where
		// same variable may have been defined multiple times
		Map<Local, Set<DefUse>> defUseMapKeyedWithLocal = createDefUseMapKeyedWithLocal(defUses);
		
		// create a def-use map that has copies as keys too
		// so that we can get the DefUse through one of the copies as well.
		Map<Stmt, DefUse> defUseMapWithAllKeys = new HashMap<Stmt, DefUse>();
		for(Stmt key: defUses.keySet()) {
			DefUse du = defUses.get(key);
			defUseMapWithAllKeys.put(du.def.id, du);
			
			for(Stmt copy: du.copies) {
				defUseMapWithAllKeys.put(copy, du);
			}
		}
		
		// now update ignored defs
		Collection<Stmt> loopStatements = parentLoop.getLoopStatements();
		Set<Stmt> newDefs = new HashSet<Stmt>();
		do {
			newDefs.clear();
			for(Stmt s: defsToIgnore) {
				if(!loopStatements.contains(s)) {
					continue;
				}
				DefUse du = defUseMapWithAllKeys.get(s);
				assert(du != null);
				for(Node use: du.uses) {
					if(!loopStatements.contains(use.id)) {
						continue;
					}
					
					// is this use actually another def?
					if((use.id instanceof AssignStmt) == false) {
						// no, we don't want to keep looking at it
						continue;
					}
					
					AssignStmt assign = (AssignStmt)use.id;
					Set<Local> rightOpVars = getVariables(assign.getRightOp());
					Set<Stmt> rightOpVarDefs = getDefsThatCorrespondToUse(rightOpVars, use.id, defUseMapKeyedWithLocal);
					boolean allRightOpDefsShouldBeIgnored = true;
					for(Stmt thisDef: rightOpVarDefs) {
						
						if(!defsToIgnore.contains(thisDef)) {
							allRightOpDefsShouldBeIgnored = false;
							break;
						}
					}
					
					if(allRightOpDefsShouldBeIgnored) {
						// then the def should also be ignored
						if(!defsToIgnore.contains(assign)) {
							newDefs.add(assign);
						}
					}
				}
			}
			defsToIgnore.addAll(newDefs);
		} while(newDefs.size() > 0);
		
		return defsToIgnore;
	}
	
	/**
	 * returns total ordering of units from `fromUnit` (`fromUnit` is not included)
	 * @param fromUnit start building list of successors from here
	 * @param unit to successor map
	 * @return ordered list, suitable for use in subsumption (see paper).
	 */
	public static List<Unit> getTotalOrdering(Unit fromUnit, Body body) {
		// NOTE: very important to build unit-to-successors map here (instead of in the caller)
		// Reason: if you build it in the caller and pass it via a parameter here, the order of 
		// successors is altered.
		Map<Unit, List<Unit>> succsMap = buildUnitToSuccessorsMap(body);
		
		Stack<Unit> worklist = new Stack<Unit>();
		List<Unit> succsList = new LinkedList<Unit>();
		
		
		// successors list is ordered s.t. the fall-through node is the very first.
		// we reverse the so that the 'farther' node (a branch target) is first
		// then we push. this makes the farther node go deeper in the stack
		List<Unit> fromSuccessors = getSuccsOf(fromUnit, succsMap);
		Collections.reverse(fromSuccessors);
		for(Iterator<Unit> iter = fromSuccessors.iterator(); iter.hasNext();) {
			worklist.push(iter.next());
		}
		
		while(!worklist.isEmpty()) {
			Unit item = worklist.pop();
			succsList.add(item);
			
			// successors list is ordered s.t. the fall-through node is the very first.
			// we reverse the so that the 'farther' node (a branch target) is first
			// then we push. this makes the farther node go deeper in the stack
			List<Unit> successors = getSuccsOf(item, succsMap);
			Collections.reverse(successors);
			for(Iterator<Unit> iter = successors.iterator(); iter.hasNext();) {
				Unit currSuccessor = iter.next();
				if(succsList.contains(currSuccessor) || worklist.contains(currSuccessor)) {
					continue;
				}
				worklist.push(currSuccessor);
			}
		}
		return succsList;
	}
	
	/**
	 * builds a map for each units successors. it guarantees that 'fall through' node (when one 
	 * exists) is always the first successor in the successors list.
	 * 
	 * this code is based on BriefUnitGraph. I wrote it because BriefUnitGraph doesn't make 
	 * any guarantees for order of getSuccsOf (just that same order would always be 
	 * returned). We wanted a guarantee that 'fall through' instruction
	 * is always the first successor.
	 * 
	 * @param b Body of the function
	 * @return map keys are units, values are successors of the unit
	 */
	public static Map<Unit, List<Unit>> buildUnitToSuccessorsMap(Body b) {
		Map<Unit, List<Unit>> unitToSuccs = new HashMap<Unit, List<Unit>>();
		
		Iterator<Unit> unitIt = b.getUnits().iterator();
		Unit currentUnit = null;
		Unit nextUnit = null;
		
		nextUnit = unitIt.hasNext() ? (Unit) unitIt.next() : null;

		while (nextUnit != null) {
			currentUnit = nextUnit;
			nextUnit = unitIt.hasNext() ? (Unit) unitIt.next() : null;
			
			ArrayList<Unit> successors = new ArrayList<Unit>();
			if(currentUnit.fallsThrough()) {
				if(nextUnit != null) {
					successors.add(nextUnit);
				}
			}
			
			if (currentUnit.branches()) {
				for (UnitBox targetBox : currentUnit.getUnitBoxes()) {
					Unit target = targetBox.getUnit();
					// Arbitrary bytecode can branch to the same
					// target it falls through to, so we screen for duplicates:
					if (!successors.contains(target)) {
						successors.add(target);
					}
				}
			}

			// Store away successors
			if (!successors.isEmpty()) {
				successors.trimToSize();
				unitToSuccs.put(currentUnit, successors);
			}
			
		}
		
		return unitToSuccs;
	}
	
	public static List<Unit> getSuccsOf(Unit unit, Map<Unit, List<Unit>> succsMap) {
		List<Unit> succs = succsMap.get(unit);
		if(succs == null) {
			return Collections.emptyList();
		}
		return succs;
	}
}
