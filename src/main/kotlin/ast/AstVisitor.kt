package ast

import OspreyThrowable

interface AstVisitor {
    fun visit(node: Node) = when (node) {
        is Statements -> this.visitStatements(node)
        is ExpressionStatement -> this.visitExpressionStatement(node)
        is Lookup -> this.visitLookup(node)
        is VariableAssignment -> this.visitVariableAssignment(node)
        is ListComprehension -> this.visitListComprehension(node)
        is ListExpression -> this.visitListExpression(node)
        is TupleExpression -> this.visitTupleExpression(node)
        is DictionaryExpression -> this.visitDictionaryExpression(node)
        is LiteralString -> this.visitLiteralString(node)
        is LiteralFloat -> this.visitLiteralFloat(node)
        is LiteralInteger -> this.visitLiteralInteger(node)
        is Constant -> this.visitConstant(node)
        is GetReference -> this.visitGetReference(node)
        is Dereference -> this.visitDereference(node)
        is Lambda -> this.visitLambda(node)
        is BinaryExpression -> this.visitBinaryExpression(node)
        is Call -> this.visitCall(node)
        is MacroCall -> this.visitMacroCall(node)
        else -> throw OspreyThrowable(
            OspreyClass.FatalOspreyClass,
            "An abstract syntax tree somewhere was passed an unhandled node %s".format(node::class.simpleName)
        )
    }

    fun visitStatements(node: Statements)
    fun visitExpressionStatement(node: ExpressionStatement)
    fun visitLookup(node: Lookup)
    fun visitVariableAssignment(node: VariableAssignment)
    fun visitListComprehension(node: ListComprehension)
    fun visitListExpression(node: ListExpression)
    fun visitTupleExpression(node: TupleExpression)
    fun visitDictionaryExpression(node: DictionaryExpression)
    fun visitLiteralString(node: LiteralString)
    fun visitLiteralFloat(node: LiteralFloat)
    fun visitLiteralInteger(node: LiteralInteger)
    fun visitConstant(node: Constant)
    fun visitGetReference(node: GetReference)
    fun visitDereference(node: Dereference)
    fun visitLambda(node: Lambda)
    fun visitBinaryExpression(node: BinaryExpression)
    fun visitCall(node: Call)
    fun visitMacroCall(node: MacroCall)
}