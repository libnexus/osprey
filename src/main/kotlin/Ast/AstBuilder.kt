package Ast

import OspreyClass.Companion.SyntaxErrorOspreyClass
import Lexeme
import Lexer
import OspreyClass.Companion.FALSE
import OspreyClass.Companion.NOTHING
import OspreyClass.Companion.TRUE
import OspreyThrowable
import Type
import java.util.*

fun precedence(operator: Type) : Int = when(operator) {
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
                SyntaxErrorOspreyClass,
                this.lexer.syntaxError(
                    "Expected %s but got: %s".format(type.looksLike(), identityOf(this.lexeme)),
                    this.lexeme
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
                SyntaxErrorOspreyClass,
                this.lexer.syntaxError(
                    "Expected keyword '%s' but got: %s".format(keyword, identityOf(this.lexeme)),
                    this.lexeme
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
            statements.add(this.multiLineStatement())
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
        run {
            val expression = ExpressionStatement(this.expression(), head)
            this.eat(Type.END_LINE)
            return expression
        }
    }

    private fun expression(): Expression {
        return this.binary(0)
    }

    private fun binary(precedence: Int) : Expression {
        if (precedence > 5) {
            return this.atomAccess()
        } else {
            var left = this.binary(precedence + 1)
            while (true) {
                val operator = this.lexeme
                val operatorPrecedence = precedence(operator.type)
                if (operatorPrecedence == precedence) {
                    this.advance()
                    left = BinaryExpression(BinaryExpressionType.of(operator.type), left, this.binary(precedence + 1), operator)
                } else {
                    break
                }
            }
            return left
        }
    }

    private fun atomAccess() : Expression {
        return this.atom()
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
                    VariableAssignment(atomHead.value, this.expression(), atomHead)
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
                        this.lexer.syntaxError("Unexpected keyword '%s'".format(atomHead.value), atomHead))
                }
            }

            else -> throw OspreyThrowable(
                SyntaxErrorOspreyClass,
                this.lexer.syntaxError("Unexpected %s".format(identityOf(atomHead)), atomHead)
            )
        }
    }
}