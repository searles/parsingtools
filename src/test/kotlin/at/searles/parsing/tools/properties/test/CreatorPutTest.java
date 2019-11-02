package at.searles.parsing.tools.properties.test;

import at.searles.lexer.Lexer;
import at.searles.parsing.*;
import at.searles.parsing.printing.ConcreteSyntaxTree;
import at.searles.parsing.tools.Utils;
import at.searles.regexparser.StringToRegex;;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

public class CreatorPutTest {
    private final Lexer tokenizer = new Lexer();
    private final Parser<Object> id = Parser.fromRegex(StringToRegex.parse("[a-z]+"),
            tokenizer, false,
            new Mapping<CharSequence, Object>() {
                @Override
                public Object parse(ParserStream stream, @NotNull CharSequence left) {
                    return left.toString();
                }

                @Nullable
                @Override
                public CharSequence left(@NotNull Object result) {
                    return result instanceof String ? result.toString() : null;
                }
            });
    final Parser<Object> parser =
            id.then(Reducer.opt(
                            Recognizer.fromString("+", tokenizer, false)
                                    .then(Utils.properties("a"))
                                    .then(Utils.create(Item.class, "a"))
                    ));
    private ParserStream input;
    private Object item; // using object to test inheritance
    private String output;

    @Test
    public void testNoOpt() {
        withInput("k");
        actParse();

        Assert.assertTrue(item instanceof String);
    }

    @Test
    public void testOpt() {
        withInput("k+");
        actParse();

        Assert.assertTrue(item instanceof Item);
    }

    @Test
    public void testOptPrint() {
        withInput("k+");
        actParse();
        actPrint();

        Assert.assertEquals("k+", output);
    }

    @Test
    public void testNoOptPrint() {
        withInput("k");
        actParse();
        actPrint();

        Assert.assertEquals("k", output);
    }

    private void actPrint() {
        ConcreteSyntaxTree tree = parser.print(item);
        output = tree != null ? tree.toString() : null;
    }

    private void actParse() {
        item = parser.parse(input);
    }

    private void withInput(String input) {
        this.input = ParserStream.fromString(input);

    }

    public static class Item {
        private final String a;

        public Item(String a) {
            this.a = a;
        }

        public String getA() {
            return a;
        }
    }
}
