package parse;

import static parse.HtmlLexer.TokenType.NAME;
import static parse.HtmlLexer.TokenType.OPEN;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import parse.HtmlLexer.Token;
import parse.HtmlLexer.TokenType;

// 11:25-15:06/19:28-21:45
public class HtmlParser {
	
	private final HtmlLexer lexer;
	
	public HtmlParser(String s) {
		lexer = new HtmlLexer(s);
	}
	
	public DocNode parseDoc() {
		DocNode docNode = new DocNode();
		List<ASTNode> children = parseNodeList(null);

		docNode.setChildren(children);
		return docNode;
	}

	private List<ASTNode> parseNodeList(ElementNode parent) {
		List<ASTNode> children = new LinkedList<>();
		outer:
		while (true) {
			ASTNode cur;
			switch (lexer.peek().type) {
				case OPEN:
					cur = parseOpenNode(parent);
					break;
				case TEXT:
					cur = parseTextNode(parent);
					break;

				case COMMENT:
				case PREAMBLE:
				case DOCTYPE:
					// ignore for now
					lexer.next();
					continue outer;

				case OPEN_END:
				case EOF:
					break outer;

				default:
					throw new AssertionError("unexpected type: " + lexer.peek());
			}
			children.add(cur);
		}

		return children;
	}

	private ASTNode parseTextNode(ElementNode parent) {
		Token text = expect(lexer.next(), TokenType.TEXT);
		return new TextNode(parent, text);
	}
	
	private ASTNode parseOpenNode(ElementNode parent) {
		Token open = expect(lexer.next(), OPEN);
		Token name = expect(lexer.next(), NAME);

		List<AttributeNode> attribs = new LinkedList<>();
		while (lexer.peek().type == NAME) {
			attribs.add(parseAttribute());
		}

		Token close = lexer.next();
		if (close.type == TokenType.CLOSE_END || isSingular(name.val)) {
			return new ElementNode(parent, open, name, attribs, close);

		} else if (close.type == TokenType.CLOSE) {
			ElementNode elementNode = new ElementNode(parent, open, name, attribs, close);

			List<ASTNode> children = parseNodeList(elementNode);
			CloseTag closeTag = parseCloseNode(elementNode);
			if (!closeTag.getName().equals(name.val)) {
				throw new AssertionError("wrong close tag, expected '" + name.val +
						"', but was '" + closeTag.getName() + "' at offset: " + closeTag.getStart());
			}
			elementNode.setChildren(children);
			elementNode.setCloseTag(closeTag);

			return elementNode;
		} else {
			throw new AssertionError("tag close expected: " + close);
		}
	}

	private boolean isSingular(String val) {
		return Arrays.asList("br", "img").contains(val);
	}
	
	private CloseTag parseCloseNode(ElementNode parent) {
		Token open = expect(lexer.next(), TokenType.OPEN_END);
		Token name = expect(lexer.next(), TokenType.NAME);
		Token close = expect(lexer.next(), TokenType.CLOSE);
		
		return new CloseTag(parent, open, name, close);
	}
	
	private AttributeNode parseAttribute() {
		Token name = expect(lexer.next(), NAME);

		Token value = null;
		if (lexer.peek().type == TokenType.EQ) {
			lexer.next();
			value = expect(lexer.next(), TokenType.STRING);
		}

		return new AttributeNode(name, value);
	}

	private Token expect(Token token, TokenType type) {
		if (token.type != type) {
			throw new AssertionError("expected type: " + type + ", was: " + token);
		}
		return token;
	}
}
