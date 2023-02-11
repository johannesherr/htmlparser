package parse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class HtmlLexer {
	
	private final String s;
	private int i = 0;
	private final List<Token> tks = new ArrayList<>();
	private int state = States.INITIAL;
	private int start = 0;
	private int tok;

	public HtmlLexer(String s) {
		this.s = s;
		lex();
	}

	public List<Token> getTokens() {
		return tks;
	}

	private void lex() {
		while (i < s.length()) {
			char c = s.charAt(i);

			switch (state) {
				case States.IN_ELEMENT:
				case States.OPEN_SEEN:
				case States.CLOSER_SEEN:
				case States.COMMENT_OPEN_2:
				case States.COMMENT_OPEN_3:

					switch (c) {

						case '/':
							if (state == States.OPEN_SEEN) {
								createToken(TokenType.OPEN_END);
								state = States.IN_ELEMENT;
							} else if (state == States.IN_ELEMENT) {
								state = States.CLOSER_SEEN;
								start = i;
							} else {
								lexerError();
							}
							break;
						
						case '>':
							if (state == States.CLOSER_SEEN) {
								createToken(TokenType.CLOSE_END);
								
							} else if (state == States.IN_ELEMENT) {
								start = i;
								createToken(TokenType.CLOSE);
							} else {
								lexerError();
							}
							state = States.INITIAL;
							break;
						
						case '=':
							if (state == States.IN_ELEMENT) {
								start = i;
								createToken(TokenType.EQ);
								
							} else {
								lexerError();
							}
							break;
						
						case '"':
							readString();
							break;
						
						case '!':
							if (state == States.OPEN_SEEN) {
								state = States.COMMENT_OPEN_2;
							} else {
								lexerError();
							}
							break;
						
						case '-':
							if (state == States.COMMENT_OPEN_2) {
								state = States.COMMENT_OPEN_3;
							} else if (state == States.COMMENT_OPEN_3) {
								readComment();
								state = States.INITIAL;
								
							} else {
								lexerError();
							}
							break;
						
						default:
							if (state == States.OPEN_SEEN) {
								createTokenEx(TokenType.OPEN);
							}
							if (c != ' ') {
								readName();
							}
							state = States.IN_ELEMENT;
					}
					break;

				case States.INITIAL:
					switch (c) {
						case '<':
							if (lookingAt("<?")) {
								readPreamble();
							} else if (lookingAt("<!") && !lookingAt("<!--")) {
								readDocType();
							} else if (state == States.INITIAL) {
								state = States.OPEN_SEEN;
								start = i;
							} else {
								lexerError();
							}
							break;

						default:
							readText();
							state = States.INITIAL;
					}
					break;

				default:
					lexerError();
				
					
			}
			i++;
		}

		if (state != States.INITIAL) lexerError();
	}

	private void readDocType() {
		readChars("<!");

		if (skipAfter(">") == -1) {
			throw new AssertionError("unclosed doctype");
		}

		i--;
		createToken(TokenType.DOCTYPE);
	}

	private void readPreamble() {
		start = i;
		readChars("<?");

		if (skipAfter("?>") == -1) {
			throw new AssertionError("unclosed preamble");
		}

		i--;
		createToken(TokenType.PREAMBLE);
	}
	
	private void readChars(String cs) {
		for (char c : cs.toCharArray()) {
			readChar(c);
		}
	}

	private void readChar(char c) {
		if (i >= s.length()) {
			throw new AssertionError("expected to see character '" + c + "', but was EOF");
		}
		char r = s.charAt(i++);
		if (r != c) {
			throw new AssertionError("expected to see character '" + c + "', but was '" + r + "'");
		}
		// else success
	}
	
	private void readName() {
		start = i;
		outer:
		while (i < s.length()) {
			char c = s.charAt(i);
			switch (c) {
				case ' ':
				case '\t':
				case '\r':
				case '\n':
				case '=':
				case '/':
				case '>':
					break outer;
			}
 			i++;
		}
		createTokenEx(TokenType.NAME);
		skipWS();
		i--;
	}

	private void skipWS() {
		while (i < s.length()) {
			char c = s.charAt(i);
			switch (c) {
				case ' ':
				case '\t':
				case '\r':
				case '\n':
					i++;
					continue;
			}

			break;
		}
	}

	private void readText() {
		start = i;
		while (i < s.length() && !lookingAt("<")) {
			i++;
		}
		i--;
		createToken(TokenType.TEXT);
	}

	private void readString() {
		start = i;
		while (++i < s.length()) {
			if (lookingAt("\"")) {
				createToken(TokenType.STRING);
				return;
			}
		}
		lexerError();
	}

	private void readComment() {
		i = skipAfter("-->");
		if (i == -1) {
			lexerError();
		}
		i--;
		createToken(TokenType.COMMENT);
	}

	private int skipAfter(String target) {
		for (; i < s.length(); i++) {
			if (lookingAt(target)) {
				i = i + target.length();
				return i;
			}
		}
		return -1;
	}

	private boolean lookingAt(String target) {
		for (int j = 0; j < target.length(); j++) {
			if (i + j >= s.length() || s.charAt(i + j) != target.charAt(j)) {
				return false;
			}
		}
		return true;
	}

	private void lexerError() {
		throw new AssertionError(String.format("unexpected lexer state: pos %s, state: %s, at char: '%s', current tokens: %s", i, States.print(state), s.charAt(i), tks.subList(Math.max(0, tks.size() - 10), tks.size())));
	}

	private void createToken(TokenType type) {
		tks.add(new Token(start, i + 1, s.substring(start, i + 1), type));
	}

	private void createTokenEx(TokenType type) {
		tks.add(new Token(start, i, s.substring(start, i), type));
	}

	public Token next() {
		return getTokens().get(tok++);
	}

	public Token peek() {
		if (tok == getTokens().size()) {
			return new Token(i, i, "", TokenType.EOF);
		} else {
			return getTokens().get(tok);
		}
	}

	private static class States {
		public static final int INITIAL = 0;
		public static final int OPEN_SEEN = 1;
		public static final int CLOSER_SEEN = 2;
		public static final int COMMENT_OPEN_2 = 3;
		public static final int COMMENT_OPEN_3 = 4;
		public static final int OPEN_END_SEEN = 7;
		public static final int IN_ELEMENT = 9;

		public static String print(int state) {
			try {
				for (Field field : States.class.getDeclaredFields()) {
					if (field.getInt(null) == state) {
						return field.getName();
						
					}
				}
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			return String.format("unkown state (%s)", state);
		}
	}

	public static class Token {
		int start, end;
		String val;
		TokenType type;

		public Token(int start, int end, String val, TokenType type) {
			this.start = start;
			this.end = end;
			this.val = val;
			this.type = type;
		}

		@Override
		public String toString() {
			return "Token{" +
					"start=" + start +
					", end=" + end +
					", val='" + val.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + '\'' +
					", type=" + type +
					'}';
		}
	}

	public enum TokenType {
		OPEN, CLOSE, OPEN_END, CLOSE_END,
		NAME, TEXT, EQ, STRING, COMMENT,
		DOCTYPE, EOF, PREAMBLE;
	}
}
