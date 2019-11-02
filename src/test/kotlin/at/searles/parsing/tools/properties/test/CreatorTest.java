package at.searles.parsing.tools.properties.test;

import at.searles.lexer.Lexer;
import at.searles.parsing.*;
import at.searles.parsing.printing.ConcreteSyntaxTree;
import at.searles.parsing.tools.Utils;
import at.searles.parsing.tools.properties.PojoCreator;
import at.searles.parsing.tools.properties.Properties;
import at.searles.parsing.tools.common.ToString;
import at.searles.regexparser.StringToRegex;
import org.junit.Assert;
import org.junit.Test;

public class CreatorTest {

    private final Lexer tokenizer = new Lexer();

    private final Parser<String> id = Parser.fromRegex(StringToRegex.parse("[a-z]+"), tokenizer, false, new ToString());

    final Parser<Properties> propertiesParser = Utils.properties().then(
            Recognizer.fromString(",", tokenizer, false).join(
                    Utils.put("a", Recognizer.fromString("+", tokenizer, false).then(id))
                    .or(Utils.put("b", Recognizer.fromString("-", tokenizer, false).then(id)), true)
            )
    );

    final Parser<Item1> parser1 = propertiesParser.then(Utils.create(Item1.class, "a", "b"));
    final Parser<Item2> parser2 = propertiesParser.then(Utils.create(Item2.class, true, "a", "b"));
    final Parser<Item3> parser3 = propertiesParser.then(new PojoCreator<>(Item3.class));

    private ParserStream input;
    private String output;

    @Test
    public void testEmpty1() {
        withInput("");

        Item1 item = actParse(parser1);
        actPrint(parser1, item);

        Assert.assertEquals("", output);
    }

    @Test
    public void testAB1() {
        withInput("+zyx,-wvu");
        Item1 item = actParse(parser1);
        actPrint(parser1, item);

        Assert.assertEquals("+zyx,-wvu", output);
    }

    @Test
    public void testAA1() {
        withInput("+zyx,+wvu");
        Item1 item = actParse(parser1);
        actPrint(parser1, item);

        Assert.assertEquals("+wvu", output);
    }

    @Test
    public void testEmpty2() {
        withInput("");

        Item2 item = actParse(parser2);
        actPrint(parser2, item);

        Assert.assertEquals("", output);
    }

    @Test
    public void testAB2() {
        withInput("+zyx,-wvu");
        Item2 item = actParse(parser2);
        actPrint(parser2, item);

        Assert.assertEquals("+zyx,-wvu", output);
    }

    @Test
    public void testAA2() {
        withInput("+zyx,+wvu");
        Item2 item = actParse(parser2);
        actPrint(parser2, item);

        Assert.assertEquals("+wvu", output);
    }

    @Test
    public void testEmpty3() {
        withInput("");

        Item3 item = actParse(parser3);

        Assert.assertNull(item.getA());
        Assert.assertNull(item.getB());
    }

    @Test
    public void testAB3() {
        withInput("+zyx,-wvu");
        Item3 item = actParse(parser3);

        Assert.assertEquals("zyx", item.getA());
        Assert.assertEquals("wvu", item.getB());
    }

    @Test
    public void testAA3() {
        withInput("+zyx,+wvu");
        Item3 item = actParse(parser3);

        Assert.assertEquals("wvu", item.getA());
        Assert.assertNull(item.getB());
    }

    private <T> void actPrint(Parser<T> parser, T item) {
        ConcreteSyntaxTree tree = parser.print(item);
        output = tree != null ? tree.toString() : null;
    }

    private <T> T actParse(Parser<T> parser) {
        return parser.parse(input);
    }

    private void withInput(String input) {
        this.input = ParserStream.fromString(input);

    }

    public static class Item1 {
        private final String mA;
        private final String mB;

        public Item1(String a, String b) {
            this.mA = a;
            this.mB = b;
        }

        public String getA() {
            return mA;
        }

        public String getB() {
            return mB;
        }
    }

    public static class Item2 {
        private final String a;
        private final String b;

        public Item2(ParserStream stream, String a, String b) {
            this.a = a;
            this.b = b;
        }

        public String getA() {
            return a;
        }

        public String getB() {
            return b;
        }
    }

    public static class Item3 {
        private String mA;
        private String mB;

        public String getA() {
            return mA;
        }

        public void setA(String a) {
            this.mA = a;
        }

        public String getB() {
            return mB;
        }

        public void setB(String b) {
            this.mB = b;
        }
    }
}
