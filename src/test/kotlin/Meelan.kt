package test;

import meelan.*

import at.searles.lexer.Lexer
import at.searles.lexer.SkipTokenizer
import at.searles.parsing.Mapping
import at.searles.parsing.Parser
import at.searles.parsing.Reducer
import at.searles.parsing.Ref
import at.searles.parsing.tools.generator.Context
import at.searles.regex.CharSet
import at.searles.regex.Regex

object Meelan {

    private val tokenizer = SkipTokenizer(Lexer())
    private val context = Context(tokenizer)

    private fun <T> Reducer<T, T>.opt(): Reducer<T, T> {
        return Reducer.opt(this)
    }

    private fun <T> Reducer<T, T>.rep(): Reducer<T, T> {
        return Reducer.rep(this)
    }

    private fun <T> Reducer<T, T>.plus(): Reducer<T, T> {
        return Reducer.rep(this)
    }

    val app = Ref<SyntaxNode>("app")
    val ifExpr = Ref<SyntaxNode>("ifExpr")
    val literal = Ref<SyntaxNode>("literal")
    val pow = Ref<SyntaxNode>("pow")
    val expr = Ref<SyntaxNode>("expr")
    val stmt = Ref<SyntaxNode>("stmt")
    val stmts = Ref<List<SyntaxNode>>("stmts")
    // position [72:131]
    val ws = context.parser(CharSet.interval(9, 9, 11, 11, 13, 13, 32, 32, 160, 160, 5760, 5760, 8192, 8202, 8239, 8239, 8287, 8287, 12288, 12288).plus())

    // position [142:199]
    val nl = context.parser(Regex.text("\r\n").or(Regex.text("\n")).or(Regex.text("\u000c")).or(Regex.text("")).or(Regex.text("\u2028")).or(Regex.text("\u2029")))

    // position [256:282]
    val slComment = context.parser(Regex.text("/*").then(CharSet.interval(0, 2147483646).rep()).then(Regex.text("*/")).nonGreedy())

    // position [296:328]
    val mlComment = context.parser(Regex.text("//").then(CharSet.interval(0, 9, 11, 2147483646).or(CharSet.interval(0, 12, 14, 2147483646)).rep()))

    // position [334:541]
    init {
        tokenizer.addSkipped(ws.tokenId)
        tokenizer.addSkipped(nl.tokenId)
        tokenizer.addSkipped(slComment.tokenId)
        tokenizer.addSkipped(mlComment.tokenId)
    }


    // position [558:576]
    val intRex: Regex = CharSet.interval(48, 57).range(1, 8)

    // position [593:617]
    val hexRex: Regex = CharSet.interval(48, 57, 65, 70, 97, 102).range(1, 8)

    // position [634:653]
    val decimals: Regex = Regex.text(".").then(CharSet.interval(48, 57).rep())

    // position [670:694]
    val exponent: Regex = CharSet.interval(69, 69, 101, 101).then(Regex.text("-").opt()).then(CharSet.interval(48, 57).plus())

    // position [711:767]
    val realRex: Regex = CharSet.interval(48, 57).plus().then(decimals.or(exponent).or(decimals.then(exponent)))

    // position [784:821]
    val identifierRex: Regex = CharSet.interval(65, 90, 95, 95, 97, 122).then(CharSet.interval(48, 57, 65, 90, 95, 95, 97, 122).rep())

    // position [838:877]
    val stringRex: Regex = Regex.text("\"").then(CharSet.interval(0, 33, 35, 91, 93, 2147483646).or(Regex.text("\\").then(CharSet.interval(0, 2147483646))).rep()).then(Regex.text("\""))

    // position [887:923]
    val intNum = context.parser(intRex, Utils.toInt)

    // position [931:970]
    val realNum = context.parser(realRex, Utils.toReal)

    // position [978:1014]
    val hexNum = context.parser(hexRex, Utils.toHex)

    // position [1022:1064]
    val str = context.parser(stringRex, Utils.toEscString)

    // position [1072:1124]
    val identifier = context.parser(identifierRex, Utils.toIdString)

