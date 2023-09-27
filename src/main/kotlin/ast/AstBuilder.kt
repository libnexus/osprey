package ast

import OspreyClass.Companion.SyntaxErrorOspreyClass
import Lexeme
import Lexer
import OspreyClass.Companion.FALSE
import OspreyClass.Companion.NOTHING
import OspreyClass.Companion.TRUE
import OspreyThrowable
import Type
import java.util.Stack

class AstBuilder(source: String, fileName: String) {
    private val lexer = Lexer(source, fileName)
    private val lexemes = lexer.parseLexemes()
    private var lexemesIndex = 0

    private var lastLexeme: Lexeme
    private var lexeme: Lexeme
    private var nextLexeme: Lexeme

    init {
        lastLexeme = lexemes[0]
        lexeme = lexemes[0]
        nextLexeme = lexemes[1]
        lexemesIndex++
    }

    private fun identityOf(lexeme: Lexeme): String = when (lexeme.type) {
        Type.KEYWORD -> {
            "keyword '%s'".format(lexeme.value)
        }

        Type.IDENTIFIER -> {
            "name '%s'".format(lexeme.value)
        }

        Type.STRING -> {
            "string '%s'".format(lexeme.value)
        }

        Type.FLOAT -> {
            "float %s".format(lexeme.value)
        }

        Type.INTEGER -> {
            "integer %s".format(lexeme.value)
        }

        else -> {
            lexeme.type.looksLike()
        }
    }

    private fun advance() {
        // print("MOVING:\n    < %s\n    ^ %s\n    ^ %s".format(this.lastLexeme, this.lexeme, this.nextLexeme))
        lexemesIndex++
        this.lastLexeme = this.lexeme
        this.lexeme = this.nextLexeme
        if (lexemesIndex < this.lexemes.size) {
            this.nextLexeme = this.lexemes[lexemesIndex]
        }
        // println("\n    ^ %s\n".format(this.nextLexeme))
    }

    fun eat(type: Type): Lexeme {
        if (this.lexeme.type != type) {
            throw OspreyThrowable(
                SyntaxErrorOspreyClass, this.lexer.syntaxError(
                    "Expected %s but got: %s".format(type.looksLike(), identityOf(this.lexeme)), this.lexeme
                )
            )

        }
        val lexeme = this.lexeme
        this.advance()
        return lexeme
    }

    private fun eatKeyword(keyword: String): Lexeme {
        if (this.lexeme.type != Type.KEYWORD || this.lexeme.value != keyword) {
            throw OspreyThrowable(
                SyntaxErrorOspreyClass, this.lexer.syntaxError(
                    "Expected keyword '%s' but got: %s".format(keyword, identityOf(this.lexeme)), this.lexeme
                )
            )
        }
        val lexeme = this.lexeme
        this.advance()
        return lexeme
    }

    private fun statements(delimiter: Type): Statements {
        val head = this.lexeme
        val statements = Stack<Statement>()
        while (true) {
            if (this.lexeme.type == delimiter) {
                break
            }
            statements.add(this.multiLineStatement())
        }
        return Statements(statements.toTypedArray(), head)
    }

    private fun scope(): Statement {
        this.eat(Type.COLON)
        return if (this.lexeme.type == Type.END_LINE) {
            this.advance()
            this.eat(Type.INDENT)
            val statements = this.statements(Type.DEDENT)
            this.eat(Type.DEDENT)
            statements
        } else {
            this.singleLineStatement()
        }
    }

    private fun names(): Array<String> {
        val names = ArrayList<String>()
        names.add(this.eat(Type.IDENTIFIER).value)
        while (this.lexeme.type == Type.COMMA) {
            this.advance()
            names.add(this.eat(Type.IDENTIFIER).value)
        }
        return names.toTypedArray()
    }

    fun build(): Statements {
        val head = this.lexeme
        val statements = ArrayList<Statement>()
        while (this.lexeme.type != Type.END_OF_FILE) {
            val statement = this.multiLineStatement()
            statements.add(statement)
        }
        this.eat(Type.END_OF_FILE)
        return Statements(statements.toTypedArray(), head)
    }

    private fun multiLineStatement(): Statement {
        val head = this.lexeme
        return this.singleLineStatement()
    }

