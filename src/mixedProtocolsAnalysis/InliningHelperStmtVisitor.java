package mixedProtocolsAnalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Trap;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AddExpr;
import soot.jimple.AndExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.ClassConstant;
import soot.jimple.CmpExpr;
import soot.jimple.CmpgExpr;
import soot.jimple.CmplExpr;
import soot.jimple.DivExpr;
import soot.jimple.DoubleConstant;
import soot.jimple.DynamicInvokeExpr;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.FloatConstant;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LengthExpr;
import soot.jimple.LongConstant;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.MulExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.OrExpr;
import soot.jimple.ParameterRef;
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
import soot.jimple.ThisRef;
import soot.jimple.ThrowStmt;
import soot.jimple.UshrExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.XorExpr;
import soot.shimple.AbstractShimpleValueSwitch;
import soot.util.Chain;

public class InliningHelperStmtVisitor extends AbstractStmtSwitch {
	/*
	 * transformed body of enclMethod containing inlined calls
	 */
	private Body newBody;

	public InliningHelperStmtVisitor(Body b) {
		this.newBody = (Body) b;
	}

	// @Override
	public void caseInvokeStmt(InvokeStmt stmt) {
		stmt.getInvokeExpr().apply(new InliningHelperValueVisitor(stmt, null));
		// need to get rid of the call statement.
		newBody.getUnits().remove(stmt);
	}

	@Override
	public void caseAssignStmt(AssignStmt stmt) {
		Value leftOp = stmt.getLeftOp();
		Value rightOp = stmt.getRightOp();
		leftOp.apply(new InliningHelperValueVisitor(stmt, stmt.getLeftOpBox()));
		rightOp.apply(new InliningHelperValueVisitor(stmt, stmt.getRightOpBox()));
	}

	@Override
	public void caseReturnStmt(ReturnStmt stmt) {

		Value returnOp = stmt.getOp();
		returnOp.apply(new InliningHelperValueVisitor(stmt, stmt.getOpBox()));
	}

	@Override
	public void caseIdentityStmt(IdentityStmt stmt) {
	}

