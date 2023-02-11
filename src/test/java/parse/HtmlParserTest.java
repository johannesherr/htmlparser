package parse;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.base.Splitter;

public class HtmlParserTest {

	public static final String HTML1 = "<foo><bar>asdf</bar><bazz /></foo>";

	@Test
	public void tag_names() {
		DocNode root = parse(HTML1);

		List<String> tags = new LinkedList<>();
		root.accept(new HtmlVisitor() {
			@Override
			public void visitElement(ElementNode elementNode) {
				tags.add(elementNode.getTagName());
			}
		});

		assertThat(tags, is(asList("foo", "bar", "bazz")));
	}

	@Test
	public void rewrite_pom() throws Exception {
		String xml = "<dependencies>\n" +
				"<dependency>\n" +
				"<name>baristad</name>\n" +
				"<version>5</version>\n" +
				"</dependency>\n" +
				"<dependency>\n" +
				"<name>foo</name>\n" +
				"<version>1</version>\n" +
				"</dependency>\n" +
				"<dependency>\n" +
				"<name>bar</name>\n" +
				"<version>2</version>\n" +
				"</dependency>\n" +
				"</dependencies>";

		class Edit {
			int s, e;
			String n;

			public Edit(int s, int e, String n) {
				this.s = s;
				this.e = e;
				this.n = n;
			}
		}

		List<Edit> edits = new LinkedList<>();

		parse(xml).accept(new HtmlVisitor() {
			@Override
			public void visitElement(ElementNode elementNode) {
				if (elementNode.getTagName().equals("version")) {
					ElementNode parent = elementNode.getParent();
					ElementNode nameNode = parent.getChildNodes().get(0);
					String name = ((TextNode) nameNode.getChildren().get(0)).trimmedString();
					if (name.startsWith("bar")) {
						TextNode textNode = (TextNode) elementNode.getChildren().get(0);
						String version = textNode.trimmedString();
						System.out.println("version = " + version);

						String newVersion = String.valueOf(Integer.parseInt(version) + 1);

						edits.add(new Edit(textNode.start(), textNode.end(), newVersion));
					}
				}
			}
		});

		Collections.reverse(edits);

		String edited = xml;
		for (Edit edit : edits) {
			edited = edited.substring(0, edit.s) + edit.n + edited.substring(edit.e);
		}

		assertThat(edited, is("<dependencies>\n" +
				"<dependency>\n" +
				"<name>baristad</name>\n" +
				"<version>6</version>\n" +
				"</dependency>\n" +
				"<dependency>\n" +
				"<name>foo</name>\n" +
				"<version>1</version>\n" +
				"</dependency>\n" +
				"<dependency>\n" +
				"<name>bar</name>\n" +
				"<version>3</version>\n" +
				"</dependency>\n" +
				"</dependencies>"));
	}

	@Test
	public void list_dependencies() throws Exception {
		String content = new String(Files.readAllBytes(Paths.get("pom.xml")), StandardCharsets.UTF_8);

		BiFunction<ElementNode, String, String> getTagValue = (node, tag) -> node.getChildNodes().stream()
				.filter(c -> c.getTagName().equals(tag))
				.findAny()
				.get()
				.getTrimmedStringContent();

		List<String> deps = new LinkedList<>();
		new HtmlParser(content).parseDoc().accept(new HtmlVisitor() {
			@Override
			public void visitElement(ElementNode elementNode) {
				if (elementNode.getTagName().equals("dependency")) {
					String line = asList("groupId", "artifactId", "version").stream()
							.map(tag -> getTagValue.apply(elementNode, tag))
							.collect(Collectors.joining(":"));
					deps.add(line);
				}
			}
		});

		deps.forEach(System.out::println);

		assertThat(deps, is(asList(
				"junit:junit:4.13.2",
				"com.google.guava:guava:31.1-jre"
		)));
	}

	/**
	 * Parse Attributes, with Offset-Data.
	 */
	@Test
	public void attribOffsets() throws Exception {
		String text = "<f abc=\"123\" de=\"foo\"></f>";
		DocNode node = parse(text);
		ElementNode elem = (ElementNode) node.getChildren().get(0);

		List<AttributeNode> attributes = elem.getAttributes();
		System.out.println("attributes = " + attributes);
		assertThat(attributes.size(), is(2));

		assertThat(attributes.get(0).start(), is(3));
		assertThat(attributes.get(0).end(), is(12));

		assertThat(
				text.substring(
						attributes.get(1).start(),
						attributes.get(1).end()), is("de=\"foo\""));
	}

	/**
	 * AST-Visitor visits attributes.
	 */
	@Test
	public void visit_attributes() throws Exception {
		String text = "<f abc=\"123\" de=\"foo\"></f>";
		DocNode node = parse(text);

		List<List<String>> attribs = new LinkedList<>();
		node.accept(new HtmlVisitor() {
			@Override
			public void visitAttribute(AttributeNode attributeNode) {
				attribs.add(asList(attributeNode.getName(), attributeNode.getValue()));
				super.visitAttribute(attributeNode);
			}
		});

		assertThat(attribs, is(asList(
				asList("abc", "123"),
				asList("de", "foo"))));
	}
	
	/**
	 * Convenience for attribute access.
	 */
	@Test
	public void getAttributShortcut() throws Exception {
		ElementNode elem = (ElementNode) parse("<foo name=\"doe\" foo=\"123\"/>").getChildren().get(0);

		assertThat(elem.getAttribute("name").get(), is("doe"));
		assertThat(elem.getAttribute("foo").get(), is("123"));
		assertThat(elem.getAttribute("not-there").isPresent(), is(false));
	}