    // position [1134:1178]
    val intNode = intNum.or(hexNum).then(Utils.toIntNode)

    // position [1186:1222]
    val realNode = realNum.then(Utils.toRealNode)

    // position [1230:1266]
    val stringNode = str.then(Utils.toStringNode)

    // position [1274:1309]
    val idNode = identifier.then(Utils.toIdNode)

    // position [1319:1345]
    val comma = context.text(",").annotate(Annot.Comma)

    // position [1353:1413]
    val exprList = Utils.list.then(comma.join(expr.fold(Utils.append)))

    // position [1423:1472]
    val vectorNode = context.text("[").then(exprList).then(Utils.toVectorNode).then(context.text("]"))

    // position [1482:1669]
    val atom = intNode.annotate(Annot.Num).or(realNode.annotate(Annot.Num)).or(stringNode.annotate(Annot.Str)).or(idNode.annotate(Annot.Id)).or(vectorNode).or(context.text("(").then(expr).then(context.text(")")))

    // position [1679:1735]
    val qualified = atom.then(context.text(".").then(identifier.fold(Utils.toQualified)).rep())

    // position [1745:1826]
    val arguments = context.text("(").then(exprList).then(context.text(")")).then(app.fold(Utils.listApply).opt()).or(app.then(Utils.toSingleton))

    // position [1836:1892]
    init {
        app.set(qualified.then(arguments.fold(Utils.toApp).opt()))
    }

    // position [1941:2385]
    init {
        ifExpr.set(context.text("if").annotate(Annot.Kw).then(Utils.properties).then(context.text("(")).then(expr.fold(Utils.set("condition"))).then(context.text(")")).then(stmt.fold(Utils.set("thenBranch"))).then(context.text("else").annotate(Annot.Kw).then(stmt.fold(Utils.set("elseBranch"))).then(Utils.create(IfElse::class.java, true, "condition", "thenBranch", "elseBranch")).or(Utils.create(If::class.java, true, "condition", "thenBranch"))))
    }

    // position [2392:2428]
    val block = context.text("{").then(stmts).then(Utils.toBlock).then(context.text("}"))

    // position [2435:2480]
    val absExpr = context.text("|").then(expr).then(Utils.toUnary(Op.Abs)).then(context.text("|"))

    // position [2490:2526]
    val term = app.or(ifExpr).or(block).or(absExpr)

    // position [2536:2690]
    init {
        literal.set(context.text("-").then(literal).then(Utils.toUnary(Op.Neg)).or(context.text("/").then(literal).then(Utils.toUnary(Op.Recip))).or(literal))
    }

    // position [2724:2781]
    val cons = literal.then(context.text(":").then(literal.fold(Utils.toBinary(Op.Cons))).opt())

    // position [2791:2851]
    init {
        pow.set(cons.then(context.text("^").then(pow.fold(Utils.toBinary(Op.Pow))).opt()))
    }

    // position [2861:2990]
    val product = pow.then(context.text("*").then(pow.fold(Utils.toBinary(Op.Mul))).or(context.text("/").then(pow.fold(Utils.toBinary(Op.Div)))).or(context.text("%").then(pow.fold(Utils.toBinary(Op.Mod)))).rep())

    // position [3000:3098]
    val sum = product.then(context.text("+").then(product.fold(Utils.toBinary(Op.Add))).or(context.text("-").then(product.fold(Utils.toBinary(Op.Sub)))).rep())

    // position [3105:3397]
    val cmp = sum.then(context.text(">").then(sum.fold(Utils.toBinary(Op.Greater))).or(context.text(">=").then(sum.fold(Utils.toBinary(Op.GreaterEqual)))).or(context.text("<=").then(sum.fold(Utils.toBinary(Op.LessEqual)))).or(context.text("<").then(sum.fold(Utils.toBinary(Op.Less)))).or(context.text("==").then(sum.fold(Utils.toBinary(Op.Equal)))).or(context.text("!=").then(sum.fold(Utils.toBinary(Op.NotEqual)))).opt())

