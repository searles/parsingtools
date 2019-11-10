# Parsing Tools

## Various Combinators

### Dealing with lists

### Create Pojos and generic objects

### Code Formatter



## Parser Generator

This parser generator creates a parser out of grammar files. These
grammar files are simple grammars enriched with kotlin-code.

The following example generates a simple parser that evaluates
mathematical expressions.

~~~
{{{package at.searles.arithmetics;

import at.searles.lexer.Lexer
import at.searles.lexer.SkipTokenizer
import at.searles.parsing.Mapping
import at.searles.parsing.Parser
import at.searles.parsing.Reducer
import at.searles.parsing.Ref
import at.searles.parsing.tools.generator.Context
import at.searles.regex.CharSet
}}}

grammar Arithmetics {
{{{
    private val tokenizer = SkipTokenizer(Lexer())
    private val context = Context(tokenizer)
}}}

    regex ws: [ \n\r\t]+ ;

{{{    init {
        tokenizer.addSkipped(ws.tokenId)
    }
}}}

    num: regex([0-9], `{ num -> num[0].toInt() - '0'.toInt() }`) ;
    term: num | '(' sum ')' ;
    literal: '-' term `Mapping{ _, i: Int -> -i }` | term ;
    product: num ('*' num >> `{_, l: Int, r -> l * r}` | '/' num >> `{_, l: Int, r -> l / r}` )* ;
    sum<`Int`>: num ('+' num >> `{_, l: Int, r -> l + r}` | '-' num >> `{_, l: Int, r -> l - r}` )* ;
}
~~~