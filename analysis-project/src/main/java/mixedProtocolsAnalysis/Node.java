package mixedProtocolsAnalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import soot.Unit;
import soot.UnitBox;
import soot.Local;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AddExpr;
import soot.jimple.AndExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BreakpointStmt;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.ClassConstant;
import soot.jimple.CmpExpr;
import soot.jimple.CmpgExpr;
import soot.jimple.CmplExpr;
import soot.jimple.Constant;
import soot.jimple.DivExpr;
import soot.jimple.DoubleConstant;
import soot.jimple.DynamicInvokeExpr;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.EqExpr;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.FloatConstant;
import soot.jimple.GeExpr;
import soot.jimple.GotoStmt;
import soot.jimple.GtExpr;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LeExpr;
import soot.jimple.LengthExpr;
import soot.jimple.LongConstant;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.LtExpr;
import soot.jimple.MethodHandle;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.NegExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.OrExpr;
import soot.jimple.ParameterRef;
import soot.jimple.RemExpr;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.ShlExpr;
import soot.jimple.ShrExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.SubExpr;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThisRef;
import soot.jimple.ThrowStmt;
import soot.jimple.UshrExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.XorExpr;
import soot.shimple.AbstractShimpleValueSwitch;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DominatorsFinder;
import soot.toolkits.graph.MHGDominatorsFinder;

/**
 * represents a "def" node or a "use" node
 * 
 * @author ishaq
 *
 */
public class Node {
	/**
	 * The values, CONSTANT and LOCAL, are only used here internally to figure out
	 * when a node is a SIMPLE_ASSIGN type. These two values (i.e. CONSTANT and
	 * LOCAL) will never appear in the JSON output and the node type will always be
	 * SIMPLE_ASSIGN. Therefore, no need to handle these in MATLAB linear program
	 * 
	 * @author ishaq
	 *
	 */
	public static enum NodeType {
	OTHER(0),
	// AbstractStmtSwitch
	SIMPLE_ASSIGN(1),
	// AbstractConstantSwitch
	CONSTANT(101),
	// AbstractJimpleValueSwitch
	ADD(201), AND(202), CMP(203), DIV(204), EQ(205), GE(206), GT(207), LE(208), LT(209), MUL(210), NE(211), OR(212),
	REM(213), SHL(214), SHR(215), SUB(216), USHR(217), XOR(218), NEG(219), LOCAL(220),
	// AbstractShimpleValueSwitch
	MUX(301),

	// Special nodes
	MPC_ANNOTATION_INSTANTIATION(1000), IN(1001), OUT(1002), INVALID_NODE(2000), PSEUDO_PHI(2001);

		private final int value;

		private NodeType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public static class NodeStmtSwitch extends AbstractStmtSwitch {
		private NodeType nodeType = NodeType.OTHER;
		private IfStmt associatedCondition = null;
		private BriefUnitGraph cfg;

		public NodeStmtSwitch(BriefUnitGraph cfg) {
			this.cfg = cfg;
		}

		public NodeType getNodeType() {
			return nodeType;
		}

		public void caseBreakpointStmt(BreakpointStmt stmt) {
			defaultCase(stmt);
		}

		public void caseInvokeStmt(InvokeStmt stmt) {
			NodeValueSwitch visitor = new NodeValueSwitch(cfg);
			stmt.getInvokeExpr().apply(visitor);
			nodeType = visitor.getNodeType();
			associatedCondition = visitor.associatedCondition;
		}

		public void caseAssignStmt(AssignStmt stmt) {
			NodeValueSwitch visitor = new NodeValueSwitch(cfg);
			stmt.getRightOp().apply(visitor);
			nodeType = visitor.getNodeType();
			associatedCondition = visitor.associatedCondition;
			if (nodeType == NodeType.LOCAL || nodeType == NodeType.CONSTANT) {
				// if RHS is a simple value or reference (not an expression)
				nodeType = NodeType.SIMPLE_ASSIGN;
			}

		}

		public void caseIdentityStmt(IdentityStmt stmt) {
			nodeType = NodeType.SIMPLE_ASSIGN;
		}

		public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
			defaultCase(stmt);
		}

		public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
			defaultCase(stmt);
		}

		public void caseGotoStmt(GotoStmt stmt) {
			nodeType = NodeType.OTHER;
		}

		public void caseIfStmt(IfStmt stmt) {
			NodeValueSwitch visitor = new NodeValueSwitch(cfg);
			stmt.getCondition().apply(visitor);
			nodeType = visitor.getNodeType();
			associatedCondition = visitor.associatedCondition;
		}

