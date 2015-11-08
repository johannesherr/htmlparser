package parse;

import java.util.List;

public class DocNode extends ASTNode {
	private List<ASTNode> children;
	
	public DocNode() {
		super(null);
	}
	
	public List<ASTNode> getChildren() {
		return children;
	}

	@Override
	public void accept(HtmlVisitor visitor) {
		for (ASTNode child : children) {
			child.accept(visitor);
		}
	}

	@Override
	public int start() {
		return children.get(0).start();
	}

	@Override
	public int end() {
		return children.get(children.size() - 1).end();
	}

	public void setChildren(List<ASTNode> children) {
		this.children = children;
	}
}
