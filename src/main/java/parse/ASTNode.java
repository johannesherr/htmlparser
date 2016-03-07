package parse;

public abstract class ASTNode {

	private int start;
	private int end;
	private ElementNode parent;
	
	public ASTNode(ElementNode parent) {
		this.parent = parent;
	}
	
	public abstract void accept(HtmlVisitor visitor);

	public ElementNode getParent() {
		return parent;
	}

	protected void setParent(ElementNode parent) {
		this.parent = parent;
	}

	public abstract int start();
	public abstract int end();
}