		public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
			defaultCase(stmt);
		}

		public void caseNopStmt(NopStmt stmt) {
			defaultCase(stmt);
		}

		public void caseRetStmt(RetStmt stmt) {
			defaultCase(stmt);
		}

		public void caseReturnStmt(ReturnStmt stmt) {
			NodeValueSwitch visitor = new NodeValueSwitch(cfg);
			stmt.getOp().apply(visitor);
			nodeType = visitor.getNodeType();
			associatedCondition = visitor.associatedCondition;
		}

		public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
			nodeType = NodeType.OTHER;
		}

		public void caseTableSwitchStmt(TableSwitchStmt stmt) {
			defaultCase(stmt);
		}

		public void caseThrowStmt(ThrowStmt stmt) {
			defaultCase(stmt);
		}

		public void defaultCase(Object obj) {
			System.out.println("Unhandled stmt: " + obj + " of type: " + obj.getClass());
			nodeType = NodeType.INVALID_NODE;
		}
	}

	public static class NodeValueSwitch extends AbstractShimpleValueSwitch {
		private NodeType nodeType = NodeType.OTHER;
		private BriefUnitGraph cfg = null;
		private IfStmt associatedCondition = null;

		public NodeValueSwitch(BriefUnitGraph cfg) {
			this.cfg = cfg;
		}

		public NodeType getNodeType() {
			return nodeType;
		}

		// From AbstractShimpleValueSwitch
		public void casePhiExpr(PhiExpr v) {
			List<UnitBox> unitBoxes = v.getUnitBoxes();
			if(unitBoxes.size() != 2)  {
				System.out.println("we only support 2 arguments for Phi Node. " 
						+ v + " has " + unitBoxes.size());
				nodeType = NodeType.INVALID_NODE;
			}
			
			DominatorsFinder<Unit> df = new MHGDominatorsFinder<Unit>(cfg);
			Unit first = unitBoxes.get(0).getUnit();
			Unit second = unitBoxes.get(1).getUnit();
			
			
			IfStmt divergenceCondition = null;
			
			Unit firstRunner = first,
					secondRunner = second;
			while(firstRunner != null && secondRunner != null) {
				firstRunner = df.getImmediateDominator(firstRunner);
				if(firstRunner instanceof IfStmt && df.isDominatedBy(second, firstRunner)) { // TODO: check there is no other if statement between
					if(isThereAnIfConditionBefore(df, firstRunner, second)) {
						divergenceCondition = null; 
					}
					else {
						divergenceCondition = (IfStmt)firstRunner;
					}
					break;
				}
				
				secondRunner = df.getImmediateDominator(secondRunner);
				if(secondRunner instanceof IfStmt && df.isDominatedBy(first, secondRunner)) { // TODO: check there is no other if statement between
					// we do not handle nested ifs
					if(isThereAnIfConditionBefore(df, secondRunner, first)) {
						divergenceCondition = null; 
					}
					else {
						divergenceCondition = (IfStmt)secondRunner;
					}
					break;
				}
			}
			
			if(divergenceCondition != null) {
				// We found the divergence condition. This Phi node is probably a MUX.
				// NOTE: this Phi node can still be a pseudo-phi node, this is checked later by 
				// checking if all variables in the associated condition are loop counters
				// But at this point we can't do better than marking it as 'probable' MUX
				this.associatedCondition = divergenceCondition;
				this.nodeType = NodeType.MUX;
			}
			else {
				// we we can't find a divergenceCondition that dominates both the arguments to Phi,
				// then we are dealing with a Phi that merges a loop counter. Since this Phi belong to a loop
				// (instead of an if/else), this Phi is not a MUX (in the paper we call these pseudo-phi nodes).
				this.nodeType = NodeType.PSEUDO_PHI;
			}
		}
		
		private boolean isThereAnIfConditionBefore(DominatorsFinder<Unit> df, Unit theIfStmt, Unit dominatee) {
			Unit runner = dominatee;
			while(runner != null) {
				runner = df.getImmediateDominator(runner);
				if(runner.equals(theIfStmt)) {
					return false;
				}
				if(runner instanceof IfStmt) {
					return true;
				}
			}
			
			return false;
		}

		// From AbstractJimpleValueSwitch
		public void caseArrayRef(ArrayRef v) {
			nodeType = NodeType.LOCAL;
		}

		public void caseAddExpr(AddExpr v) {
			nodeType = NodeType.ADD;
		}

		public void caseAndExpr(AndExpr v) {
			nodeType = NodeType.AND;
		}

		public void caseCmpExpr(CmpExpr v) {
			nodeType = NodeType.CMP;
		}

		public void caseCmpgExpr(CmpgExpr v) {
			defaultCase(v);
		}

		public void caseCmplExpr(CmplExpr v) {
			defaultCase(v);
		}

		public void caseDivExpr(DivExpr v) {
			nodeType = NodeType.DIV;
		}

		public void caseEqExpr(EqExpr v) {
			nodeType = NodeType.EQ;
		}

		public void caseGeExpr(GeExpr v) {
			nodeType = NodeType.GE;
		}

		public void caseGtExpr(GtExpr v) {
			nodeType = NodeType.GT;
		}

		public void caseLeExpr(LeExpr v) {
			nodeType = NodeType.LE;
		}

		public void caseLtExpr(LtExpr v) {
			nodeType = NodeType.LT;
		}

		public void caseMulExpr(MulExpr v) {
			nodeType = NodeType.MUL;
		}

		public void caseNeExpr(NeExpr v) {
			nodeType = NodeType.NE;
		}

		public void caseOrExpr(OrExpr v) {
			nodeType = NodeType.OR;
		}

		public void caseRemExpr(RemExpr v) {
			nodeType = NodeType.REM;
		}

		public void caseShlExpr(ShlExpr v) {
			nodeType = NodeType.SHL;
		}

		public void caseShrExpr(ShrExpr v) {
			nodeType = NodeType.SHR;
		}

		public void caseSubExpr(SubExpr v) {
			nodeType = NodeType.SUB;
		}

		public void caseUshrExpr(UshrExpr v) {
			nodeType = NodeType.USHR;
		}

		public void caseXorExpr(XorExpr v) {
			nodeType = NodeType.XOR;
		}

		public void caseInterfaceInvokeExpr(InterfaceInvokeExpr v) {
			// FIXME: these checks should be improved. instead of comparing to hard-coded
			// signatures,
			// these should do something smarter
			if (v.getMethod().getSignature().equals("<MPCAnnotation: int IN()>")) {
				nodeType = NodeType.IN;
				return;
			} else if (v.getMethod().getSignature().equals("<MPCAnnotation: void OUT(int)>")) {
				nodeType = NodeType.OUT;
				return;
			}
			defaultCase(v);
		}

		public void caseSpecialInvokeExpr(SpecialInvokeExpr v) {
			defaultCase(v);
		}

		public void caseStaticInvokeExpr(StaticInvokeExpr v) {
			// FIXME: these checks should be improved, instead of comparing to hard-coded
			// signature,
			// these should do something smarter.
			if (v.getMethodRef().getSignature().equals("<MPCAnnotationImpl: MPCAnnotation v()>")) {
				nodeType = NodeType.MPC_ANNOTATION_INSTANTIATION;
				return;
			}
			defaultCase(v);
		}

		public void caseVirtualInvokeExpr(VirtualInvokeExpr v) {
			defaultCase(v);
		}

		public void caseDynamicInvokeExpr(DynamicInvokeExpr v) {
			defaultCase(v);
		}

		public void caseCastExpr(CastExpr v) {
			defaultCase(v);
		}

		public void caseInstanceOfExpr(InstanceOfExpr v) {
			defaultCase(v);
		}

		public void caseNewArrayExpr(NewArrayExpr v) {
			nodeType = NodeType.OTHER;
		}

		public void caseNewMultiArrayExpr(NewMultiArrayExpr v) {
			nodeType = NodeType.OTHER;
		}

		public void caseNewExpr(NewExpr v) {
			defaultCase(v);
		}

		public void caseLengthExpr(LengthExpr v) {
			defaultCase(v);
		}

		public void caseNegExpr(NegExpr v) {
			nodeType = NodeType.NEG;
		}

		public void caseInstanceFieldRef(InstanceFieldRef v) {
			defaultCase(v);
		}

		public void caseLocal(Local v) {
			nodeType = NodeType.LOCAL;
		}

		public void caseParameterRef(ParameterRef v) {
			defaultCase(v);
		}

		public void caseCaughtExceptionRef(CaughtExceptionRef v) {
			defaultCase(v);
		}

		public void caseThisRef(ThisRef v) {
			defaultCase(v);
		}

		public void caseStaticFieldRef(StaticFieldRef v) {
			defaultCase(v);
		}

		// From AbstractConstantSwitch
		public void caseDoubleConstant(DoubleConstant v) {
			constantCase(v);
		}

		public void caseFloatConstant(FloatConstant v) {
			constantCase(v);
		}

		public void caseIntConstant(IntConstant v) {
			constantCase(v);
		}

		public void caseLongConstant(LongConstant v) {
			constantCase(v);
		}

		public void caseNullConstant(NullConstant v) {
			constantCase(v);
		}

		public void caseStringConstant(StringConstant v) {
			constantCase(v);
		}

		public void caseClassConstant(ClassConstant v) {
			constantCase(v);
		}

		public void caseMethodHandle(MethodHandle v) {
			constantCase(v);
		}

		public void constantCase(Constant v) {
			nodeType = NodeType.CONSTANT;
		}

		public void defaultCase(Object obj) {
			System.out.println("Unhandled expr: " + obj + " of type: " + obj.getClass());
			nodeType = NodeType.INVALID_NODE;
		}

	}

	/**
	 * the instruction corresponding to the node, used to identify the node (both
	 * def and use)
	 */
	protected Stmt id;

	protected NodeType nodeType = NodeType.OTHER;
	/**
	 * if nodeType is 'MUX', then it would have an associated condition that determines 
	 * which argument of Phi gets chosen. for all other cases, this condition is null
	 */
	protected IfStmt associatedCondition = null;

	/**
	 * line number corresponding to the node, mostly for debugging eas
	 */
	protected int lineNumber;

	/**
	 * weight of this node
	 */
	protected int weight = 1;

	/**
	 * represents rank of a use (if this node is a "use" node.).
	 * 
	 * use orders are used to sort the use nodes to correctly identify min-cut for
	 * conversions and compute subsumption as appropriate
	 */
	protected int useOrder = -1;

	/**
	 * the instruction before which conversion (if any) for def-use edge should be
	 * placed. this field is only defined if this object represents a "use", it is
	 * null for a "def"
	 */
	protected Stmt conversionPoint = null;

	// TODO: currently we keep both these variables here for convenience,
	// but once we start exporting all nodes (currently we only export def/uses that
	// we need, not all of them), we should get rid of these
	protected int conversionWeight = 1;
	protected int conversionParallelParam = 1;

	/**
	 * parallelization parameter, this is less than or equal to weight and indicates
	 * how many executions (of weight) may be in parallel.
	 */
	protected int parallelParam = 1;

	/**
	 * contains the array dimensions, this is only applicable if this node
	 * represents definition of an array
	 */
	protected ArrayList<Integer> arrayDimensions = null;

	public Node(Stmt stmt, BriefUnitGraph cfg) {
		this.id = stmt;
		NodeStmtSwitch visitor = new NodeStmtSwitch(cfg);
		stmt.apply(visitor);
		this.nodeType = visitor.getNodeType();
		this.associatedCondition = visitor.associatedCondition;
	}

	public Stmt getId() {
		return id;
	}

	public NodeType getNodeType() {
		return nodeType;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public int getWeight() {
		return weight;
	}

	public Stmt getConversionPoint() {
		return conversionPoint;
	}

	public int getUseOrder() {
		return useOrder;
	}

	public void setUseOrder(int useOrder) {
		this.useOrder = useOrder;
	}

	public void setConversionPoint(Stmt conversionPoint) {
		this.conversionPoint = conversionPoint;
	}

	public int getParallelParam() {
		return parallelParam;
	}

	public void setParallelParam(int parallelParam) {
		this.parallelParam = parallelParam;
	}
	
	/**
	 * @return associated condition (valid only if the nodeType is MUX)
	 */
	public IfStmt getAssociatedCondition() {
		return this.associatedCondition;
	}

	public int getArrayWeight() {
		if (arrayDimensions == null) {
			return 1;
		}
		int w = 1;
		for (int d : arrayDimensions) {
			w = w * d;
		}
		return w;
	}

	@Override
	public int hashCode() {
		// hash code is the same as the instruction it encapsulates
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != getClass())
			return false;
		Node other = (Node) o;
		return (id.equals(other.id));
	}

	public String toString() {
		String repr = "(instruction = " + lineNumber + ":" + id + ", type:" + nodeType + ", order: " + useOrder
				+ ", weight = " + weight + ", arrayWeight: " + getArrayWeight() + ", parallelWeight: " + parallelParam + ")";
		if(associatedCondition != null) {
			repr = "(instruction = " + lineNumber + ":" + id + ", type:" + nodeType + ", order: " + useOrder
					+ ", weight = " + weight + ", arrayWeight: " + getArrayWeight() + ", parallelWeight: " + parallelParam + ")"
					+ " associatedCondition: " + associatedCondition.getCondition();
		}
		return repr;

	}
}
