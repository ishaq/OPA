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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import mixedProtocolsAnalysis.Node.NodeType;
import soot.ArrayType;
import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.BooleanType;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocal;
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
			obj.addProperty("array_weight", src.getArrayWeight());
			obj.addProperty("line_number", src.getLineNumber());
			TypeToken<Node.NodeType> nodeTypeType = new TypeToken<Node.NodeType>() {
			};
			obj.add("node_type", ctx.serialize(src.getNodeType(), nodeTypeType.getType()));
			
			if(src.getConversionPoint() != null) {
				obj.add("conversion_point", ctx.serialize(src.getConversionPoint(), stmtType.getType()));
				obj.addProperty("conversion_weight", src.conversionWeight);
				obj.addProperty("conversion_parallel_param", src.conversionParallelParam);
			}
			obj.addProperty("parallel_param", src.getParallelParam());
			obj.addProperty("order", src.getUseOrder());
			return obj;
		}
	}

	private class DefUseSerializer implements JsonSerializer<DefUse> {
		public JsonElement serialize(DefUse src, Type typeOfSrc, JsonSerializationContext ctx) {
			JsonObject obj = new JsonObject();
			TypeToken<Local> localType = new TypeToken<Local>() {
			};
			obj.add("var", ctx.serialize(src.getVar(), localType.getType()));
			TypeToken<Set<Stmt>> copiesType = new TypeToken<Set<Stmt>>() {
			};
			obj.add("copies", ctx.serialize(src.getCopies(), copiesType.getType()));
			TypeToken<Node> nodeType = new TypeToken<Node>() {
			};
			obj.add("def", ctx.serialize(src.getDef(), nodeType.getType()));
			TypeToken<List<Node>> nodesListType = new TypeToken<List<Node>>() {
			};
			//obj.add("uses", ctx.serialize(src.getUses(), nodesListType.getType()));
			
			// sort uses by order
			List<Node> uses = new ArrayList<Node>();
			uses.addAll(src.uses);
			Collections.sort(uses, new UseComparator());
			
			obj.add("uses", ctx.serialize(uses, nodesListType.getType()));
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
	
	private class LoopInfoSerializer implements JsonSerializer<LoopInfo> {
		public JsonElement serialize(LoopInfo src, Type typeOfSrc, JsonSerializationContext ctx) {
			JsonObject obj = new JsonObject();
			TypeToken<Loop> loopType = new TypeToken<Loop>() {
			};
			obj.add("loop", ctx.serialize(src.loop, loopType.getType()));
			obj.addProperty("parallel_param", src.parallelParam);
			obj.addProperty("weight", src.weight);
			obj.addProperty("iterations_count", src.iterationsCount);
			return obj;
		}
	}
	
	public class UseComparator implements Comparator<Node> {
		@Override
		public int compare(Node o1, Node o2) {
			return Integer.compare(o1.useOrder, o2.useOrder);
		}
	}

	static final String ANALYSIS_JSON_FILENAME = "analysis.json";

	protected Map<SootMethod, Map<Stmt, DefUse>> methodDefUses = 
			new HashMap<SootMethod, Map<Stmt, DefUse>>();
	protected Map<SootMethod, Map<Loop, LoopInfo>> methodLoopInfo = 
			new HashMap<SootMethod, Map<Loop, LoopInfo>>();

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
				
				verifyAllNodesAreValid(body);
				
				// NOTE: this lookup used in serialization, the linear program (MATLAB) uses indices to uniquely 
				// identify the nodes, I thought indices were easier/less-error-prone than using string representation
				// of "id" field.
				buildNodesToIndexLookup(body);
				
				// printBodyInfo(body);
				
				Map<Stmt, DefUse> defUses = collectDefUses(body);
				patchDefUsesForMUXNodes(body, defUses);
				Map<Stmt, DefUse> arrayDefUses = collectArrayDefUses(body);
				defUses = doCopyPropagation(defUses);
				
				// merge update def-use map
				Map<Stmt, DefUse> defUsesWithFixedArrayDefUse = new HashMap<Stmt, DefUse>();
				defUsesWithFixedArrayDefUse.putAll(defUses);
				defUsesWithFixedArrayDefUse.putAll(arrayDefUses);
				defUses = defUsesWithFixedArrayDefUse;
				
				defUses = updateUseOrder(body, defUses, nodeToIndex);
				defUses = removeUsesThatOccurAfterRedefinition(body, defUses, 
						nodeToIndex);
				defUses = setDefUseConversionPoints(body, defUses);
				defUses = assignLineNumbersToDefUses(body, defUses);
				
				Map<Loop, LoopInfo> loopInfo = gatherLoopParallelizationInfo(body, defUses);
				defUses = updateWeightAndParallelParam(body, loopInfo, 
						defUses);
				
				// Once we have done everything else, we get rid of def/uses corresponding to loops. 
				// it is important that this is done last because previous steps may make use of those def/uses
				defUses = removeDefUsesForLoopCountersVars((ShimpleBody)body, defUses);
				defUses = finalizeOutput(defUses);
				
				methodDefUses.put(m, defUses);
				methodLoopInfo.put(m, loopInfo);
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
				System.out.println("    " + thisDefUse.def.id + " local: " + thisDefUse.var + " copies: " + thisDefUse.getCopies() + "");
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
			gsonBuilder.registerTypeAdapter(LoopInfo.class, new LoopInfoSerializer());
			// TODO: pretty printing can be turned off
			gsonBuilder.setPrettyPrinting();
			
			Gson gson = gsonBuilder.create();
			SootMethod defUseKey = methodDefUses.keySet().iterator().next();
			Map<Stmt, DefUse> defUses = methodDefUses.get(defUseKey);
			SootMethod loopInfoKey = methodLoopInfo.keySet().iterator().next();
			Map<Loop, LoopInfo> loopInfo = methodLoopInfo.get(loopInfoKey);
			
			Map<String, Object> analysis = new HashMap<String, Object>();
			analysis.put("def_use", defUses);
			analysis.put("loop_info", loopInfo);
			gson.toJson(analysis, writer);
			//gson.toJson(defUses, writer);
			System.out.println("Def/Use analysis written to: " + (new java.io.File(".")).getCanonicalPath() + "/" + ANALYSIS_JSON_FILENAME);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected static void verifyAllNodesAreValid(Body body) throws UnsupportedFeatureException {
		final PatchingChain<Unit> units = body.getUnits();
		BriefUnitGraph cfg = new BriefUnitGraph(body);
		for(Unit u: units) {
			Node n = new Node((Stmt)u, cfg);
			if(n.nodeType == Node.NodeType.INVALID_NODE) {
				throw new UnsupportedFeatureException("Can't handle the node: " + u);
			}
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
		BriefUnitGraph cfg = new BriefUnitGraph(sb);
		ShimpleLocalDefs localDefs = new ShimpleLocalDefs(sb);
		ShimpleLocalUses localUses = new ShimpleLocalUses(sb);

		Map<Stmt, DefUse> defUses = new HashMap<Stmt, DefUse>();

		for (Local local : sb.getLocals()) {
			List<Unit> defs = localDefs.getDefsOf(local);
			@SuppressWarnings("unchecked")
			List<UnitValueBoxPair> uses = localUses.getUsesOf(local);

			// System.out.println("---- " + local + " -> " + defs.get(0));
			DefUse du = new DefUse(local, (Stmt) defs.get(0), cfg);
			for (UnitValueBoxPair u : uses) {
				// System.out.println("<- unit: " + u.getUnit());
				// System.out.println("<- value box: " + u.getValueBox());
				Node use = new Node((Stmt) u.getUnit(), cfg);
				du.addUse(use);
			}

			defUses.put(du.def.id, du);
		}

		return defUses;
	}
	
	protected static void patchDefUsesForMUXNodes(Body body, Map<Stmt, DefUse> defUses) 
			throws UnsupportedFeatureException {
		ShimpleLocalDefs localDefs = new ShimpleLocalDefs((ShimpleBody)body);
		BriefUnitGraph cfg = new BriefUnitGraph(body);
		LoopHelper lh = new LoopHelper(body);
		Set<Local> loopVars = lh.getAllLoopCounterVariables(defUses);
		
		Map<Stmt, DefUse> newDefUses = new HashMap<Stmt, DefUse>();
		for(Stmt key: defUses.keySet()) {
			DefUse du = defUses.get(key);
			patchDefUsesForMUXNodesHelper(du.def, localDefs, cfg, loopVars, defUses, newDefUses);
			for(Node use: du.getUses()) {
				patchDefUsesForMUXNodesHelper(use, localDefs, cfg, loopVars, defUses, newDefUses);
			}
		}
		
		defUses.putAll(newDefUses);
		return;
	}
	
	protected static void patchDefUsesForMUXNodesHelper(Node node, ShimpleLocalDefs localDefs, BriefUnitGraph cfg, 
			Set<Local> loopVars, Map<Stmt, DefUse> defUses, Map<Stmt, DefUse> newDefUses) 
			throws UnsupportedFeatureException {
		if(node.nodeType == NodeType.MUX) {
			assert(node.associatedCondition != null);
			Set<Local> condLocals = Util.getVariables(node.associatedCondition.getCondition());
			if(loopVars.containsAll(condLocals)) {
				node.nodeType = Node.NodeType.PSEUDO_PHI; // only depends on loop counters, therefore, pseudo-phi
			}
			
			DefUse newDU = newDefUses.get(node.associatedCondition);
			if(newDU == null) {
				Local flag = new JimpleLocal(node.associatedCondition.getCondition().toString(), BooleanType.v());
				newDU = new DefUse(flag, node.associatedCondition, cfg);
				newDefUses.put(newDU.def.id, newDU);
			}
			
			// MUX is a use of this def
			newDU.addUse(node);
		}
	}

	protected static Map<Stmt, DefUse> collectArrayDefUses(Body body) 
			throws UnsupportedFeatureException, IllegalArgumentException {
		if ((body instanceof ShimpleBody) == false) {
			throw new IllegalArgumentException("Not a shimple body");
		}
		
		ShimpleLocalDefs localDefs = new ShimpleLocalDefs((ShimpleBody) body);

		Map<Stmt, DefUse> arrayDefUses = new HashMap<Stmt, DefUse>();
		BriefUnitGraph cfg = new BriefUnitGraph(body);
		Set<Unit> handledArrayDefs = new HashSet<Unit>();
		final PatchingChain<Unit> units = body.getUnits();
		for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
			final Unit u = iter.next();
			Stmt s = (Stmt) u;
			Local l = Util.getLocalCorrespondingToArrayDefStmt(s);
			if (l != null && Util.isArrayCopyStatment(s) == false) { // array copies shall be handled below 
				if(handledArrayDefs.contains(u)) {
					continue;
				}
				
				DefUse thisDefUse = new DefUse(l, s, cfg);
				thisDefUse.def.arrayDimensions = Util.getArraySizes(u, localDefs);
				
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
							if(Util.isArrayCopyStatment((Stmt)item)) { // if this is a copy, we record it as a copy
								thisDefUse.copies.add((Stmt)item);
								localsToMatch.add(defL);
							}
							else {
								Node use = new Node((Stmt) item, cfg);
								thisDefUse.addUse(use);
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
		
		// now order the uses
		
		return arrayDefUses;
	}
	
	protected static Map<Stmt, DefUse> setDefUseConversionPoints(Body body, Map<Stmt, DefUse> defUses) throws Exception {
		LoopHelper loopHelper = new LoopHelper(body);
		for (Stmt stmt : defUses.keySet()) {
			DefUse thisDefUse = defUses.get(stmt);
			for (Node use : thisDefUse.getUses()) {
				loopHelper.setConversionPoint(thisDefUse.def, use);
			}
		}

		// TODO: need to find better home for this chunk of code
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
	 * NOTE:
	 *  (1) it can't handle array copies (but this is not an issue since array def-use collection takes care of copies 
	 *  inside itself) 
	 * 	(2) right now only does it in the analysis (no code is transformed)
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
			
			if((assignStmt.getLeftOp() instanceof Local) == false) {
				continue;
			}
			
			if ((assignStmt.getRightOp() instanceof Local) == false) {
				continue;
			}
			if((assignStmt.getLeftOp().getType() instanceof ArrayType) || 
					assignStmt.getRightOp().getType() instanceof ArrayType) {
				// if we have found an array copy statement, we just remove it (we assume
				// this would be taken care of by the array def-use collection code anyway).
				toRemove.add(du.def.id);
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
	
	public static class LoopInfo {
		// primary attrs 
		public Loop loop;
		public int iterationsCount = 1;
		public boolean isParallel = false;
		
		// derived attrs
		public int parallelParam = 1;
		public int weight = 1;
		
		public LoopInfo(Loop loop, int iterationsCount, boolean isParallel) {
			this.loop = loop;
			this.iterationsCount = iterationsCount;
			this.isParallel = isParallel;
			
			weight = iterationsCount;
			parallelParam = (isParallel) ? iterationsCount:1;
		}
	}
	
	protected static Map<Loop, LoopInfo> gatherLoopParallelizationInfo(Body body, Map<Stmt, DefUse> defUses) 
			throws UnsupportedFeatureException {
		Set<Loop> nonParallelLoops = new HashSet<Loop>();
		LoopHelper loopHelper = new LoopHelper(body);
		Map<Loop, Set<Stmt>> loopDefs = loopHelper.getLoopCounterVariableDefStmts(defUses);
		Set<Loop> prevLoops = new HashSet<Loop>();
		for(Loop l: loopHelper.getLoopsTree()) {
			Collection<Stmt> loopStatements = l.getLoopStatements();
			Set<Stmt> defsToIgnore = loopDefs.get(l);
			for(Loop prevLoop: prevLoops) {
				Collection<Stmt> prevLoopStatements = prevLoop.getLoopStatements();
				if(loopStatements.containsAll(prevLoopStatements)) {
					defsToIgnore.addAll(loopDefs.get(prevLoop));
				}
			}
			for(Stmt key: defUses.keySet()) {
				if(loopStatements.contains(key) == false) {
					continue;
				}
				DefUse du = defUses.get(key);
				if(defsToIgnore.contains(du.def.id)) {
					continue;
				}
				Set<Stmt> tc = Util.getTransitiveClosureForDef(du, defUses, l);
				if(tc.contains(du.def.id)) {
					defsToIgnore.addAll(tc);
					defsToIgnore = Util.updateDefsToIgnore(defsToIgnore, defUses, l);
					nonParallelLoops.add(l);
				}
			}
			loopDefs.put(l, defsToIgnore);
			prevLoops.add(l);
		}
		
		Map<Loop, LoopInfo> loopInfo = new HashMap<Loop, LoopInfo>();
		for(Loop l: loopHelper.getLoopsTree()) {
			int iterationsCount = loopHelper.guessLoopIterations(l);
			boolean isParallel = !(nonParallelLoops.contains(l));
			loopInfo.put(l, new LoopInfo(l, iterationsCount, isParallel));
		}
		
		for(Loop l: loopHelper.getLoopsTree()) {
			LoopInfo li = loopInfo.get(l);
			Loop parentLoop = loopHelper.getImmediateParentLoop(l);
			while(parentLoop != null) {
				LoopInfo pli = loopInfo.get(parentLoop);
				li.weight = li.weight * pli.iterationsCount;
				if(pli.isParallel) {
					li.parallelParam = li.parallelParam * pli.iterationsCount;
				}
				
				parentLoop = loopHelper.getImmediateParentLoop(parentLoop);
			}
		}
		return loopInfo;
	}
	
	protected static Map<Stmt, DefUse> updateWeightAndParallelParam(Body body, Map<Loop, LoopInfo> loopInfo, 
			Map<Stmt, DefUse> defUses) throws UnsupportedFeatureException {
		LoopHelper loopHelper = new LoopHelper(body);
		Set<Stmt> allKeys = defUses.keySet();
		for(Stmt key: allKeys) {
			DefUse du = defUses.get(key);
			Loop defLoop = loopHelper.getImmediateParentLoop(du.def.id);
			if(defLoop != null) {
				LoopInfo li = loopInfo.get(defLoop);
				du.def.setParallelParam(li.parallelParam);
				du.def.setWeight(li.weight);
			}
			else {
				// TODO:
				// outside of any loop. Currently we consider this non parallelizable. This is not strictly true,
				// since multiple nodes on the same depth should ideally still run in parallel. But analyzing that is part
				// of future work (when we implement this stuff against something better e.g. CBMC-GC)
				du.def.setParallelParam(1);
				du.def.setWeight(1);
			}
			
			for(Node use: du.uses) {
				Loop useLoop = loopHelper.getImmediateParentLoop(use.id);
				if(useLoop != null) {
					LoopInfo li = loopInfo.get(useLoop);
					use.setParallelParam(li.parallelParam);
					use.setWeight(li.weight);
				}
				else {
					use.setParallelParam(1);
					use.setWeight(1);
				}
				
				// TODO: this is redundant, but currently we need to keep it here because we don't export all nodes
				Loop conversionLoop = loopHelper.getImmediateParentLoop(use.conversionPoint);
				if(conversionLoop != null) {
					LoopInfo li = loopInfo.get(conversionLoop);
					use.conversionParallelParam = li.parallelParam;
					use.conversionWeight = li.weight;
				}
				else {
					use.conversionParallelParam = 1;
					use.conversionWeight = 1;
				}
			}
			
		}
		return defUses;
	}
	
	protected static Map<Stmt, DefUse> updateUseOrder(Body body, Map<Stmt, DefUse> defUses, 
			Map<Unit, Integer> nodeToIndex) {
		Set<Stmt> keys = defUses.keySet();
		for(Stmt k: keys) {
			DefUse du = defUses.get(k);
			Map<Stmt, Node> useMap = new HashMap<Stmt, Node>();
			for(Node use: du.getUses()) {
				useMap.put(use.id, use);
			}
			
			Set<Node> usesThatHaveBeenAssignedOrder = new HashSet<Node>();
			List<Unit> defSuccessors = Util.getTotalOrdering(du.def.id, body);
			
			int order = 0;
			Iterator<Unit> iter = defSuccessors.iterator();
			//System.out.println(k.toString());
			while(iter.hasNext()) {
				Unit s = iter.next();
				Node use = useMap.get((Stmt)s);
				if(use != null) {
					//System.out.println("\t" + indx + ": " + order + ": " + s);
					use.setUseOrder(order);
					usesThatHaveBeenAssignedOrder.add(use);
					
					if(usesThatHaveBeenAssignedOrder.equals(du.getUses())) {
						// found rank for all uses
						break;
					}
					
					order += 1;
				}
			}
			assert(usesThatHaveBeenAssignedOrder.equals(du.getUses()));
		}
		
		return defUses;
	}
	
	public static class DefUseComparator implements Comparator<DefUse> {
		Map<Unit, Integer> nodeToIndex;
		DefUseComparator(Map<Unit, Integer> nodeToIndex) {
			this.nodeToIndex = nodeToIndex;
		}
		@Override
		public int compare(DefUse o1, DefUse o2) {
			return nodeToIndex.get(o1.def.id).compareTo(nodeToIndex.get(o2.def.id));
		}
	}
	
	protected static Map<Stmt, DefUse> removeUsesThatOccurAfterRedefinition(Body body, Map<Stmt, DefUse> defUses,
			Map<Unit, Integer> nodeToIndex) {
		
		Map<Local, List<DefUse>> varDefUseMap = new HashMap<Local, List<DefUse>>();
		
		DefUseComparator comp = new DefUseComparator(nodeToIndex);
		
		// build the map
		for(Stmt key: defUses.keySet()) {
			DefUse du = defUses.get(key);
			ArrayList<DefUse> existingList = (ArrayList<DefUse>) varDefUseMap.get(du.var);
			if(existingList == null) {
				existingList = new ArrayList<DefUse>();
				varDefUseMap.put(du.var, existingList);
			}
			existingList.add(du);
		}
		
		// go through the map
		for(Local key: varDefUseMap.keySet()) {
			List<DefUse> defUseList = varDefUseMap.get(key);
			if(defUseList.size() < 2) {
				continue;
			}
			
			// if multiple defs for same var
			Collections.sort(defUseList, comp);
			
			for(int i = 1; i < defUseList.size(); i++) {
				// remove all uses of the later def (that occur after the later def).
				DefUse prevDefUse = defUseList.get(i-1);
				Set<Node> prevUses = prevDefUse.uses;
				DefUse currDefUse = defUseList.get(i);
				int currDefIndex = nodeToIndex.get(currDefUse.def.id);
				Set<Stmt> currUsesIDs = new HashSet<Stmt>();
				for(Node u: currDefUse.uses) {
					currUsesIDs.add(u.id);
				}
				
				Set<Node> toRemove = new HashSet<Node>();
				for(Node u: prevUses) {
					if(nodeToIndex.get(u.id) > currDefIndex && currUsesIDs.contains(u.id)) {
						toRemove.add(u);
					}
				}
				prevUses.removeAll(toRemove);
			}
		}
		
		// any changes would automatically have been reflected in original defUses
		return defUses;
	}
	
	protected static Map<Stmt, DefUse> removeDefUsesForLoopCountersVars(ShimpleBody body, Map<Stmt, DefUse> defUses)
			throws UnsupportedFeatureException {
		LoopHelper lh = new LoopHelper(body);
		Set<Stmt> loopDefs = lh.getAllLoopCounterVariableDefStmts(defUses);
		for(Stmt key: loopDefs) {
			assert(defUses.containsKey(key));
			defUses.remove(key);
		}
		return defUses;
	}
	
	protected static Map<Stmt, DefUse> finalizeOutput(Map<Stmt, DefUse> defUses) {		
		Set<Stmt> toRemove = new HashSet<Stmt>();
		for(Stmt key: defUses.keySet()) {
			DefUse du = defUses.get(key);
			
			if(du.uses.size() == 0) { // no need to include defs that have no uses
				toRemove.add(key);
			}
			else if(du.uses.size() == 1) { // if def and use are the same instruction, it is dead code
				// technically such def/use should not exist to begin with, I don't know whether it's a bug in Soot
				// or Soot needs to do multiple passes (and we are only doing one) to get rid of such artifacts
				Node use = du.uses.iterator().next();
				if(du.def.id.equals(use.id)) {
					toRemove.add(key);
				}
			}
			
			if(du.def.getNodeType() == Node.NodeType.MPC_ANNOTATION_INSTANTIATION) {
				// this variable is just an instantiation of MPC Annotation utility,
				// there is no need to output its def-use
				toRemove.add(key);
			}
		}
		for(Stmt item: toRemove) {
			DefUse du = defUses.get(item);
			System.out.println("Removing defuse: " + du);
			defUses.remove(item);
		}
		return defUses;
	}
}