    private fun singleLineStatement(): Statement {
        val head = this.lexeme
        val expressions = this.expressionSequence()
        val expression = if (expressions.size == 1) {
            expressions[0]
        } else {
            TupleExpression(expressions, head)
        }


        if (this.lexeme.type == Type.EQUALS) {
            this.advance()
            if (expressions.all { it is Lookup || it is GetAttribute || it is MacroCall }) {
                val assignmentValue = TupleExpression(this.expressionSequence(), head)
                this.eat(Type.END_LINE)
                return ExpressionStatement(AssignmentsExpression(expressions.map {
                    when (it) {
                        is Lookup -> NameAssignment(it.name)
                        is GetAttribute -> AttributeAssignment(it.subject, it.name)
                        is MacroCall -> MacroAssignment(it.subject, it.args)
                        else -> {
                            throw OspreyThrowable(SyntaxErrorOspreyClass, "Impossible assignment error")
                        }
                    }
                }.toTypedArray(), assignmentValue, head), head)
            } else {
                throw OspreyThrowable(
                    SyntaxErrorOspreyClass,
                    this.lexer.syntaxError("Invalid assignment target/s", head)
                )
            }
        } else {
            this.eat(Type.END_LINE)
            return ExpressionStatement(expression, head)
        }
    }

    private fun expression(): Expression {
        return this.boolean()
    }

    private fun expressionSequence(): Array<Expression> {
        val expressions = ArrayList<Expression>()
        expressions.add(this.expression())
        while (this.lexeme.type == Type.COMMA) {
            this.advance()
            expressions.add(this.expression())
        }
        return expressions.toTypedArray()
    }

    private fun boolean(): Expression {
        var left = this.identity()
        while (true) {
            val operator = this.lexeme
            left = if (this.lexeme.isKeyword("and")) {
                this.advance()
                BinaryExpression(BinaryExpressionType.AND, left, this.identity(), operator)
            } else if (this.lexeme.isKeyword("or")) {
                this.advance()
                BinaryExpression(BinaryExpressionType.OR, left, this.identity(), operator)
            } else {
                break
            }
        }
        return left
    }

    private fun identity(): Expression {
        var left = this.binary(0)
        while (true) {
            val operator = this.lexeme
            left = if (this.lexeme.isKeyword("is")) {
                this.advance()
                if (this.lexeme.isKeyword("not")) {
                    this.advance()
                    if (this.lexeme.isKeyword("in")) {
                        this.advance()
                        BinaryExpression(BinaryExpressionType.IS_NOT_IN, left, this.binary(0), operator)
                    } else {
                        val right = this.binary(0)
                        if (this.lexeme.isKeyword("instance")) {
                            this.advance()
                            BinaryExpression(BinaryExpressionType.IS_NOT_INSTANCE, left, right, operator)
                        } else {
                            BinaryExpression(BinaryExpressionType.IS_NOT, left, right, operator)
                        }
                    }
                } else if (this.lexeme.isKeyword("in")) {
                    this.advance()
                    BinaryExpression(BinaryExpressionType.IS_IN, left, this.binary(0), operator)
                } else {
                    val right = this.binary(0)
                    if (this.lexeme.isKeyword("instance")) {
                        this.advance()
                        BinaryExpression(BinaryExpressionType.IS_INSTANCE, left, right, operator)
                    } else {
                        BinaryExpression(BinaryExpressionType.IS, left, right, operator)
                    }
                }
            } else {
                break
            }
        }
        return left
    }

    private fun binary(precedence: Int): Expression {
        if (precedence > 5) {
            return this.atomAccess()
        } else {
            var left = this.binary(precedence + 1)
            while (true) {
                val operator = this.lexeme
                val operatorPrecedence = when (operator.type) {
                    Type.DOUBLE_EQUALS -> 1
                    Type.BANG_EQUALS -> 1
                    Type.LESS_THAN -> 1
                    Type.MORE_THAN -> 1
                    Type.LESS_THAN_OR_EQUALS -> 1
                    Type.MORE_THAN_OR_EQUALS -> 1
                    Type.DOUBLE_QUESTION -> 2
                    Type.PLUS -> 3
                    Type.MINUS -> 3
                    Type.STAR -> 4
                    Type.SLASH -> 4
                    Type.DOUBLE_STAR -> 5
                    Type.PERCENT -> 5
                    else -> -1
                }
                if (operatorPrecedence == precedence) {
                    this.advance()
                    left = BinaryExpression(
                        when (operator.type) {
                            Type.EQUALS -> BinaryExpressionType.EQUALS
                            Type.BANG_EQUALS -> BinaryExpressionType.NOT_EQUALS
                            Type.LESS_THAN -> BinaryExpressionType.LESS_THAN
                            Type.MORE_THAN -> BinaryExpressionType.MORE_THAN
                            Type.LESS_THAN_OR_EQUALS -> BinaryExpressionType.LESS_THAN_OR_EQ
                            Type.MORE_THAN_OR_EQUALS -> BinaryExpressionType.MORE_THAN_OR_EQ
                            Type.DOUBLE_QUESTION -> BinaryExpressionType.NOT_NULL_OR
                            Type.PLUS -> BinaryExpressionType.PLUS
                            Type.MINUS -> BinaryExpressionType.MINUS
                            Type.STAR -> BinaryExpressionType.MULTIPLY
                            Type.SLASH -> BinaryExpressionType.DIVIDE
                            Type.DOUBLE_STAR -> BinaryExpressionType.POW
                            Type.PERCENT -> BinaryExpressionType.MODULO
                            else -> throw OspreyThrowable(OspreyClass.FatalOspreyClass, "Error code 1591524")
                        }, left, this.binary(precedence + 1), operator
                    )
                } else {
                    break
                }
            }
            return left
        }
    }

