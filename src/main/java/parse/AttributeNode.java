package parse;

public class AttributeNode extends ASTNode {

	private final HtmlLexer.Token name;
	private final HtmlLexer.Token value;

	public AttributeNode(HtmlLexer.Token name, HtmlLexer.Token value) {
		super(null);
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name.val;
	}

	public String getValue() {
		return value == null ? null : value.val.substring(1, value.val.length() - 1);
	}

	@Override
	public void accept(HtmlVisitor visitor) {
		visitor.visitAttribute(this);
	}

	@Override
	public int start() {
		return name.start;
	}

	@Override
	public int end() {
		return value.end;
	}

	@Override
	public String toString() {
		return "AttributeNode{" +
				"name=" + getName() +
				", value=" + getValue() +
				'}';
	}
}
