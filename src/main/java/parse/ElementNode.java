package parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import parse.HtmlLexer.Token;

public class ElementNode extends ASTNode {
	private final Token open;
	private final Token name;
	private final List<AttributeNode> attribs;
	private List<ASTNode> children = Collections.emptyList();
	private final Token close;
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

		for (AttributeNode attrib : this.attribs) {
			attrib.setParent(this);
		}
	}

	public List<ASTNode> getChildren() {
		return children;
	}

	public String getTagName() {
		return name.val;
	}

	@Override
	public void accept(HtmlVisitor visitor) {
		visitor.visitElement(this);
		getAttributes().forEach(visitor::visitAttribute);
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
		List<ElementNode> ret = new ArrayList<>();
		for (ASTNode astNode : getChildren()) {
			if (astNode instanceof ElementNode) {
				ret.add((ElementNode) astNode);
			}
		}
		return ret;
	}

	public String getTrimmedStringContent() {
		List<ASTNode> children = getChildren();
		if (children.size() != 1) throw new IllegalStateException("more than one child");

		ASTNode child = children.get(0);
		if (child instanceof TextNode) {
			TextNode textNode = (TextNode) child;
			return textNode.trimmedString();
		} else {
			throw new IllegalStateException("only child is not a text node");
		}
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

	public Optional<String> getAttribute(String name) {
		return getAttributes().stream()
				.filter(a -> a.getName().equals(name))
				.map(AttributeNode::getValue)
				.findAny();
	}
}
