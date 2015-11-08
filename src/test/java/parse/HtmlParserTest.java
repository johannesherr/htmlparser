package parse;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
				"junit:junit:4.10",
				"com.google.guava:guava:18.0"
		)));
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
		
		System.out.println("Tags:");
		tags.stream().sorted().forEach(System.out::println);

		System.out.println();
		System.out.println("Classes:");
		classes.stream().sorted().forEach(System.out::println);

	}

	private DocNode parse(String html) {
		HtmlParser parser = new HtmlParser(html);
		return parser.parseDoc();
	}
	
}