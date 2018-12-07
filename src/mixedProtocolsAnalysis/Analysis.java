package mixedProtocolsAnalysis;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import mixedProtocolsAnalysis.Node.NodeType;
import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.shimple.ShimpleBody;
import soot.shimple.toolkits.scalar.ShimpleLocalDefs;
import soot.shimple.toolkits.scalar.ShimpleLocalUses;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.toolkits.graph.LoopNestTree;
import soot.toolkits.scalar.UnitValueBoxPair;

public class Analysis extends BodyTransformer {

	private class LocalSerializer implements JsonSerializer<Local> {
		public JsonElement serialize(Local src, Type typeOfSrc, JsonSerializationContext ctx) {
			return new JsonPrimitive(src.toString());
		}
	}

	private class StmtSerializer implements JsonSerializer<Stmt> {
		protected Map<Unit, Integer> nodeToIndex;

		public StmtSerializer(Map<Unit, Integer> nodeToIndex) {
			this.nodeToIndex = nodeToIndex;
		}

		public JsonElement serialize(Stmt src, Type typeOfSrc, JsonSerializationContext ctx) {
			Integer nodeIndex = nodeToIndex.get(src);
			JsonObject obj = new JsonObject();
			obj.addProperty("index", nodeIndex);
			obj.addProperty("unit", src.toString());
			return obj;
		}
	}

	private class NodeSerializer implements JsonSerializer<Node> {
		public JsonElement serialize(Node src, Type typeOfSrc, JsonSerializationContext ctx) {
			JsonObject obj = new JsonObject();
			TypeToken<Stmt> stmtType = new TypeToken<Stmt>() {
			};
			obj.add("id", ctx.serialize(src.getId(), stmtType.getType()));
			obj.addProperty("weight", src.getWeight());
			obj.addProperty("line_number", src.getLineNumber());
			TypeToken<Node.NodeType> nodeTypeType = new TypeToken<Node.NodeType>() {
			};
			obj.add("node_type", ctx.serialize(src.getNodeType(), nodeTypeType.getType()));
			obj.addProperty("conversion_weight", src.getConversionWeight());
			obj.add("conversion_point", ctx.serialize(src.getConversionPoint(), stmtType.getType()));
			obj.addProperty("parallelizable", src.isParallelizable());
			return obj;
		}
	}

	private class DefUseSerializer implements JsonSerializer<DefUse> {
		public JsonElement serialize(DefUse src, Type typeOfSrc, JsonSerializationContext ctx) {
			JsonObject obj = new JsonObject();
			TypeToken<Local> localType = new TypeToken<Local>() {
			};
			obj.add("var", ctx.serialize(src.getVar(), localType.getType()));
			TypeToken<Set<Local>> copiesType = new TypeToken<Set<Local>>() {
			};
			obj.add("copies", ctx.serialize(src.getCopies(), copiesType.getType()));
			TypeToken<Node> nodeType = new TypeToken<Node>() {
			};
			obj.add("def", ctx.serialize(src.getDef(), nodeType.getType()));
			TypeToken<List<Node>> nodesListType = new TypeToken<List<Node>>() {
			};
			obj.add("uses", ctx.serialize(src.getUses(), nodesListType.getType()));
			return obj;
		}
	}
	
	private class LoopSerializer implements JsonSerializer<Loop> {
		public JsonElement serialize(Loop src, Type typeOfSrc, JsonSerializationContext ctx) {
			JsonObject obj = new JsonObject();
			TypeToken<Stmt> stmtType = new TypeToken<Stmt>() {
			};
			obj.add("head", ctx.serialize(src.getHead(), stmtType.getType()));
			//obj.add("loopTail", ctx.serialize(src.get));
			TypeToken<Collection<Stmt>> stmtListType = new TypeToken<Collection<Stmt>>() {
				
			};
			obj.add("exits", ctx.serialize(src.getLoopExits(), stmtListType.getType()));
			return obj;
		}
	}

