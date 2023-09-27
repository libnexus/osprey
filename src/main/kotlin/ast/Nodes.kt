package ast

import Lexeme
import OspreyObject

abstract class Node(val at: Lexeme)
abstract class Statement(at: Lexeme) : Node(at)
abstract class Expression(at: Lexeme) : Node(at)

// Statement Types

class Statements(val statements: Array<Statement>, at: Lexeme) : Statement(at)
class ExpressionStatement(val expression: Expression, at: Lexeme) : Statement(at)

// Expression Types

abstract class Expressions(val expressions: Array<Expression>, at: Lexeme) : Expression(at)
class Lookup(val name: String, at: Lexeme) : Expression(at)
abstract class SomeAssignment
class AttributeAssignment(val subject: Expression, val name: String) : SomeAssignment()
class MacroAssignment(val subject: Expression, val macroArgs: Array<Expression>?) : SomeAssignment()
class NameAssignment(val name: String) : SomeAssignment()
class AssignmentExpression(val assignment: SomeAssignment, val expression: Expression, at: Lexeme) : Expression(at)
class AssignmentsExpression(val assignments: Array<SomeAssignment>, val expression: Expression, at: Lexeme) : Expression(at)

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
class Call(
    val subject: Expression,
    private var args: Array<Expression>?,
    val keywords: HashMap<String, Expression>?,
    at: Lexeme
) : Expression(at) {
    private var unitAltered = false

    fun args() = this.args

    fun insertUnit(unit: ExpressionUnit) : Boolean {
        return if (this.unitAltered) {
            false
        } else {
            if (args == null) {
                this.args = arrayOf(unit)
            } else {
                val newArgs = this.args!!.toMutableList()
                newArgs.add(unit)
                this.args = newArgs.toTypedArray()
            }
            this.unitAltered = true
            true
        }
    }
}

class AnnotatedExpression(val expression: Expression, val annotation: Expression?)
class AnnotatedName(val name: String, val annotation: Expression?)
class CreateFunction(
    val name: String,
    val args: Array<AnnotatedName>,
    val varArgs: String?,
    val defaults: HashMap<String, AnnotatedExpression>?,
    val keywords: String?,
    val statements: Statements,
    at: Lexeme
) : Expression(at)

class ExpressionUnit(
    val expressions: Array<Expression>,
    at: Lexeme
) : Expression(at)

class MacroCall(
    val subject: Expression,
    val args: Array<Expression>?,
    at: Lexeme
) : Expression(at)

class GetAttribute(val subject: Expression, val name: String, at: Lexeme) : Expression(at)


// Atom Extras
enum class BinaryExpressionType {
    AND, OR,
    IS, IS_NOT, IS_IN, IS_NOT_IN, IS_INSTANCE, IS_NOT_INSTANCE,
    EQUALS, NOT_EQUALS, LESS_THAN, MORE_THAN, LESS_THAN_OR_EQ, MORE_THAN_OR_EQ,
    NOT_NULL_OR,
    PLUS, MINUS, MULTIPLY, DIVIDE, POW, MODULO;
}

class BinaryExpression(val operator: BinaryExpressionType, val left: Expression, val right: Expression, at: Lexeme) :
    Expression(at)


// Expression Literals

class LiteralString(val string: String, at: Lexeme) : Expression(at)
class LiteralFloat(val float: Float, at: Lexeme) : Expression(at)
class LiteralInteger(val int: Int, at: Lexeme) : Expression(at)
class Constant(val constant: OspreyObject, at: Lexeme) : Expression(at)
class GetReference(val name: String, at: Lexeme) : Expression(at)
class Dereference(val expression: Expression, at: Lexeme) : Expression(at)
class Lambda(val expression: Expression, at: Lexeme) : Expression(at)