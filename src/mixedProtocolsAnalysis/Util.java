package mixedProtocolsAnalysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import soot.ArrayType;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NegExpr;
import soot.jimple.NumericConstant;
import soot.jimple.Stmt;
import soot.jimple.UnopExpr;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ValueUnitPair;

public class Util {
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
		
	public static Set<Local> getTransitiveClosureForDef(DefUse du, Map<Stmt, DefUse> defUses, Loop parentLoop) {
		
		// corner case:
		if(du.uses.size() == 1) {
			Stmt useStmt = du.uses.iterator().next().id;
			if(du.def.id.equals(useStmt)) {
				return new HashSet<Local>();
			}
		}
		
		Collection<Stmt> loopStatements = parentLoop.getLoopStatements();
		Set<Local> transitiveClosure = new HashSet<Local>();
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
			
			transitiveClosure.add(currentDefUse.var);
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
		// TODO: the only reason we need this case is because we are currently using
		// Invoke Expressions for MUX. Once we start supporting if/else statements, this
		// should be removed since our code doesn't expect calls
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
	
	public static Set<Local> updateVariablesToIgnore(Set<Local> variablesToIgnore, 
			Map<Stmt, DefUse> defUses, Loop parentLoop) throws UnsupportedFeatureException {
		
		// create local to def-use mapping, it would be helpful later
		Map<Local, Set<DefUse>> defUsesKeyedWithLocal = new HashMap<Local, Set<DefUse>>();
		for(Stmt key: defUses.keySet()) {
			DefUse du = defUses.get(key);
			Set<DefUse> duSet = defUsesKeyedWithLocal.get(du.var);
			if(duSet == null) {
				duSet = new HashSet<DefUse>();
			}
			duSet.add(du);
			defUsesKeyedWithLocal.put(du.var, duSet);
			
			for(Local copy: du.copies) {
				Set<DefUse> copyDUSet = defUsesKeyedWithLocal.get(copy);
				if(copyDUSet == null) {
					copyDUSet = new HashSet<DefUse>();
				}
				copyDUSet.add(du);
				defUsesKeyedWithLocal.put(copy, copyDUSet);
			}
		}
		
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
}