	@Override
	public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
	}

	@Override
	public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
	}

	@Override
	public void caseThrowStmt(ThrowStmt stmt) {
	}

	@Override
	public void caseGotoStmt(GotoStmt stmt) {
	}

	@Override
	public void caseIfStmt(IfStmt stmt) {
	}

	@Override
	public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
	}

	@Override
	public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
	}

	/**
	 * looks for static invoke expressions and inlines those calls
	 * 
	 * @author ishaq
	 *
	 */
	class InliningHelperValueVisitor extends AbstractShimpleValueSwitch {
		private Stmt currentStmt;
		private ValueBox boxForReturnValue;

		/**
		 * 
		 * @param currentStmt
		 *            The statement whose values are being visited by this class
		 * @param boxForReturnValue
		 *            If this is not null and inlined function has a return
		 *            statement, that would be removed and the return value
		 *            would be put in the the passed box instead.
		 * 
		 *            If NULL is passed, return statement is not removed (if
		 *            present).
		 * 
		 *            This param has no effect if the inlined function does not
		 *            contain return statement.
		 */
		InliningHelperValueVisitor(Stmt currentStmt, ValueBox boxForReturnValue) {
			this.currentStmt = currentStmt;
			this.boxForReturnValue = boxForReturnValue;
		}

		@Override
		public void caseCastExpr(CastExpr v) {
		}

		@Override
		public void caseParameterRef(ParameterRef v) {
		}

		@Override
		public void caseThisRef(ThisRef v) {
		}

		@Override
		public void caseLocal(Local v) {
		}

		@Override
		public void caseStaticFieldRef(StaticFieldRef v) {
		}

		@Override
		public void caseInstanceFieldRef(InstanceFieldRef v) {
			// TODO: figure out some thing for error
		}

		@Override
		public void caseArrayRef(ArrayRef v) {
			// TODO: figure out some thing for error
		}

		@Override
		public void caseInterfaceInvokeExpr(InterfaceInvokeExpr v) {
			// Ignore calls on ArrayType receivers (e.g., equals(), hashCode())
			// TODO: figure out some thing for error
		}

		@Override
		public void caseSpecialInvokeExpr(SpecialInvokeExpr v) {
			// System.out.println(v);
			handleDirectCall(v);
		}

		@Override
		public void caseStaticInvokeExpr(StaticInvokeExpr v) {
			// System.out.println(v);
			handleDirectCall(v);
		}

		@Override
		public void caseVirtualInvokeExpr(VirtualInvokeExpr v) {
			// Ignore calls on ArrayType receivers (e.g., clone())
			// TODO: figure out some thing for error
		}

		// Can ignore. We consider Java bytecode.
		@Override
		public void caseDynamicInvokeExpr(DynamicInvokeExpr v) {
			handleDirectCall(v);
		}

		@Override
		public void caseNewExpr(NewExpr v) {
			// TODO: figure out some thing for error
		}

		@Override
		public void caseNewArrayExpr(NewArrayExpr v) {
			// TODO: figure out some thing for error
		}

		@Override
		public void caseNewMultiArrayExpr(NewMultiArrayExpr v) {
			// TODO: figure out some thing for error
		}

		@Override
		public void caseLengthExpr(LengthExpr v) {
		}

		@Override
		public void caseAddExpr(AddExpr v) {
		}

		@Override
		public void caseAndExpr(AndExpr v) {
		}

		@Override
		public void caseDivExpr(DivExpr v) {
		}

		@Override
		public void caseMulExpr(MulExpr v) {
		}

		@Override
		public void caseOrExpr(OrExpr v) {
		}

		@Override
		public void caseSubExpr(SubExpr v) {
		}

		@Override
		public void caseXorExpr(XorExpr v) {
		}

		@Override
		public void caseShlExpr(ShlExpr v) {
		}

		@Override
		public void caseShrExpr(ShrExpr v) {
		}

		@Override
		public void caseUshrExpr(UshrExpr v) {
		}

		@Override
		public void caseDoubleConstant(DoubleConstant v) {
		}

		@Override
		public void caseFloatConstant(FloatConstant v) {
		}

		@Override
		public void caseIntConstant(IntConstant v) {
		}

		@Override
		public void caseLongConstant(LongConstant v) {
		}

		@Override
		public void caseNullConstant(NullConstant v) {
		}

		@Override
		public void caseStringConstant(StringConstant v) {
		}

		@Override
		public void caseClassConstant(ClassConstant v) {
		}

		@Override
		public void caseCmpExpr(CmpExpr v) {
		}

		@Override
		public void caseCmpgExpr(CmpgExpr v) {
		}

		@Override
		public void caseCmplExpr(CmplExpr v) {
		}

		@Override
		public void caseCaughtExceptionRef(CaughtExceptionRef v) {
		}

		@Override
		public void caseInstanceOfExpr(InstanceOfExpr v) {
		}

		@Override
		public void defaultCase(Object v) {
			// TODO:
			System.out.println("Unhandled value: " + v + " of type " + v.getClass());
		}

		private void handleDirectCall(InvokeExpr v) {
			try {
				if (v.getMethod() == null) {
					System.out.println("WARN: Cannot find method at " + "\n\t" + v);
					return;
				}
				SootMethod sm = v.getMethod();
				if (sm.getName().equals("<init>") && sm.getDeclaringClass().getName().equals("java.lang.Object"))
					return;
				inlineMethod(v);

			} catch (RuntimeException e) {
				System.out.println("ERROR: Cannot find method in InvokeExpr " + v);
				e.printStackTrace();
			} catch(UnsupportedFeatureException e) {
				e.printStackTrace();
			}
		}

		private void inlineMethod(InvokeExpr e) throws UnsupportedFeatureException {
//			System.out.println("inlineMethod: ");
//			System.out.println("beforeTransform: " + newBody);
//			System.out.println();
			SootMethod m = e.getMethod();
			renameLocals(m);
			Body b = m.getActiveBody();
			PatchingChain<Unit> unitChain = newBody.getUnits();
			Chain<Trap> trapChain = newBody.getTraps();
			Chain<Local> localChain = newBody.getLocals();

			HashMap<Object, Object> bindings = new HashMap<Object, Object>();

			{
				// Clone units in body's statement list
				for (Unit original : b.getUnits()) {
					
					Unit copy = (Unit) original.clone();

					copy.addAllTagsOf(original);

					// Add cloned unit to our unitChain.
					unitChain.insertBefore(copy, currentStmt);

					// Build old <-> new map to be able to patch up references
					// to other units
					// within the cloned units. (these are still referring to
					// the original
					// unit objects).
					bindings.put(original, copy);
				}
			}

			{
				// NOTE: We do not need to copy traps, we'll not allow code to
				// throw exceptions
				// Clone trap units.
				for (Trap original : b.getTraps()) {
					Trap copy = (Trap) original.clone();

					// Add cloned unit to our trap list.
					trapChain.addLast(copy);

					// Store old <-> new mapping.
					bindings.put(original, copy);
				}
			}

			{
				// Clone local units.
				for (Local original : b.getLocals()) {
					Local copy = (Local) original.clone();

					// Add cloned unit to our trap list.
					localChain.addLast(copy);

					// Build old <-> new mapping.
					bindings.put(original, copy);
				}
			}

			{
				// Patch up references within units using our (old <-> new) map.
				for (UnitBox box : newBody.getAllUnitBoxes()) {
					Unit newObject, oldObject = box.getUnit();

					// if we have a reference to an old object, replace it
					// it's clone.
					if ((newObject = (Unit) bindings.get(oldObject)) != null) {
						box.setUnit(newObject);
					}

				}
			}

			{
				// backpatching all local variables.
				for (ValueBox vb : newBody.getUseBoxes()) {
					if (vb.getValue() instanceof Local) {
						Value newValue = (Value) bindings.get(vb.getValue());
						if (newValue != null) {
							vb.setValue(newValue);
						}

					}
				}
				for (ValueBox vb : newBody.getDefBoxes()) {
					if (vb.getValue() instanceof Local) {
						Value newValue = (Value) bindings.get(vb.getValue());
						if (newValue != null) {
							vb.setValue(newValue);
						}
					}
				}
			}

//			System.out.println("after copy: " + newBody);

			{
				// get rid of identity statements from callee, we change the
				// uses to use
				// the actual params instead
				for (Unit original : b.getUnits()) {
					if (!(original instanceof IdentityStmt)) {
						continue;
					}

					IdentityRef idRef = (IdentityRef) ((IdentityStmt) original).getRightOp();
					Value oldValue = ((IdentityStmt) original).getLeftOp();
					Value newValue = (Value) bindings.get(oldValue);
					if (!(idRef instanceof ParameterRef)) {
						continue;
					}
					ParameterRef paramRef = (ParameterRef) idRef;
					int n = paramRef.getIndex();

					// replace uses to point to the local variable in the parent
					for (ValueBox vb : newBody.getUseBoxes()) {
						if (vb.getValue().equals(newValue)) {
							vb.setValue(e.getArg(n));
						}
					}

					// remove copy of identity statement
					Unit copyIdentityStmt = (Unit) bindings.get(original);
					newBody.getUnits().remove(copyIdentityStmt);
					// remove copy of local
					if (oldValue instanceof Local) {
						newBody.getLocals().remove((Local) newValue);
					}
				}
			}
			
			// we disallow more than 1 return statements
			// TODO: may be handle more than 1 return statement by creating appropriate Phi Nodes?
			{	
				int returnStmtCount = 0;
				// get rid of the return statement
				for(Unit original: b.getUnits()) {
					// no need to add void return statement
					if (original instanceof ReturnVoidStmt) {
						returnStmtCount++;
						newBody.getUnits().remove(bindings.get(original));
						continue;
					}
					// if it returns something, put it in the box for return value
					if(original instanceof ReturnStmt) {
						returnStmtCount++;
						this.boxForReturnValue.setValue(((ReturnStmt)original).getOp());
						newBody.getUnits().remove(bindings.get(original));
						continue;
					}
				}
				if(returnStmtCount > 1) {
					throw new UnsupportedFeatureException("cannot handle more than 1 return statements in func: " + m.getName());
				}
			}

			System.out.println("after eliminating idenity statements: " + newBody);
			System.out.println();
		}

		private void renameLocals(SootMethod m) {
			// collect local names that can't be used
			Set<String> usedNames = new HashSet<String>();
			for (Local l : newBody.getLocals()) {
				usedNames.add(l.getName());
			}

			Body b = m.getActiveBody();
			String prefix = "__" + m.getName() + "_";
			// System.out.println("Function body before renaming: \n " + b);
			for (Local l : b.getLocals()) {
				String newName = prefix + l.getName();
				if (usedNames.contains(newName)) {
					int postFix = 1;
					do {
						newName = prefix + l.getName() + postFix;
						postFix++;
					} while (usedNames.contains(newName));
				}
				usedNames.add(newName);
				l.setName(newName);
			}

			String ret = prefix + "ret";
			if (usedNames.contains(ret)) {
				int postFix = 1;
				do {
					ret = prefix + "ret" + postFix;
					postFix++;
				} while (usedNames.contains(ret));
			}

			// System.out.println("Function body after renaming: \n " + b);
		}
	}

	public void defaultCase(Object obj) {
		System.out.println("Default case (" + obj.getClass() + "): " + obj);
	}

}
