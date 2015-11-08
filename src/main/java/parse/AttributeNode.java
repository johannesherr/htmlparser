package parse;

public class AttributeNode {

	private final HtmlLexer.Token name;
	private final HtmlLexer.Token value;

	public AttributeNode(HtmlLexer.Token name, HtmlLexer.Token value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name.val;
	}

	public String getValue() {
		return value == null ? null : value.val.substring(1, value.val.length() - 1);
	}
}
