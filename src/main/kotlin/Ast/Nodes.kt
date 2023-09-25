package Ast

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
class VariableAssignment(val name: String, val value: Expression, at: Lexeme) : Expression(at)
class ListComprehension(val expression: Expression, val names: Array<String>, val iterator: Expression, val discriminator: Expression?, at: Lexeme) : Expression(at)
class ListExpression(values: Array<Expression>, at: Lexeme) : Expressions(values, at)
class TupleExpression(values: Array<Expression>, at: Lexeme) : Expressions(values, at)
class DictionaryExpression(keyPairs: Array<Expression>, at: Lexeme) : Expressions(keyPairs, at)


// Expression Literals

class LiteralString(val string: String, at: Lexeme) : Expression(at)
class LiteralFloat(val float: Float, at: Lexeme) : Expression(at)
class LiteralInteger(val int: Int, at: Lexeme) : Expression(at)
class Constant(val constant: OspreyObject, at: Lexeme) : Expression(at)
class GetReference(val name: String, at: Lexeme) : Expression(at)
class Dereference(val expression: Expression, at: Lexeme) : Expression(at)
class Lambda(val expression: Expression, at: Lexeme) : Expression(at)