    // position [3404:3455]
    val logicalLit = context.text("not").then(cmp).then(Utils.toUnary(Op.Not)).or(cmp)

    // position [3460:3530]
    val logicalAnd = logicalLit.then(context.text("and").then(logicalLit.fold(Utils.toBinary(Op.And))).rep())

    // position [3535:3605]
    val logicalXor = logicalAnd.then(context.text("xor").then(logicalAnd.fold(Utils.toBinary(Op.Xor))).rep())

    // position [3610:3677]
    val logicalOr = logicalXor.then(context.text("or").then(logicalXor.fold(Utils.toBinary(Op.Or))).rep())

    // position [3684:3711]
    init {
        expr.set(logicalOr)
    }

    // position [3718:3775]
    val exprstmt = expr.then(context.text("=").then(expr.fold(Utils.toBinary(Op.Assign))).opt())

    // position [3782:3986]
    val whilestmt = context.text("while").annotate(Annot.Kw).then(Utils.properties).then(context.text("(")).then(expr.fold(Utils.set("condition"))).then(context.text(")")).then(stmt.fold(Utils.set("body")).opt()).then(Utils.create(While::class.java, true, "condition", "body"))

    // position [3993:4212]
    val forstmt = context.text("for").annotate(Annot.Kw).then(Utils.properties).then(context.text("(")).then(identifier.fold(Utils.set("name"))).then(context.text("in")).then(expr.fold(Utils.set("range"))).then(context.text(")")).then(stmt.fold(Utils.set("body"))).then(Utils.create(For::class.java, true, "name", "range", "body"))

    // position [4219:4274]
    init {
        stmt.set(exprstmt.or(whilestmt).or(forstmt).then(context.text(";").opt()))
    }

    // position [4281:4536]
    val vardecl = context.text("var").annotate(Annot.DeclKw).then(Utils.properties).then(identifier.fold(Utils.set("name"))).then(context.text(":").then(identifier.fold(Utils.set("type"))).opt()).then(context.text("=").then(expr.fold(Utils.set("init"))).opt()).then(Utils.create(VarDecl::class.java, true, "name", "type", "init"))

    // position [4543:4784]
    val parameter = context.text("var").annotate(Annot.DeclKw).then(Utils.properties).then(identifier.fold(Utils.set("name"))).then(context.text(":").then(identifier.fold(Utils.set("type"))).opt()).then(Utils.create(VarParameter::class.java, true, "name", "type")).or(idNode)

    // position [4791:4858]
    val parameters = Utils.list.then(comma.join(parameter.fold(Utils.append)))

    // position [4865:5113]
    val fundecl = context.text("fun").annotate(Annot.DeclKw).then(Utils.properties).then(identifier.fold(Utils.set("name"))).then(context.text("(")).then(parameters.fold(Utils.set("parameters"))).then(context.text(")")).then(block.fold(Utils.set("body"))).then(Utils.create(FunDecl::class.java, true, "name", "parameters", "body"))

    // position [5120:5391]
    val classdecl = context.text("class").annotate(Annot.DeclKw).then(Utils.properties).then(identifier.fold(Utils.set("name"))).then(context.text("(").then(parameters).then(context.text(")")).or(Utils.list).fold(Utils.set("parameters"))).then(block.fold(Utils.set("body"))).then(Utils.create(ClassDecl::class.java, true, "name", "parameters", "body"))

    // position [5398:5595]
    val defdecl = context.text("def").annotate(Annot.DeclKw).then(Utils.properties).then(identifier.fold(Utils.set("name"))).then(context.text("=")).then(expr.fold(Utils.set("body"))).then(Utils.create(DefDecl::class.java, true, "name", "body"))

    // position [5611:5663]
    val decl = vardecl.or(fundecl).or(classdecl).or(defdecl).then(context.text(";").opt())

    // position [5670:5759]
    init {
        stmts.set(Utils.list.then(stmt.or(decl).annotate(Annot.Stmt).fold(Utils.append).rep()))
    }

    // position [5764:5794]
    val program = stmts.then(Utils.toBlock)

}