	@Test
	public void spiegel() throws Exception {
		byte[] bytes = Files.readAllBytes(Paths.get("src/test/java", this.getClass().getPackage().getName(), "spiegel.txt"));
		String content = new String(bytes, StandardCharsets.ISO_8859_1);

		DocNode docNode = parse(content);

		Set<String> tags = new HashSet<>();
		Set<String> classes = new HashSet<>();
		docNode.accept(new HtmlVisitor() {
			@Override
			public void visitElement(ElementNode elementNode) {
				tags.add(elementNode.getTagName());

				for (AttributeNode attributeNode : elementNode.getAttributes()) {
					if (attributeNode.getName().equals("class")) {
						String value = attributeNode.getValue();
						if (value != null) {
							classes.addAll(Splitter.onPattern(" +").splitToList(value));
						}
					}
				}
			}
		});

		assertThat(tags, is(Set.of("a", "b", "body", "br", "div", "form", "h1", "h2", "h4", "h5",
		                           "head", "hr", "html", "i", "iframe", "img", "input", "label",
		                           "li", "link", "meta", "noscript", "p", "script", "span",
		                           "strong", "style", "textarea", "time", "title", "ul")));
		assertThat(classes, is(Set.of("", "a1", "active", "adition", "article-comment", "article" +
				"-comment-title", "article-comment-user", "article-comments-box", "article" +
				"-copyright", "article-feedback", "article-function-box", "article-function-box" +
				"-wide", "article-function-date", "article-function-forum", "article-function" +
				"-social-media", "article-functions", "article-functions-bottom", "article-image" +
				"-description", "article-intro", "article-section", "article-social-bookmark",
		                              "article-title", "article-topic-box", "article-topics-all",
		                              "asset", "asset-align-center", "asset-align-left", "asset" +
				                              "-box", "asset-credit", "asset-headline", "asset" +
				                              "-headline-intro", "asset-link-box", "asset-list-box"
				, "asset-title", "aussen", "author", "box-position", "breadcrumb-history",
				                      "breitwandaufmacher", "channel-auto-border-bottom", "channel" +
				                              "-einestages-border-bottom", "channel-gesundheit" +
				                              "-border-bottom", "channel-karriere-border-bottom",
				                      "channel-kultur-border-bottom", "channel-name", "channel" +
				                              "-netzwelt-border-bottom", "channel-panorama-border" +
				                              "-bottom", "channel-politik-border-bottom", "channel" +
				                              "-reise-border-bottom", "channel-sport-border-bottom"
				, "channel-stil-border-bottom", "channel-unispiegel-border-bottom", "channel" +
				                              "-wirtschaft-border-bottom", "channel-wissenschaft" +
				                              "-border-bottom", "clearfix", "column-both", "column" +
				                              "-box-pic", "column-small", "column-wide",
				                      "delicious", "digg", "display-block", "dt-www", "einestages" +
				                              "-forum-info", "einestages-module-title", "facebook" +
				                              "-info-button", "fb-twitter-bar-bottom", "feedback" +
				                              "-item", "footer-main-nav", "footer-partner-bar",
				                      "footer-sub-nav", "form-button", "function-box", "grid" +
				                              "-article", "header-logo", "header-main", "header" +
				                              "-search", "header-top", "headline", "headline-intro"
				, "home-link", "home-link-box", "html_116000", "image-buttons", "image-buttons" +
				                              "-panel", "innen", "is-first", "js-article-comments" +
				                              "-box-content", "js-article-comments-box-form" +
				                              "-headline", "js-article-comments-box-nav", "js" +
				                              "-article-comments-box-page-count", "js-article" +
				                              "-comments-box-page-first", "js-article-comments-box" +
				                              "-page-last", "js-article-comments-box-page-next",
				                      "js-article-comments-box-page-prev", "js-article-comments" +
				                              "-toggle-all", "js-article-post-full-text", "js" +
				                              "-article-post-more", "js-article-post-teaser", "js" +
				                              "-blog-login-check", "js-facebook-info-text", "js" +
				                              "-module-box-image", "js-pager-right", "kalooga",
				                      "link-gradient-button", "link-gradient-button-box", "link" +
				                              "-more", "linkedin", "list-float", "list-float-left"
				, "list-listboxtext", "login-links", "module-box", "module-subtitle", "module" +
				                              "-title", "more-social-media", "msocomoff", "nav" +
				                              "-channel", "nav-channel-name", "nav-channel-sub",
				                      "nav-channel-sub-assets", "nav-channel-sub-entry", "nav" +
				                              "-extra", "nav-main", "nav-service", "nav-service" +
				                              "-sub", "other-social-media", "pinterest", "reddit",
				                      "search-form-single", "search-form-submit", "search-options"
				, "so-hdln", "social-media-box", "spArticleContent", "spBereich5050",
				                      "spDivideNaviAd", "spFbTwitterBarInfoText", "spFirst",
				                      "spForumQuote", "spForumQuoteHeadline", "spForumQuoteText",
				                      "spNaviLevel2Last", "spPanoImageTeaserPic", "spPicZoom",
				                      "spSmallScreen", "spTextlinkExt", "spXXLPano", "text-link" +
				                              "-ext", "text-link-int", "timeformat", "top-anchor",
				                      "tumblr", "xing")));
	}

	private DocNode parse(String html) {
		HtmlParser parser = new HtmlParser(html);
		return parser.parseDoc();
	}
	
}