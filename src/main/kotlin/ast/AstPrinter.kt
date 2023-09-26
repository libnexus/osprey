package ast

import java.util.Stack

class AstPrinterPrinter {
    var indent = 0
    var savedIndentStates = Stack<Int>()
    fun print(string: String) {
        println("    ".repeat(indent) + string)
    }

    fun title(string: String) {
        this.print(string)
        this.indent++
    }

    fun newSubTitle(string: String) {
        this.indent--
        this.print(string)
        this.indent++
    }

    fun group() = savedIndentStates.add(this.indent)
    fun release() {
        this.indent = savedIndentStates.pop()
    }

    fun printIndented(title: String, block: () -> Unit) {
        this.print(title)
        this.indent++
        block()
        this.indent--
    }
}

class AstPrinter : AstVisitor {
    private val printer = AstPrinterPrinter()

    override fun visit(node: Node) {
        super.visit(node)
    }

    override fun visitStatements(node: Statements) {
        for (statement in node.statements) {
            this.visit(statement)
        }
    }

    override fun visitExpressionStatement(node: ExpressionStatement) {
        this.visit(node.expression)
    }

    override fun visitLookup(node: Lookup) {
        this.printer.print("Lookup: %s".format(node.name))
    }

    override fun visitVariableAssignment(node: VariableAssignment) {
        this.printer.group()
        this.printer.title("Assignment:")
        this.printer.title("Name:")
        this.printer.print("%s".format(node.name))
        this.printer.newSubTitle("Value:")
        this.visit(node.value)
        this.printer.release()
    }

    override fun visitListComprehension(node: ListComprehension) {
        this.printer.group()
        this.printer.title("List Comprehension:")
        this.printer.title("Iterated Expression:")
        this.visit(node.expression)
        this.printer.newSubTitle("Iterator:")
        this.visit(node.iterator)
        if (node.discriminator != null) {
            this.printer.newSubTitle("Discriminator:")
            this.visit(node.discriminator)
        }
        this.printer.release()
    }

    override fun visitListExpression(node: ListExpression) {
        if (node.expressions.isEmpty()) {
            this.printer.print("Empty List")
            return
        }
        this.printer.group()
        this.printer.title("List:")
        for (expression in node.expressions) {
            this.printer.title("-")
            this.visit(expression)
            this.printer.indent--
        }
        this.printer.release()
    }

    override fun visitTupleExpression(node: TupleExpression) {
        if (node.expressions.isEmpty()) {
            this.printer.print("Empty Tuple")
            return
        }
        this.printer.group()
        this.printer.title("Tuple:")
        for (expression in node.expressions) {
            this.printer.title("-")
            this.visit(expression)
            this.printer.indent--
        }
        this.printer.release()
    }

    override fun visitDictionaryExpression(node: DictionaryExpression) {
        if (node.expressions.isEmpty()) {
            this.printer.print("Empty Dictionary")
            return
        }
        this.printer.group()
        this.printer.title("Dictionary:")
        for (i in 0..(node.expressions.size / 2)) {
            this.printer.title("Key")
            this.visit(node.expressions[2 * i])
            this.printer.newSubTitle("Value:")
            this.visit(node.expressions[2 * i + 1])
            this.printer.indent--
        }
        this.printer.release()
    }

    override fun visitLiteralString(node: LiteralString) {
        this.printer.print("String: '%s'".format(node.string))
    }

    override fun visitLiteralFloat(node: LiteralFloat) {
        this.printer.print("Float: %s".format(node.float))
    }

    override fun visitLiteralInteger(node: LiteralInteger) {
        this.printer.print("Integer: %s".format(node.int))
    }

    override fun visitConstant(node: Constant) {
        this.printer.print("Constant of type: %s, UUID: %s".format(node.constant.type.name, node.constant.getUID()))
    }

    override fun visitGetReference(node: GetReference) {
        this.printer.print("Get Reference of: %s".format(node.name))
    }

    override fun visitDereference(node: Dereference) {
        this.printer.title("Dereference:")
        this.visit(node.expression)
        this.printer.indent--
    }

    override fun visitLambda(node: Lambda) {
        this.printer.title("Lambda:")
        this.visit(node.expression)
        this.printer.indent--
    }

    override fun visitBinaryExpression(node: BinaryExpression) {
        this.printer.group()
        this.printer.title("Binary (%s):".format(node.operator.name))
        this.printer.title("Left:")
        this.visit(node.left)
        this.printer.newSubTitle("Right:")
        this.visit(node.right)
        this.printer.release()
    }

    private fun visitSomeCall(node: SomeCall) {
        this.printer.printIndented("%s:".format(node::class.simpleName)) {
            this.printer.printIndented("Subject:") { this.visit(node.subject) }

            if (node.args != null) {
                if (node.args.isEmpty()) {
                    this.printer.print("No Arguments")
                } else {
                    this.printer.printIndented("Arguments:") {
                        for (arg in node.args) {
                            this.printer.printIndented("Argument:") { this.visit(arg) }
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
                    this.printer.printIndented("Keywords:") {
                        for ((name, value) in node.keywords.entries) {
                            this.printer.printIndented("Keyword %s:".format(name)) {
                                this.printer.printIndented("Value:") { this.visit(value) }
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
        this.printer.printIndented("GetAttribute:") {
            this.printer.print("Name: %s".format(node.name))
            this.printer.printIndented("Subject:") { this.visit(node.subject) }
        }
    }
}