	static final String ANALYSIS_JSON_FILENAME = "analysis.json";

	protected Map<SootMethod, Map<Stmt, DefUse>> methodDefUses = new HashMap<SootMethod, Map<Stmt, DefUse>>();
	protected Map<SootMethod, Set<Loop>> methodNonParallelLoops = new HashMap<SootMethod, Set<Loop>>();

	private Map<Unit, Integer> nodeToIndex = null;
	private ArrayList<Unit> nodes;

	private void buildNodesToIndexLookup(Body body) {
		final PatchingChain<Unit> units = body.getUnits();
		this.nodes = new ArrayList<Unit>(units.size());
		this.nodeToIndex = new HashMap<Unit, Integer>();
		for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
			final Unit u = iter.next();
			nodes.add(u);
			nodeToIndex.put(u, nodes.size() - 1);
		}

//		for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
//			final Unit u = iter.next();
//			System.out.println("u: " + u + "i: " + nodeToIndex.get(u) + " u: " + nodes.get(nodeToIndex.get(u)));
//		}
	}

	/**
	  * Called back from the Soot framework. Traverses method Body calling .apply
	  * (i.e., .accept) on each statement.
	  * 
	  * @see soot.BodyTransformer#internalTransform(soot.Body, java.lang.String,
	  * java.util.Map)
	  */
	@Override
	protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
		try {
			SootMethod m = body.getMethod();
			System.out.println("inside method: " + m.getSignature());
			if (m.isMain()) {
				// NOTE: this lookup used in serialization, the linear program (MATLAB) uses indices to uniquely 
				// identify the nodes, I thought indices were easier/less-error-prone than using string representation
				// of "id" field.
				buildNodesToIndexLookup(body);
				
				// printBodyInfo(body);
				
				// TODO: scan body for call instructions, throw an exception if there is one
				
				Map<Stmt, DefUse> defUses = collectDefUses(body);
				defUses = doCopyPropagation(defUses);
				Map<Stmt, DefUse> arrayDefUses = collectArrayDefUses(body);
				
				// merge update def-use map
				Map<Stmt, DefUse> defUsesWithFixedArrayDefUse = new HashMap<Stmt, DefUse>();
				defUsesWithFixedArrayDefUse.putAll(defUses);
				defUsesWithFixedArrayDefUse.putAll(arrayDefUses);
				
				defUsesWithFixedArrayDefUse = updateUseOrder(body, defUsesWithFixedArrayDefUse);
				// TODO: conversion points and weights should be assigned according to subsumption
				defUsesWithFixedArrayDefUse = adjustWeigthsOfDefUses(body, defUsesWithFixedArrayDefUse);
				defUsesWithFixedArrayDefUse = assignLineNumbersToDefUses(body, defUsesWithFixedArrayDefUse);
				
				Set<Loop> nonParallelLoops = getNonParallelizableLoops(body, defUsesWithFixedArrayDefUse);
				defUsesWithFixedArrayDefUse = updateParallelizationAttribute(body, nonParallelLoops, 
						defUsesWithFixedArrayDefUse);
				
				methodDefUses.put(m, defUsesWithFixedArrayDefUse);
				methodNonParallelLoops.put(m, nonParallelLoops);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void showResult() {

		if (!(methodDefUses.keySet().size() > 0)) {
			throw new RuntimeException("the results map is empty");
		}

		System.out.println("---------------------------DEF USE----------------------------");
		for (SootMethod m : methodDefUses.keySet()) {
			System.out.println("method: " + m.getSignature());
			Map<Stmt, DefUse> defUses = methodDefUses.get(m);
			for (Stmt stmt : defUses.keySet()) {
				DefUse thisDefUse = defUses.get(stmt);
				System.out.println("    " + thisDefUse.def.id + " copies: " + thisDefUse.getCopies() + "");
				System.out.println("     def: " + thisDefUse.getDef());
				for (Node use : thisDefUse.getUses()) {
					System.out.println("     use: " + use);
				}

			}
		}

		try (Writer writer = new FileWriter(ANALYSIS_JSON_FILENAME)) {
			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.registerTypeAdapter(Local.class, new LocalSerializer());
			gsonBuilder.registerTypeAdapter(Stmt.class, new StmtSerializer(this.nodeToIndex));
			gsonBuilder.registerTypeAdapter(Node.class, new NodeSerializer());
			gsonBuilder.registerTypeAdapter(DefUse.class, new DefUseSerializer());
			gsonBuilder.registerTypeAdapter(Loop.class, new LoopSerializer());
			// TODO: pretty printing can be turned off
			gsonBuilder.setPrettyPrinting();
			
			Gson gson = gsonBuilder.create();
			SootMethod defUseKey = methodDefUses.keySet().iterator().next();
			Map<Stmt, DefUse> defUses = methodDefUses.get(defUseKey);
			SootMethod nonParallelLoopKey = methodNonParallelLoops.keySet().iterator().next();
			Set<Loop> nonParallelLoops = methodNonParallelLoops.get(nonParallelLoopKey);
			
			Map<String, Object> analysis = new HashMap<String, Object>();
			analysis.put("def_use", defUses);
			analysis.put("nonparallel_loops", nonParallelLoops);
			gson.toJson(analysis, writer);
			//gson.toJson(defUses, writer);
			System.out.println("Def/Use analysis written to: " + (new java.io.File(".")).getCanonicalPath() + "/" + ANALYSIS_JSON_FILENAME);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected static void printBodyInfo(Body body) throws Exception {
		SootMethod m = body.getMethod();
		System.out.println("**** Method: " + m.getSignature() + " ****");

		System.out.println("**** Body ****");
		System.out.println(body);
		System.out.println();

		System.out.println("**** Blocks ****");
		BlockGraph blockGraph = new ExceptionalBlockGraph(body);
		for (Block block : blockGraph.getBlocks()) {
			System.out.println(block);
		}
		System.out.println();
		System.out.println("**** Loops ****");
		LoopNestTree loopNestTree = new LoopNestTree(body);
		for (Loop loop : loopNestTree) {
			System.out.println("Found a loop with head: " + loop.getHead());
			// System.out.println(loop.getLoopStatements());
		}

		if ((body instanceof ShimpleBody) == false) {
			throw new Exception("Not a shimple body");
		}

		// System.out.println("**** Defs ****");
		// ShimpleBody sb = (ShimpleBody)body;
		// ShimpleLocalDefs localDefs = new ShimpleLocalDefs(sb);
		// ShimpleLocalUses localUses = new ShimpleLocalUses(sb);
		// for(Local local: sb.getLocals()) {
		// System.out.println("**** " + local + " ****");
		// System.out.println(localDefs.getDefsOf(local));
		// System.out.println(localUses.getUsesOf(local));
		// System.out.println();
		// }
	}

	protected static Map<Stmt, DefUse> collectDefUses(Body body) throws IllegalArgumentException {
		if ((body instanceof ShimpleBody) == false) {
			throw new IllegalArgumentException("Not a shimple body");
		}

		ShimpleBody sb = (ShimpleBody) body;
		ShimpleLocalDefs localDefs = new ShimpleLocalDefs(sb);
		ShimpleLocalUses localUses = new ShimpleLocalUses(sb);

		Map<Stmt, DefUse> defUses = new HashMap<Stmt, DefUse>();

		for (Local local : sb.getLocals()) {
			List<Unit> defs = localDefs.getDefsOf(local);
			@SuppressWarnings("unchecked")
			List<UnitValueBoxPair> uses = localUses.getUsesOf(local);

			// System.out.println("---- " + local + " -> " + defs.get(0));
			DefUse du = new DefUse(local, (Stmt) defs.get(0));
			for (UnitValueBoxPair u : uses) {
				// System.out.println("<- unit: " + u.getUnit());
				// System.out.println("<- value box: " + u.getValueBox());
				Node use = new Node((Stmt) u.getUnit());
				du.addUse(use);
			}

			defUses.put(du.def.id, du);
		}

		return defUses;
	}

	protected static Map<Stmt, DefUse> collectArrayDefUses(Body body) throws IllegalArgumentException {
		if ((body instanceof ShimpleBody) == false) {
			throw new IllegalArgumentException("Not a shimple body");
		}

		Map<Stmt, DefUse> arrayDefUses = new HashMap<Stmt, DefUse>();
		BriefUnitGraph cfg = new BriefUnitGraph(body);
		Set<Unit> handledArrayDefs = new HashSet<Unit>();
		final PatchingChain<Unit> units = body.getUnits();
		for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
			final Unit u = iter.next();
			Stmt s = (Stmt) u;
			Local l = Util.getLocalCorrespondingToArrayDefStmt(s);
			if (l != null) {
				if(handledArrayDefs.contains(u)) {
					continue;
				}
				
				DefUse thisDefUse = new DefUse(l, s);
				arrayDefUses.put(thisDefUse.def.id, thisDefUse);
				Set<Local> localsToMatch = new HashSet<Local>();
				localsToMatch.add(thisDefUse.var);
				List<Unit> defSuccessors = cfg.getSuccsOf(thisDefUse.def.id);
				Queue<Unit> worklist = new LinkedList<Unit>(defSuccessors);
				Set<Unit> worklistProcessed = new HashSet<Unit>();
				while (worklist.isEmpty() == false) {
					Unit item = worklist.poll();
					worklistProcessed.add(item);
					Local useL = Util.getLocalCorrespondingToArrayUseStmt((Stmt) item);
					Local defL = Util.getLocalCorrespondingToArrayDefStmt((Stmt) item);
					if (useL != null) {
						if (localsToMatch.contains(useL)) {
							// we don't record use for copies
							if(Util.isArrayCopyStatment((Stmt)item) == false) {
								// this is not a copy
								Node use = new Node((Stmt) item);
								thisDefUse.addUse(use);
							}
							
							if (defL != null) {
								// if we are also defining a new array from
								// the use of the current array, then the new array
								// is a copy (or sub-array) of this one
								handledArrayDefs.add(item);
								localsToMatch.add(defL);
								thisDefUse.copies.add(defL);
							}
							

						}
					} else if (defL != null) {
						if (localsToMatch.contains(defL)) {
							// we have reached a re-definition of the current array
							// any successors from here on will use the new def, therefore
							// we should stop here
							continue;
						}
					}
					List<Unit> itemSuccessors = cfg.getSuccsOf(item);
					for (Unit currentSuccessor : itemSuccessors) {
						if (worklist.contains(currentSuccessor) || worklistProcessed.contains(currentSuccessor)) {
							continue;
						}
						worklist.add(currentSuccessor);
					}
				}

			}
		}
		
		return arrayDefUses;
	}

	protected static Map<Stmt, DefUse> adjustWeigthsOfDefUses(Body body, Map<Stmt, DefUse> defUses) throws Exception {
		LoopHelper loopHelper = new LoopHelper(body);
		for (Stmt stmt : defUses.keySet()) {
			DefUse thisDefUse = defUses.get(stmt);
			for (Node use : thisDefUse.getUses()) {
				loopHelper.setDefUseWeights(thisDefUse.def, use);
			}
		}

		Set<IfStmt> loopIfs = loopHelper.getIfStmtsOwnedByLoops();
		for (Stmt stmt : defUses.keySet()) {
			DefUse thisDefUse = defUses.get(stmt);
			if (loopIfs.contains(thisDefUse.def.id)) {
				// since this if belongs to a loop, it is merely setting the weight of
				// other nodes within the loop. This is not a comparison node per se.
				// therefore, need to set type to OTHER
				thisDefUse.def.nodeType = NodeType.OTHER;
			}
			for (Node use : thisDefUse.getUses()) {
				if (loopIfs.contains(use.id)) {
					use.nodeType = NodeType.OTHER;
				}
			}
		}

		return defUses;
	}

	protected static Map<Stmt, DefUse> assignLineNumbersToDefUses(Body body, Map<Stmt, DefUse> defUses) {
		int i = 0;
		Map<Unit, Integer> lineNumbers = new HashMap<Unit, Integer>();
		final PatchingChain<Unit> units = body.getUnits();
		for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
			final Unit u = iter.next();
			lineNumbers.put(u, i + 1);
			++i;
		}

		for (Stmt key : defUses.keySet()) {
			DefUse du = defUses.get(key);
			du.def.lineNumber = lineNumbers.get(du.def.id);
			for (Node dui : du.getUses()) {
				dui.lineNumber = lineNumbers.get(dui.id);
			}
		}

		return defUses;
	}

	/**
	 * takes code that looks like this 
	 *  temp = y_1 - z_1
	 *  x = temp 
	 *  y = x
	 * 
	 * and turn it into
	 * 
	 * y = y_1 - z_1 (gets rid of x and temp)
	 * 
	 * NOTE: right now only does it in the analysis (no code is transformed)
	 * 
	 * @param defUses map containing def and uses
	 * @return updated map of def uses, copies are marked
	 */
	protected static Map<Stmt, DefUse> doCopyPropagation(Map<Stmt, DefUse> defUses) {
		Set<Stmt> toRemove = new HashSet<Stmt>();
		Queue<Stmt> worklist = new LinkedList<Stmt>(defUses.keySet());

		while (!worklist.isEmpty()) {
			Stmt key = worklist.poll();
			DefUse du = defUses.get(key);

			if ((du.def.id instanceof AssignStmt) == false) {
				continue;
			}
			AssignStmt assignStmt = (AssignStmt) du.def.id;
			if ((assignStmt.getRightOp() instanceof Local) == false) {
				continue;
			}
			Local rightOpLocal = (Local)assignStmt.getRightOp();
			DefUse rightOpDefUse = null;
			// find def use corresponding to the right side local
			for(Stmt rightKey: defUses.keySet()) {
				DefUse thisDefUse = defUses.get(rightKey);
				if(thisDefUse.var.equals(rightOpLocal)) {
					rightOpDefUse = thisDefUse;
					break;
				}
			}

			rightOpDefUse.markCopy(du);
			if (!worklist.contains(rightOpDefUse.def.id)) {
				worklist.add(rightOpDefUse.def.id);
			}
			toRemove.add(key);
		}

		for (Stmt stmt : toRemove) {
			defUses.remove(stmt);
		}
		return defUses;
	}
	
	protected static Set<Loop> getNonParallelizableLoops(Body body, Map<Stmt, DefUse> defUses) 
			throws UnsupportedFeatureException {
		Set<Loop> nonParallelLoops = new HashSet<Loop>();
		LoopHelper loopHelper = new LoopHelper(body);
		Map<Loop, Set<Local>> loopVars = loopHelper.getLoopCounterVariables(defUses);
		Set<Loop> prevLoops = new HashSet<Loop>();
		for(Loop l: loopHelper.getLoopsTree()) {
			Collection<Stmt> loopStatements = l.getLoopStatements();
			Set<Local> varsToIgnore = loopVars.get(l);
			for(Loop prevLoop: prevLoops) {
				Collection<Stmt> prevLoopStatements = prevLoop.getLoopStatements();
				if(loopStatements.containsAll(prevLoopStatements)) {
					varsToIgnore.addAll(loopVars.get(prevLoop));
				}
			}
			for(Stmt key: defUses.keySet()) {
				if(loopStatements.contains(key) == false) {
					continue;
				}
				DefUse du = defUses.get(key);
				// TODO: new logic for ignored variables
				if(varsToIgnore.contains(du.var)) {
					continue;
				}
				Set<Local> tc = Util.getTransitiveClosureForDef(du, defUses, l);
				if(tc.contains(du.var)) {
					varsToIgnore.addAll(tc);
					varsToIgnore = Util.updateVariablesToIgnore(varsToIgnore, defUses, l);
					nonParallelLoops.add(l);
				}
			}
			loopVars.put(l, varsToIgnore);
			prevLoops.add(l);
		}
		
		return nonParallelLoops;
	}
	
	protected static Map<Stmt, DefUse> updateParallelizationAttribute(Body body, Set<Loop> nonParallelLoops, 
			Map<Stmt, DefUse> defUses) throws UnsupportedFeatureException {
		LoopHelper loopHelper = new LoopHelper(body);
		Set<Stmt> allKeys = defUses.keySet();
		for(Stmt key: allKeys) {
			DefUse du = defUses.get(key);
			Loop defLoop = loopHelper.getImmediateParentLoop(du.def.id);
			if(defLoop != null) {
				du.def.setParallelizable(!nonParallelLoops.contains(defLoop));
			}
			else {
				// outside of any loop, therefore, no parallelizable
				du.def.setParallelizable(false);
			}
			
			for(Node use: du.uses) {
				Loop useLoop = loopHelper.getImmediateParentLoop(use.id);
				if(useLoop != null) {
					use.setParallelizable(!nonParallelLoops.contains(useLoop));
				}
				else {
					use.setParallelizable(false);
				}
			}
		}
		return defUses;
	}
	
	public class UnitIndexComparator implements Comparator<Unit> {
		@Override
		public int compare(Unit o1, Unit o2) {
			return nodeToIndex.get(o1).compareTo(nodeToIndex.get(o2));
		}
	}
	
	protected Map<Stmt, DefUse> updateUseOrder(Body body, Map<Stmt, DefUse> defUses) {
		BriefUnitGraph cfg = new BriefUnitGraph(body);
		Set<Stmt> keys = defUses.keySet();
		for(Stmt k: keys) {
			DefUse du = defUses.get(k);
			Map<Stmt, Node> useMap = new HashMap<Stmt, Node>();
			for(Node use: du.getUses()) {
				useMap.put(use.id, use);
			}
			
			Stack<Unit> worklist = new Stack<Unit>();
			Set<Unit> processedWorklist = new HashSet<Unit>();
			Set<Stmt> usesThatHaveBeenAssignedOrder = new HashSet<Stmt>();
			
			List<Unit> defSuccessors = cfg.getSuccsOf(du.def.id);
			Collections.sort(defSuccessors, new UnitIndexComparator());
			// push so that the closest (in terms distance in lines) gets pushed last
			// so 1. reverse and then 2. push
			Collections.reverse(defSuccessors);
			for(Iterator<Unit> iter = defSuccessors.iterator(); iter.hasNext();) {
				worklist.push(iter.next());
			}
			
			int rank = 0;
			while(!worklist.isEmpty()) {
				Unit item = worklist.pop();
				processedWorklist.add(item);
				Node use = useMap.get((Stmt)item);
				if(use != null) {
					use.setUseOrder(rank);
					usesThatHaveBeenAssignedOrder.add((Stmt)use.id);
					
					if(usesThatHaveBeenAssignedOrder.size() == du.getUses().size()) {
						// found rank for all uses
						break;
					}
					
					rank += 1;
				}
				
				List<Unit> successors = cfg.getSuccsOf(item);
				Collections.sort(successors, new UnitIndexComparator());
				// push so that the closest (in terms distance in lines) gets pushed last
				// so 1. reverse and then 2. push
				Collections.reverse(successors);
				for(Iterator<Unit> iter = successors.iterator(); iter.hasNext();) {
					Unit currSuccessor = iter.next();
					if(processedWorklist.contains(currSuccessor) || worklist.contains(currSuccessor)) {
						continue;
					}
					worklist.push(currSuccessor);
				}
			}
			
			assert(usesThatHaveBeenAssignedOrder.size() == du.getUses().size());
		}
		
		return defUses;
	}

}