    private fun callArguments(): Pair<Array<Expression>, HashMap<String, Expression>> {
        val args = ArrayList<Expression>()
        val keywords = HashMap<String, Expression>()
        while (true) {
            val expression = this.expression()
            if (this.lexeme.type == Type.EQUALS) {
                this.advance()
                if (expression is Lookup) {
                    val name = expression.name
                    if (keywords.containsKey(name)) {
                        throw OspreyThrowable(
                            SyntaxErrorOspreyClass,
                            this.lexer.syntaxError("Keyword %s can't be used twice".format(name), expression.at)
                        )
                    } else {
                        keywords[name] = this.expression()
                    }
                } else {
                    throw OspreyThrowable(
                        SyntaxErrorOspreyClass,
                        this.lexer.syntaxError("Expected keyword to be a name", expression.at)
                    )
                }
            } else {
                args.add(expression)
            }

            if (this.lexeme.type == Type.COMMA) {
                this.advance()
            } else {
                break
            }
        }
        return Pair(args.toTypedArray(), keywords)
    }

    private fun giveArguments(): Pair<Array<AnnotatedName>, HashMap<String, AnnotatedExpression>> {
        // TODO redo entire method, got confused with parameters AGAIN
        val args = ArrayList<AnnotatedName>()
        val keywords = HashMap<String, AnnotatedExpression>()
        val usedNames = HashMap<String, Boolean>()
        while (true) {
            var annotation: Expression? = null
            val name = this.eat(Type.IDENTIFIER)

            if (usedNames.getOrDefault(name.value, false)) {
                throw OspreyThrowable(
                    SyntaxErrorOspreyClass,
                    this.lexer.syntaxError("The name \"%s\" is already a parameter name".format(name.value), name)
                )
            } else {
                usedNames[name.value] = true
            }

            if (this.lexeme.type == Type.COLON) {
                this.advance()
                annotation = this.expression()
            }

            if (this.lexeme.type == Type.EQUALS) {
                this.advance()
                val expression = this.expression()
                keywords[name.value] = AnnotatedExpression(expression, annotation)
            } else {
                args.add(AnnotatedName(name.value, annotation))
            }

            if (this.lexeme.type == Type.COMMA) {
                this.advance()
            } else {
                break
            }
        }
        return Pair(args.toTypedArray(), keywords)
    }

    private fun atomAccess(): Expression {
        var subject = this.atom()
        while (true) {
            val operator = this.lexeme
            when (operator.type) {
                Type.OPEN_PARENS -> {
                    this.advance()
                    var args: Array<Expression>? = null
                    var keywords: HashMap<String, Expression>? = null
                    if (this.lexeme.type != Type.CLOSE_PARENS) {
                        val givenArguments = this.callArguments()
                        args = givenArguments.first
                        keywords = givenArguments.second
                    }
                    this.eat(Type.CLOSE_PARENS)
                    subject = Call(subject, args, keywords, operator)
                }

                Type.OPEN_BRACKETS -> {
                    this.advance()
                    var args: Array<Expression>? = null
                    if (this.lexeme.type != Type.CLOSE_BRACKETS) {
                        val givenArguments = this.callArguments()
                        args = givenArguments.first
                        if (givenArguments.second.isNotEmpty()) {
                            throw OspreyThrowable(
                                SyntaxErrorOspreyClass,
                                this.lexer.syntaxError(
                                    "Macros cannot be given any keyword arguments. Supplied \"%s\"".format(
                                        givenArguments.second.keys.joinToString("\", \"")
                                    ), operator
                                )
                            )
                        }
                    }
                    this.eat(Type.CLOSE_BRACKETS)
                    subject = MacroCall(subject, args, operator)
                }

                Type.DOT -> {
                    this.advance()
                    val name = this.eat(Type.IDENTIFIER).value
                    subject = GetAttribute(subject, name, operator)
                }

                Type.OPEN_CURLY -> {
                    this.advance()
                    val expressions = this.expressionSequence()
                    this.eat(Type.CLOSE_CURLY)
                    if (subject is Call && !subject.insertUnit(ExpressionUnit(expressions, operator))) {
                        throw OspreyThrowable(
                            SyntaxErrorOspreyClass,
                            this.lexer.syntaxError(
                                "Cannot add an expression unit to a call with an expression unit already present",
                                operator
                            )
                        )
                    } else {
                        subject = Call(subject, arrayOf(ExpressionUnit(expressions, operator)), null, operator)
                    }
                }

                else -> break
            }
        }
        return subject
    }

