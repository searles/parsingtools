import at.searles.parsing.Fold
import at.searles.parsing.Initializer
import at.searles.parsing.Mapping
import at.searles.parsing.ParserStream
import at.searles.parsing.printing.PartialConcreteSyntaxTree
import at.searles.parsing.utils.builder.Properties
import meelan.Op
import meelan.SyntaxNode

object Utils {

    val toInt = {s: CharSequence -> s.toString().toInt()}
    val toHex = {s: CharSequence -> s.toString().toBigInteger(16).toInt()}
    val toReal = {s: CharSequence -> s.toString().toDouble()}
    val toIdString = {s: CharSequence -> s.toString()}

    object toIntNode: Mapping<Int, SyntaxNode> {
        override fun parse(stream: ParserStream, left: Int): SyntaxNode? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    object toRealNode: Mapping<Double, SyntaxNode> {
        override fun parse(stream: ParserStream, left: Double): SyntaxNode? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    object toStringNode: Mapping<String, SyntaxNode> {
        override fun parse(stream: ParserStream, left: String): SyntaxNode? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    object toIdNode: Mapping<String, SyntaxNode> {
        override fun parse(stream: ParserStream, left: String): SyntaxNode? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    object list: Initializer<List<SyntaxNode>> {
        override fun parse(stream: ParserStream): List<SyntaxNode>? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    object append: Fold<List<SyntaxNode>, SyntaxNode, List<SyntaxNode>> {
        override fun apply(stream: ParserStream, left: List<SyntaxNode>, right: SyntaxNode): List<SyntaxNode> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    object toVectorNode: Mapping<List<SyntaxNode>, SyntaxNode> {
        override fun parse(stream: ParserStream, left: List<SyntaxNode>): SyntaxNode? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    object toQualified: Fold<SyntaxNode, String, SyntaxNode> {
        override fun apply(stream: ParserStream, left: SyntaxNode, right: String): SyntaxNode {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    object toEscString: Mapping<CharSequence, String> {
        override fun parse(stream: ParserStream, left: CharSequence): String? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    object listApply: Fold<List<SyntaxNode>, SyntaxNode, List<SyntaxNode>> {
        override fun apply(stream: ParserStream, left: List<SyntaxNode>, right: SyntaxNode): List<SyntaxNode>? {
            // sin (x+1) y = sin ((x+1)*y)
            // max (x,y) z is an error.

            if(left.size != 1) {
                return null
            }

            return listOf(toApp.apply(stream, left.first(), listOf(right)))
        }

        // no inverse. Other methods will take care of that.
    }

    object toSingleton: Mapping<SyntaxNode, List<SyntaxNode>> {
        override fun parse(stream: ParserStream, left: SyntaxNode): List<SyntaxNode> {
            return listOf(left)
        }

        override fun left(result: List<SyntaxNode>): SyntaxNode? {
            return result.singleOrNull()
        }
    }

    object toApp: Fold<SyntaxNode, List<SyntaxNode>, SyntaxNode> {
        override fun apply(stream: ParserStream, left: SyntaxNode, right: List<SyntaxNode>): SyntaxNode {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    object properties: Initializer<Properties> {
        override fun parse(stream: ParserStream): Properties? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    fun <T> set(property: String): Fold<Properties, T, Properties> {
        return object: Fold<Properties, T, Properties> {
            override fun apply(stream: ParserStream, left: Properties, right: T): Properties {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    fun <T> create(clazz: Class<T>, withSourceInfo: Boolean, vararg properties: String): Mapping<Properties, SyntaxNode> {
        return object: Mapping<Properties, SyntaxNode> {
            override fun parse(stream: ParserStream, left: Properties): SyntaxNode {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    object toBlock: Mapping<List<SyntaxNode>, SyntaxNode> {
        override fun parse(stream: ParserStream, left: List<SyntaxNode>): SyntaxNode? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    fun toUnary(op: Op): Mapping<SyntaxNode, SyntaxNode> {
        return object: Mapping<SyntaxNode, SyntaxNode> {
            override fun parse(stream: ParserStream, left: SyntaxNode): SyntaxNode? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    fun toBinary(op: Op): Fold<SyntaxNode, SyntaxNode, SyntaxNode> {
        return object: Fold<SyntaxNode, SyntaxNode, SyntaxNode> {
            override fun apply(stream: ParserStream, left: SyntaxNode, right: SyntaxNode): SyntaxNode {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }
}
