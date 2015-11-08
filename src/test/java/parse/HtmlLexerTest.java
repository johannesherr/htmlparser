package parse;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

public class HtmlLexerTest {

	@Test
	public void standalone() {
		List<HtmlLexer.Token> tokens = lex("<foo/>");

		assertThat(tokens.get(0).toString(), is("Token{start=0, end=1, val='<', type=OPEN}"));
		assertThat(tokens.get(1).toString(), is("Token{start=1, end=4, val='foo', type=NAME}"));
		assertThat(tokens.get(2).toString(), is("Token{start=4, end=6, val='/>', type=CLOSE_END}"));
	}

	@Test
	public void open_close() throws Exception {
		List<HtmlLexer.Token> tokens = lex("<foo></foo>");

		assertThat(tokens.get(0).toString(), is("Token{start=0, end=1, val='<', type=OPEN}"));
		assertThat(tokens.get(1).toString(), is("Token{start=1, end=4, val='foo', type=NAME}"));
		assertThat(tokens.get(2).toString(), is("Token{start=4, end=5, val='>', type=CLOSE}"));
		assertThat(tokens.get(3).toString(), is("Token{start=5, end=7, val='</', type=OPEN_END}"));
		assertThat(tokens.get(4).toString(), is("Token{start=7, end=10, val='foo', type=NAME}"));
		assertThat(tokens.get(5).toString(), is("Token{start=10, end=11, val='>', type=CLOSE}"));
	}

	@Test
	public void text_content() throws Exception {
		List<HtmlLexer.Token> tokens = lex("<foo>asdf bar bazz</foo>");

		assertThat(tokens.get(0).toString(), is("Token{start=0, end=1, val='<', type=OPEN}"));
		assertThat(tokens.get(1).toString(), is("Token{start=1, end=4, val='foo', type=NAME}"));
		assertThat(tokens.get(2).toString(), is("Token{start=4, end=5, val='>', type=CLOSE}"));
		assertThat(tokens.get(3).toString(), is("Token{start=5, end=18, val='asdf bar bazz', type=TEXT}"));
		assertThat(tokens.get(4).toString(), is("Token{start=18, end=20, val='</', type=OPEN_END}"));
		assertThat(tokens.get(5).toString(), is("Token{start=20, end=23, val='foo', type=NAME}"));
		assertThat(tokens.get(6).toString(), is("Token{start=23, end=24, val='>', type=CLOSE}"));
	}

	@Test
	public void attribute() throws Exception {
		List<HtmlLexer.Token> tokens = lex("<foo bar=\"1 + 2 = asdf\"></foo>");

		assertThat(tokens.get(0).toString(), is("Token{start=0, end=1, val='<', type=OPEN}"));
		assertThat(tokens.get(1).toString(), is("Token{start=1, end=4, val='foo', type=NAME}"));
		assertThat(tokens.get(2).toString(), is("Token{start=5, end=8, val='bar', type=NAME}"));
		assertThat(tokens.get(3).toString(), is("Token{start=8, end=9, val='=', type=EQ}"));
		assertThat(tokens.get(4).toString(), is("Token{start=9, end=23, val='\"1 + 2 = asdf\"', type=STRING}"));
		assertThat(tokens.get(5).toString(), is("Token{start=23, end=24, val='>', type=CLOSE}"));
		assertThat(tokens.get(6).toString(), is("Token{start=24, end=26, val='</', type=OPEN_END}"));
		assertThat(tokens.get(7).toString(), is("Token{start=26, end=29, val='foo', type=NAME}"));
		assertThat(tokens.get(8).toString(), is("Token{start=29, end=30, val='>', type=CLOSE}"));
	}

	@Test
	public void standalone_attributes() throws Exception {
		List<HtmlLexer.Token> tokens = lex("<foo asdf bar bazz/>");

		assertThat(tokens.get(0).toString(), is("Token{start=0, end=1, val='<', type=OPEN}"));
		assertThat(tokens.get(1).toString(), is("Token{start=1, end=4, val='foo', type=NAME}"));
		assertThat(tokens.get(2).toString(), is("Token{start=5, end=9, val='asdf', type=NAME}"));
		assertThat(tokens.get(3).toString(), is("Token{start=10, end=13, val='bar', type=NAME}"));
		assertThat(tokens.get(4).toString(), is("Token{start=14, end=18, val='bazz', type=NAME}"));
		assertThat(tokens.get(5).toString(), is("Token{start=18, end=20, val='/>', type=CLOSE_END}"));
	}

