package Ast

import Lexeme
import OspreyObject
import OspreyThrowable
import Type

abstract class Node(val at: Lexeme)
abstract class Statement(at: Lexeme) : Node(at)
abstract class Expression(at: Lexeme) : Node(at)

// Statement Types

class Statements(val statements: Array<Statement>, at: Lexeme) : Statement(at)
class ExpressionStatement(val expression: Expression, at: Lexeme) : Statement(at)

// Expression Types

abstract class Expressions(val expressions: Array<Expression>, at: Lexeme) : Expression(at)
class Lookup(val name: String, at: Lexeme) : Expression(at)
class VariableAssignment(val name: String, val value: Expression, at: Lexeme) : Expression(at)
class ListComprehension(
    val expression: Expression,
    val names: Array<String>,
    val iterator: Expression,
    val discriminator: Expression?,
    at: Lexeme
) : Expression(at)

class ListExpression(values: Array<Expression>, at: Lexeme) : Expressions(values, at)
class TupleExpression(values: Array<Expression>, at: Lexeme) : Expressions(values, at)
class DictionaryExpression(keyPairs: Array<Expression>, at: Lexeme) : Expressions(keyPairs, at)


// Atom Extras
enum class BinaryExpressionType {
    AND, OR,
    IS, IS_NOT, IS_IN, IS_NOT_IN,
    EQUALS, NOT_EQUALS, LESS_THAN, MORE_THAN, LESS_THAN_OR_EQ, MORE_THAN_OR_EQ,
    NOT_NULL_OR,
    PLUS, MINUS, MULTIPLY, DIVIDE, POW, MODULO;

    companion object {
        fun of(operator: Type) : BinaryExpressionType = when(operator) {
            Type.EQUALS -> EQUALS
            Type.BANG_EQUALS -> NOT_EQUALS
            Type.LESS_THAN -> LESS_THAN
            Type.MORE_THAN -> MORE_THAN
            Type.LESS_THAN_OR_EQUALS -> LESS_THAN_OR_EQ
            Type.MORE_THAN_OR_EQUALS -> MORE_THAN_OR_EQ
            Type.DOUBLE_QUESTION -> NOT_NULL_OR
            Type.PLUS -> PLUS
            Type.MINUS -> MINUS
            Type.STAR -> MULTIPLY
            Type.SLASH -> DIVIDE
            Type.DOUBLE_STAR -> POW
            Type.PERCENT -> MODULO
            else -> throw OspreyThrowable(OspreyClass.FatalOspreyClass, "Error code 1591524")
        }
    }
}

class BinaryExpression(val operator: BinaryExpressionType, val left: Expression, val right: Expression, at: Lexeme) : Expression(at)


// Expression Literals

class LiteralString(val string: String, at: Lexeme) : Expression(at)
class LiteralFloat(val float: Float, at: Lexeme) : Expression(at)
class LiteralInteger(val int: Int, at: Lexeme) : Expression(at)
class Constant(val constant: OspreyObject, at: Lexeme) : Expression(at)
class GetReference(val name: String, at: Lexeme) : Expression(at)
class Dereference(val expression: Expression, at: Lexeme) : Expression(at)
class Lambda(val expression: Expression, at: Lexeme) : Expression(at)