package parse;

import parse.HtmlLexer.Token;

public class CloseTag extends ASTNode {
	private final Token open;
	private final Token name;
	private final Token close;

	public CloseTag(ElementNode parent, Token open, Token name, Token close) {
		super(parent);
		this.open = open;
		this.name = name;
		this.close = close;
	}

	public String getName() {
		return name.val;
	}

	public int getStart() {
		return open.start;
	}

	@Override
	public void accept(HtmlVisitor visitor) {

	}

	@Override
	public int start() {
		return open.start;
	}

	@Override
	public int end() {
		return close.end;
	}
}
