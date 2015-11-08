package parse;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import parse.HtmlLexer.Token;

public class ElementNode extends ASTNode {
	private final Token open;
	private final Token name;
	private final List<AttributeNode> attribs;
	private List<ASTNode> children = Collections.emptyList();
	private Token close;
	private CloseTag closeTag;

	public ElementNode(ElementNode parent,
					   Token open,
					   Token name,
					   List<AttributeNode> attribs,
					   Token close) {
		super(parent);

		this.open = open;
		this.name = name;
		this.attribs = attribs;
		this.close = close;
	}

	public List<ASTNode> getChildren() {
		return children;
	}

	public String getTagName() {
		return name.val;
	}

	@Override
	public String toString() {
		return "ElementNode{" +
				"open=" + open +
				", name=" + name +
				", attribs=" + attribs +
				", children=" + children +
				", close=" + close +
				", closeTag=" + closeTag +
				'}';
	}

	@Override
	public void accept(HtmlVisitor visitor) {
		visitor.visitElement(this);
		for (ASTNode child : children) {
			child.accept(visitor);
		}
	}

	@Override
	public int start() {
		return open.start;
	}

	@Override
	public int end() {
		return closeTag != null ? closeTag.end() : close.end;
	}

	public List<AttributeNode> getAttributes() {
		return attribs;
	}

	public void setChildren(List<ASTNode> children) {
		this.children = children;
	}

	public void setCloseTag(CloseTag closeTag) {
		this.closeTag = closeTag;
	}

	public List<ElementNode> getChildNodes() {
		List<ElementNode> ret = new LinkedList<>();
		for (ASTNode astNode : getChildren()) {
			if (astNode instanceof ElementNode) {
				ret.add((ElementNode) astNode);
			}
		}
		return ret;
	}
}
