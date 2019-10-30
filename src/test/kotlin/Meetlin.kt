package test;

import MeelanUtils.toHex
import MeelanUtils.toInt
import MeelanUtils.toReal
import SyntaxNode
import at.searles.lexer.Lexer
import at.searles.lexer.SkipTokenizer
import at.searles.parsing.Mapping
import at.searles.parsing.Parser
import at.searles.parsing.Reducer
import at.searles.parsing.Ref
import at.searles.parsing.tools.generator.Context
import at.searles.regex.CharSet
import at.searles.regex.Regex

object Meetlin : MeelanUtils {
    val app = Ref<SyntaxNode>("app")
    val ifExpr = Ref<SyntaxNode>("ifExpr")
    val literal = Ref<SyntaxNode>("literal")
    val expr = Ref<SyntaxNode>("expr")
    val stmt = Ref<SyntaxNode>("stmt")
    val stmts = Ref<List<SyntaxNode>>("stmts")
    // position [387:493]

    private val tokenizer = SkipTokenizer(Lexer())
    private val context = Context(tokenizer)


    // position [507:566]
    val ws = context.parser(CharSet.interval(9, 9, 11, 11, 13, 13, 32, 32, 160, 160, 5760, 5760, 8192, 8202, 8239, 8239, 8287, 8287, 12288, 12288).plus())

    // position [577:634]
    val nl = context.parser(Regex.text("\r\n").or(Regex.text("\n")).or(Regex.text("\u000c")).or(Regex.text("")).or(Regex.text("\u2028")).or(Regex.text("\u2029")))

    // position [691:717]
    val slComment = context.parser(Regex.text("/*").then(CharSet.interval(0, 2147483646).rep()).then(Regex.text("*/")).nonGreedy())

    // position [731:763]
    val mlComment = context.parser(Regex.text("//").then(CharSet.interval(0, 9, 11, 2147483646).or(CharSet.interval(0, 12, 14, 2147483646)).rep()))

    // position [769:976]
    init {
        tokenizer.addSkipped(ws.tokenId)
        tokenizer.addSkipped(nl.tokenId)
        tokenizer.addSkipped(slComment.tokenId)
        tokenizer.addSkipped(mlComment.tokenId)
    }


    // position [993:1011]
    val intRex: Regex = CharSet.interval(48, 57).range(1, 8)

    // position [1028:1052]
    val hexRex: Regex = CharSet.interval(48, 57, 65, 70, 97, 102).range(1, 8)

    // position [1069:1088]
    val decimals: Regex = Regex.text(".").then(CharSet.interval(48, 57).rep())

    // position [1105:1129]
    val exponent: Regex = CharSet.interval(69, 69, 101, 101).then(Regex.text("-").opt()).then(CharSet.interval(48, 57).plus())

    // position [1146:1202]
    val realRex: Regex = CharSet.interval(48, 57).plus().then(decimals.or(exponent).or(decimals.then(exponent)))

    // position [1219:1256]
    val identifierRex: Regex = CharSet.interval(65, 90, 95, 95, 97, 122).then(CharSet.interval(48, 57, 65, 90, 95, 95, 97, 122).rep())

    // position [1273:1312]
    val stringRex: Regex = Regex.text("\"").then(CharSet.interval(0, 33, 35, 91, 93, 2147483646).or(Regex.text("\\").then(CharSet.interval(0, 2147483646))).rep()).then(Regex.text("\""))

    // position [1322:1352]
    val intNum = context.parser(intRex, toInt)

    // position [1360:1393]
    val realNum = context.parser(realRex, toReal)

    // position [1401:1431]
    val hexNum = context.parser(hexRex, toHex)

    // position [1439:1475]
    val str = context.parser(stringRex, toEscString)

    // position [1483:1529]
    val identifier = context.parser(identifierRex, toIdString)

    // position [1539:1577]
    val intNode = intNum.or(hexNum).then(toIntNode)

    // position [1585:1615]
    val realNode = realNum.then(toRealNode)

    // position [1623:1653]
    val stringNode = str.then(toStringNode)

    // position [1661:1690]
    val idNode = identifier.then(toIdNode)

    // position [1700:1710]
    val comma = context.text(",")

    // position [1718:1771]
    val exprList = emptyList.then(comma.join(expr.fold(append)))

    // position [1781:1824]
    val vectorNode = context.text("[").then(exprList).then(toVectorNode).then(context.text("]"))

    // position [1834:1982]
    val atom = intNode.annotate(Num).or(realNode.annotate(Num)).or(stringNode.annotate(String)).or(idNode.annotate(Ident)).or(vectorNode).or(context.text("(").then(expr).then(context.text(")")))

    // position [1992:2036]
    val qualified = atom.then(Reducer.rep(context.text(".").then(atom.fold(toQualified))))

    // position [2046:2115]
    val arguments = context.text("(").then(exprList).then(context.text(")")).then(Reducer.opt(app.fold(listApply))).or(app.then(toSingleton))

    // position [2125:2175]
    init {
        app.set(qualified.then(Reducer.opt(arguments.fold(toApp))))
    }

