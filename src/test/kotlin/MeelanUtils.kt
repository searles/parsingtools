import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream

object MeelanUtils {

    val toInt = {s: CharSequence -> s.toString().toInt()}
    val toHex = {s: CharSequence -> s.toString().toBigInteger(16).toInt()}
    val toReal = {s: CharSequence -> s.toString().toDouble()}

    object toEscString: Mapping<CharSequence, String> {
        override fun parse(stream: ParserStream, left: CharSequence): String? {
            // TODO
            return null
        }
    }

    val toIdString = {s: CharSequence -> s.toString()}

    object toIntNode: Mapping<Int, SyntaxNode> {
        override fun parse(stream: ParserStream?, left: Int): SyntaxNode? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    object toRealNode: Mapping<Double, SyntaxNode> {

    }

    object toStringNode: Mapping<String, SyntaxNode> {

    }

    object toIdNode: Mapping<String, SyntaxNode> {

    }




}