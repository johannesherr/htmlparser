package parse;

import parse.HtmlLexer.Token;

public class TextNode extends ASTNode {
	private Token text;

	public TextNode(ElementNode parent, Token text) {
		super(parent);
		this.text = text;
	}

	@Override
	public void accept(HtmlVisitor visitor) {
		visitor.visitText(this);
	}

	@Override
	public int start() {
		return text.start;
	}

	@Override
	public int end() {
		return text.end;
	}

	public String trimmedString() {
		return text.val.trim();
	}

	@Override
	public String toString() {
		return "TextNode{" +
				"text=" + text +
				'}';
	}
}