    // position [2224:2616]
    init {
        ifExpr.set(context.text("if").annotate(Keyword).then(properties).then(context.text("(")).then(expr.fold(set("condition"))).then(context.text(")")).then(stmt.fold(set("thenBranch"))).then(context.text("else").then(stmt.fold(put("elseBranch"))).then(Mapping.create(If.class, true, "condition", "thenBranch", "elseBranch")).or(Mapping.create(If.class, true, "condition", "thenBranch"))))
    }

    // position [2623:2653]
    val block = context.text("{").then(stmts).then(toBlock).then(context.text("}"))

    // position [2660:2689]
    val absExpr = context.text("|").then(expr).then(toAbs).then(context.text("|"))

    // position [2699:2735]
    val term = app.or(ifExpr).or(block).or(absExpr)

    // position [2745:2847]
    init {
        literal.set(context.text("-").then(Neg).or(context.text("/").then(Recip)).then(literal.fold(toUnary)).or(literal))
    }

    // position [2881:2937]
    val cplxCons = literal.then(Reducer.opt(context.text(":").then(literal.fold(toBinary(CplxCons)))))

    // position [2947:3000]
    val pow = cplxCons.then(Reducer.rep(context.text("^").then(cplxCons.fold(toBinary(CplxCons)))))

    // position [3010:3112]
    val product = pow.then(Reducer.rep(context.text("*").then(pow.fold(toBinary(Mul))).or(context.text("/").then(pow.fold(toBinary(Div)))).or(context.text("%").then(pow.fold(toBinary(Mod))))))

    // position [3122:3202]
    val sum = product.then(Reducer.rep(context.text("+").then(product.fold(toBinary(Add))).or(context.text("-").then(product.fold(toBinary(Sub))))))

    // position [3209:3447]
    val cmp = sum.then(Reducer.opt(context.text(">").then(sum.fold(toBinary(Greater))).or(context.text(">=").then(sum.fold(toBinary(GreaterEqual)))).or(context.text("<=").then(sum.fold(toBinary(LessEqual)))).or(context.text("<").then(sum.fold(toBinary(Less)))).or(context.text("==").then(sum.fold(toBinary(Equal)))).or(context.text("!=").then(sum.fold(toBinary(NotEqual))))))

    // position [3454:3497]
    val logicalLit = context.text("not").then(Not).then(cmp).then(toUnary).or(cmp)

    // position [3502:3563]
    val logicalAnd = logicalLit.then(Reducer.rep(context.text("and").then(logicalLit.fold(toBinary(And)))))

    // position [3568:3629]
    val logicalXor = logicalAnd.then(Reducer.rep(context.text("xor").then(logicalAnd.fold(toBinary(Xor)))))

    // position [3634:3692]
    val logicalOr = logicalXor.then(Reducer.rep(context.text("or").then(logicalXor.fold(toBinary(Or)))))

    // position [3699:3726]
    init {
        expr.set(logicalOr)
    }

    // position [3733:3781]
    val exprstmt = expr.then(Reducer.opt(context.text("=").then(expr.fold(toBinary(Assign)))))

    // position [3788:3947]
    val whilestmt = context.text("while").then(properties).then(context.text("(")).then(expr.fold(put("condition"))).then(context.text(")")).then(Reducer.opt(stmt.fold(put("body")))).then(create(While.class, true, "condition", body))

    // position [3954:4118]
    val forstmt = context.text("for").then(properties).then(context.text("(")).then(name.fold(put("name"))).then(context.text("in")).then(expr.fold(put("range"))).then(context.text(")")).then(stmt.fold(put("body"))).then(create(For.class, true, "name", "range", "body"))

    // position [4124:4179]
    init {
        stmt.set(exprstmt.or(whilestmt).or(forstmt).then(Reducer.opt(context.text(";"))))
    }

    // position [4186:4352]
    val vardecl = context.text("var").then(properties).then(name.fold(put("name"))).then(Reducer.opt(context.text(":").then(type.fold(put("type"))))).then(Reducer.opt(context.text("=").then(expr.fold(put("init"))))).then(create(Var.class, "name", "type", "init"))

    // position [4359:4426]
    val argument = context.text("var").then(id).then(context.text(":").then(type.fold(TypedVarArg)).or(VarArg)).or(idNode)

    // position [4431:4484]
    val arguments = list.then(comma.join(argument.fold(append)))

    // position [4491:4667]
    val fundecl = context.text("fun").then(properties).then(name.fold(put("name"))).then(context.text("(")).then(arguments.fold(put("arguments"))).then(context.text(")")).then(block.fold(put("body"))).then(create(Fun.class, "name", "arguments", "body"))

    // position [4674:4865]
    val classdecl = context.text("class").then(properties).then(name.fold(put("name"))).then(context.text("(").then(arguments).then(context.text(")")).or(list).fold(put("arguments")
            block >> )).then(put).then(context.text("body")).then(create(Clazz.class, "name", "arguments", "body"))

    // position [4872:4900]
    val defdecl = context.text("def").then(name).then(context.text("=")).then(expr)

    // position [4917:4969]
    val decl = vardecl.or(fundecl).or(classdecl).or(defdecl).then(Reducer.opt(context.text(";")))

    // position [4976:5038]
    init {
        stmts.set(list.then(Reducer.rep(stmt.or(decl).fold(append))))
    }

    // position [5043:5067]
    val program = stmts.then(toBlock)

}