	@Test
	public void mult_elements() throws Exception {
		List<HtmlLexer.Token> tokens = lex("<foo><bar/></foo>");

		assertThat(tokens.get(0).toString(), is("Token{start=0, end=1, val='<', type=OPEN}"));
		assertThat(tokens.get(1).toString(), is("Token{start=1, end=4, val='foo', type=NAME}"));
		assertThat(tokens.get(2).toString(), is("Token{start=4, end=5, val='>', type=CLOSE}"));
		assertThat(tokens.get(3).toString(), is("Token{start=5, end=6, val='<', type=OPEN}"));
		assertThat(tokens.get(4).toString(), is("Token{start=6, end=9, val='bar', type=NAME}"));
		assertThat(tokens.get(5).toString(), is("Token{start=9, end=11, val='/>', type=CLOSE_END}"));
		assertThat(tokens.get(6).toString(), is("Token{start=11, end=13, val='</', type=OPEN_END}"));
		assertThat(tokens.get(7).toString(), is("Token{start=13, end=16, val='foo', type=NAME}"));
		assertThat(tokens.get(8).toString(), is("Token{start=16, end=17, val='>', type=CLOSE}"));
	}

	@Test
	public void comment() throws Exception {
		List<HtmlLexer.Token> tokens = lex("<foo><!--<bar/> --></foo>");

		assertThat(tokens.get(0).toString(), is("Token{start=0, end=1, val='<', type=OPEN}"));
		assertThat(tokens.get(1).toString(), is("Token{start=1, end=4, val='foo', type=NAME}"));
		assertThat(tokens.get(2).toString(), is("Token{start=4, end=5, val='>', type=CLOSE}"));
		assertThat(tokens.get(3).toString(), is("Token{start=5, end=19, val='<!--<bar/> -->', type=COMMENT}"));
		assertThat(tokens.get(4).toString(), is("Token{start=19, end=21, val='</', type=OPEN_END}"));
		assertThat(tokens.get(5).toString(), is("Token{start=21, end=24, val='foo', type=NAME}"));
		assertThat(tokens.get(6).toString(), is("Token{start=24, end=25, val='>', type=CLOSE}"));
	}

	@Test
	public void line_break() throws Exception {
		List<HtmlLexer.Token> tokens = lex("<foo\nbar>\nbazz\r\n</foo>");

		assertThat(tokens.get(0).toString(), is("Token{start=0, end=1, val='<', type=OPEN}"));
		assertThat(tokens.get(1).toString(), is("Token{start=1, end=4, val='foo', type=NAME}"));
		assertThat(tokens.get(2).toString(), is("Token{start=5, end=8, val='bar', type=NAME}"));
		assertThat(tokens.get(3).toString(), is("Token{start=8, end=9, val='>', type=CLOSE}"));
		assertThat(tokens.get(4).toString(), is("Token{start=9, end=16, val='\\nbazz\\r\\n', type=TEXT}"));
		assertThat(tokens.get(5).toString(), is("Token{start=16, end=18, val='</', type=OPEN_END}"));
		assertThat(tokens.get(6).toString(), is("Token{start=18, end=21, val='foo', type=NAME}"));
		assertThat(tokens.get(7).toString(), is("Token{start=21, end=22, val='>', type=CLOSE}"));
	}

	@Test
	public void specials_in_text() throws Exception {
		List<HtmlLexer.Token> tokens = lex("<foo>asdf - bar ! </foo>");

		print(tokens);
	}

	@Test
	public void quoted_text_is_still_text() throws Exception {
		List<HtmlLexer.Token> tokens = lex("<foo>\"bar\"</foo>");

		assertThat(tokens.get(0).toString(), is("Token{start=0, end=1, val='<', type=OPEN}"));
		assertThat(tokens.get(1).toString(), is("Token{start=1, end=4, val='foo', type=NAME}"));
		assertThat(tokens.get(2).toString(), is("Token{start=4, end=5, val='>', type=CLOSE}"));
		assertThat(tokens.get(3).toString(), is("Token{start=5, end=10, val='\"bar\"', type=TEXT}"));
		assertThat(tokens.get(4).toString(), is("Token{start=10, end=12, val='</', type=OPEN_END}"));
		assertThat(tokens.get(5).toString(), is("Token{start=12, end=15, val='foo', type=NAME}"));
		assertThat(tokens.get(6).toString(), is("Token{start=15, end=16, val='>', type=CLOSE}"));
	}

	@Test
	public void spiegel() throws Exception {
		byte[] bytes = Files.readAllBytes(Paths.get("src", this.getClass().getPackage().getName(), "spiegel.txt"));
		String text = new String(bytes, StandardCharsets.ISO_8859_1);
		
		List<HtmlLexer.Token> tokens = lex(text);

		assertThat(tokens.size(), is(12_354));
	}

	private void print(List<HtmlLexer.Token> tokens) {
		for (HtmlLexer.Token token : tokens) {
			System.out.println(token);
		}
	}

	private List<HtmlLexer.Token> lex(String s) {
		return new HtmlLexer(s).getTokens();
	}

	private void genAsserts(List<HtmlLexer.Token> tokens) {
		int i = 0;
		for (HtmlLexer.Token token : tokens) {
			System.out.println(String.format("assertThat(tokens.get(%2$d).toString(), is(\"%1$s\"));", token.toString().replace("\\", "\\\\"), i++));
		}
	}
	
}