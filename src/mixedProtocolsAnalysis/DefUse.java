package mixedProtocolsAnalysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import mixedProtocolsAnalysis.Node.NodeType;
import soot.Local;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;

/**
 * definition and uses of a Local
 * @author ishaq
 *
 */
public class DefUse {
	protected final Local var;
	protected Node def;
	protected Set<Stmt> copies = new HashSet<Stmt>();
	
	protected final Set<Node> uses = new HashSet<Node>();
	
	public DefUse(Local var, Stmt defn, BriefUnitGraph cfg) {
		this.var = var;
		this.def = new Node(defn, cfg);
	}
	
	public void addUse(Node use) {
		this.uses.add(use);
	}
	
	public Local getVar() {
		return var;
	}
	
	public Node getDef() {
		return def;
	}
	
	public void setDef(Node defn) {
		this.def = defn;
	}
	
	public Set<Node> getUses() {
		return uses;
	}
	
	public Set<Stmt> getCopies() {
		return copies;
	}
	
	public void markCopy(DefUse other) {
		this.copies.add(other.def.id);
		this.copies.addAll(other.copies);
		removeUse(other.def);
		this.uses.addAll(other.uses);
	}
	
	@Override
	public String toString() {
		if(copies.size() > 0) {
			return "DefUse<var = " + var + " copies: " + copies + ", def = " + def + ", uses = " + uses + ">";
		}
		return "DefUse<var = " + var + ", def = " + def + ", uses = " + uses + ">";
		
	}
	
	private void removeUse(Node key) {
		Iterator<Node> iter = uses.iterator();
		while(iter.hasNext()) {
			Node dui = iter.next();
			if(dui.id.equals(key.id)) {
				iter.remove();
			}
		}
	}
	
}