    /**
     * Returns an atomic expression (lowest level of the parser)
     *
     * Current atoms:
     *      Lookup,
     *      VariableAssignment,
     *      String,
     *      Float,
     *      Integer,
     *      Parenthesized Expression,
     *      List,
     *      Dictionary,
     *      Tuple,
     *      True, False, Nothing,
     *      Get Reference,
     *      Dereference (atom),
     */
    private fun atom(): Expression {
        val atomHead = this.lexeme

        return when (atomHead.type) {
            Type.IDENTIFIER -> {
                this.advance()
                if (this.lexeme.type == Type.ARROW_LEFT) {
                    this.advance()
                    AssignmentExpression(NameAssignment(atomHead.value), this.expression(), atomHead)
                } else {
                    Lookup(atomHead.value, atomHead)
                }
            }

            Type.STRING -> {
                this.advance()
                LiteralString(atomHead.value, atomHead)
            }

            Type.FLOAT -> {
                this.advance()
                LiteralFloat(atomHead.value.toFloat(), atomHead)
            }

            Type.INTEGER -> {
                this.advance()
                LiteralInteger(atomHead.value.toInt(), atomHead)
            }

            Type.OPEN_PARENS -> {
                this.advance()
                val expression = this.expression()
                this.eat(Type.CLOSE_PARENS)
                expression
            }

            Type.OPEN_BRACKETS -> {
                this.advance()
                if (this.lexeme.type == Type.CLOSE_BRACKETS) {
                    this.advance()
                    ListExpression(arrayOf(), atomHead)
                }

                val first = this.expression()
                if (this.lexeme.isKeyword("for")) {
                    this.advance()
                    val names = this.names()
                    this.eatKeyword("in")
                    val iterator = this.expression()
                    var discriminator: Expression? = null
                    if (this.lexeme.isKeyword("if")) {
                        this.advance()
                        discriminator = this.expression()
                    }
                    this.eat(Type.CLOSE_BRACKETS)
                    ListComprehension(first, names, iterator, discriminator, atomHead)
                } else {
                    val values = ArrayList<Expression>()
                    values.add(first)
                    while (this.lexeme.type == Type.COMMA) {
                        this.advance()
                        values.add(this.expression())
                        if (this.lexeme.type == Type.CLOSE_BRACKETS) {
                            break
                        }
                    }
                    this.eat(Type.CLOSE_BRACKETS)
                    ListExpression(values.toTypedArray(), atomHead)
                }
            }

            Type.OPEN_CURLY -> {
                this.advance()
                if (this.lexeme.type == Type.CLOSE_CURLY) {
                    this.advance()
                    DictionaryExpression(arrayOf(), atomHead)
                } else if (this.lexeme.type == Type.COMMA) {
                    this.advance()
                    this.eat(Type.CLOSE_CURLY)
                    TupleExpression(arrayOf(), atomHead)
                } else {
                    val values = ArrayList<Expression>()
                    values.add(this.expression())
                    if (this.lexeme.type == Type.COLON) {
                        this.advance()
                        values.add(this.expression())
                        while (this.lexeme.type == Type.COMMA) {
                            this.advance()
                            values.add(this.expression())
                            this.eat(Type.COLON)
                            values.add(this.expression())
                            if (this.lexeme.type == Type.CLOSE_CURLY) {
                                break
                            }
                        }
                        this.eat(Type.CLOSE_CURLY)
                        DictionaryExpression(values.toTypedArray(), atomHead)
                    } else {
                        while (this.lexeme.type == Type.COMMA) {
                            this.advance()
                            values.add(this.expression())
                            if (this.lexeme.type == Type.CLOSE_CURLY) {
                                break
                            }
                        }
                        this.eat(Type.CLOSE_CURLY)
                        TupleExpression(values.toTypedArray(), atomHead)
                    }
                }
            }

            Type.KEYWORD -> {
                this.advance()
                when (atomHead.value) {
                    "true" -> Constant(TRUE, atomHead)
                    "false" -> Constant(FALSE, atomHead)
                    "nothing" -> Constant(NOTHING, atomHead)
                    "ref" -> GetReference(this.eat(Type.IDENTIFIER).value, atomHead)
                    "deref" -> Dereference(this.atom(), atomHead)
                    "lambda" -> Lambda(this.expression(), atomHead)
                    else -> throw OspreyThrowable(
                        SyntaxErrorOspreyClass,
                        this.lexer.syntaxError("Unexpected keyword '%s'".format(atomHead.value), atomHead)
                    )
                }
            }

            else -> throw OspreyThrowable(
                SyntaxErrorOspreyClass, this.lexer.syntaxError("Unexpected %s".format(identityOf(atomHead)), atomHead)
            )
        }
    }
}