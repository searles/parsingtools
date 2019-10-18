package test;

import at.searles.lexer.Lexer
import at.searles.lexer.SkipTokenizer
import at.searles.parsing.Mapping
import at.searles.parsing.Parser
import at.searles.parsing.Reducer
import at.searles.parsing.Ref
import at.searles.parsing.tools.generator.Context
import at.searles.regex.CharSet
class Arithmetics {
    val sum = Ref<Int>("sum")
    // position [326:429]

    private val tokenizer = SkipTokenizer(Lexer())
    private val context = Context(tokenizer)


    // position [441:455]
    val ws = context.parser(CharSet.interval(9, 10, 13, 13, 32, 32).plus())

    // position [459:523]
    init {
        tokenizer.addSkipped(ws.tokenId)
    }


    // position [529:589]
    val num = context.parser(CharSet.interval(48, 57), { num -> num[0].toInt() - '0'.toInt() })

    // position [596:619]
    val term = num.or(context.text("(").then(sum).then(context.text(")")))

    // position [626:679]
    val literal = context.text("-").then(term).then(Mapping{ _, i: Int -> -i }).or(term)

    // position [686:778]
    val product = num.then(Reducer.rep(context.text("*").then(num.fold({_, l: Int, r -> l * r})).or(context.text("/").then(num.fold({_, l: Int, r -> l / r})))))

    // position [785:880]
    init {
        sum.set(num.then(Reducer.rep(context.text("+").then(num.fold({_, l: Int, r -> l + r})).or(context.text("-").then(num.fold({_, l: Int, r -> l - r}))))))
    }

}