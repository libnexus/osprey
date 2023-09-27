package ast

import java.util.Stack

class AstPrinterPrinter {
    private var indent = 0
    
    fun print(string: String) {
        println("    ".repeat(indent) + string)
    }

    fun printIndented(title: String, block: () -> Unit) {
        this.print("$title {")
        this.indent++
        block()
        this.indent--
        this.print("}")
    }
}

class AstPrinter : AstVisitor {
    private val printer = AstPrinterPrinter()

    override fun visitStatements(node: Statements) {
        for (statement in node.statements) {
            this.visit(statement)
        }
    }

    override fun visitExpressionStatement(node: ExpressionStatement) {
        this.visit(node.expression)
    }

    override fun visitLookup(node: Lookup) {
        this.printer.print("Lookup (${node.name})")
    }

    override fun visitVariableAssignment(node: VariableAssignment) {
        this.printer.printIndented("Assignment") {
            this.printer.print("Name (${node.name})")
            this.printer.printIndented("Value") { this.visit(node.value) }
        }
    }

    override fun visitListComprehension(node: ListComprehension) {
        this.printer.printIndented("List Comprehension") {
            this.printer.printIndented("Iterated Expression") { this.visit(node.expression) }
            this.printer.printIndented("Iterator") { this.visit(node.iterator) }
            if (node.discriminator != null) {
                this.printer.printIndented("Discriminator") { this.visit(node.discriminator) }
            }
        }
    }

    override fun visitListExpression(node: ListExpression) {
        if (node.expressions.isEmpty()) {
            this.printer.print("Empty List")
            return
        }
        
        this.printer.printIndented("List") {
            for (expression in node.expressions) {
                this.printer.printIndented("-") { this.visit(expression) }
            }
        }
    }

    override fun visitTupleExpression(node: TupleExpression) {
        if (node.expressions.isEmpty()) {
            this.printer.print("Empty Tuple")
            return
        }
        
        this.printer.printIndented("Tuple") {
            for (expression in node.expressions) {
                this.printer.printIndented("-") { this.visit(expression) }
            }
        }
    }

    override fun visitDictionaryExpression(node: DictionaryExpression) {
        if (node.expressions.isEmpty()) {
            this.printer.print("Empty Dictionary")
            return
        }
        
        this.printer.printIndented("Dictionary") {
            for (i in 0..(node.expressions.size / 2)) {
                this.printer.printIndented("Key") { this.visit(node.expressions[2 * i]) }
                this.printer.printIndented("Value") { this.visit(node.expressions[2 * i + 1]) }
            }
        }
    }

    override fun visitLiteralString(node: LiteralString) {
        this.printer.print("String '${node.string}'")
    }

    override fun visitLiteralFloat(node: LiteralFloat) {
        this.printer.print("Float (${node.float})")
    }

    override fun visitLiteralInteger(node: LiteralInteger) {
        this.printer.print("Integer (${node.int})")
    }

    override fun visitConstant(node: Constant) {
        this.printer.print("Constant of type(${node.constant.type.name}), UID(${node.constant.getUID()})")
    }

    override fun visitGetReference(node: GetReference) {
        this.printer.print("Get Reference of (${node.name})")
    }

    override fun visitDereference(node: Dereference) {
        this.printer.printIndented("Dereference") { this.visit(node.expression) }
    }

    override fun visitLambda(node: Lambda) {
        this.printer.printIndented("Lambda") { this.visit(node.expression) }
    }

    override fun visitBinaryExpression(node: BinaryExpression) {
        
        this.printer.printIndented("Binary (${node.operator.name.lowercase().replaceFirstChar { it.uppercaseChar() }})") {
            this.printer.printIndented("Left") { this.visit(node.left) }
            this.printer.printIndented("Right") { this.visit(node.right) }
        }
        
    }

    private fun visitSomeCall(node: SomeCall) {
        this.printer.printIndented(node::class.simpleName.toString()) {
            this.printer.printIndented("Subject") { this.visit(node.subject) }

            if (node.args != null) {
                if (node.args.isEmpty()) {
                    this.printer.print("No Arguments")
                } else {
                    this.printer.printIndented("Arguments") {
                        for (arg in node.args) {
                            this.printer.printIndented("Argument") { this.visit(arg) }
                        }
                    }
                }
            } else {
                this.printer.print("No Arguments")
            }

            if (node.keywords != null) {
                if (node.keywords.isEmpty()) {
                    this.printer.print("No Keywords")
                } else {
                    this.printer.printIndented("Keywords") {
                        for ((name, value) in node.keywords.entries) {
                            this.printer.printIndented("Keyword $name") {
                                this.printer.printIndented("Value") { this.visit(value) }
                            }
                        }
                    }
                }
            } else {
                this.printer.print("No Keywords")
            }
        }
    }

    override fun visitCall(node: Call) {
        this.visitSomeCall(node)
    }

    override fun visitMacroCall(node: MacroCall) {
        this.visitSomeCall(node)
    }

    override fun visitGetAttribute(node: GetAttribute) {
        this.printer.printIndented("GetAttribute") {
            this.printer.print("Name (${node.name})")
            this.printer.printIndented("Subject") { this.visit(node.subject) }
        }
    }
}