package ast

interface AstPrinterManager {
    fun print(string: String)
    fun printIndented(title: String, block: () -> Unit)
}

class PlainAstPrinter : AstPrinterManager {
    private var indent = 0

    override fun print(string: String) {
        println("    ".repeat(indent) + string)
    }

    override fun printIndented(title: String, block: () -> Unit) {
        this.print("$title {")
        this.indent++
        block()
        this.indent--
        this.print("}")
    }
}

class HtmlAstPrinter : AstPrinterManager {
    private val generatedHtml = StringBuilder()
    private var indent = 3

    init {
        generatedHtml.append("""
            <style>
            /*
                Created by W3SCHOOLS @ https://www.w3schools.com/howto/howto_js_treeview.asp
            */
            
            body {
                font-family: "Liberation Mono",serif;
            }
            
            ul, #myUL {
              list-style-type: none;
            }

            #myUL {
              margin: 0;
              padding: 0;
            }

            .caret {
              cursor: pointer;
              user-select: none; /* Prevent text selection */
            }

            .caret::before {
              content: "\25B6";
              color: black;
              display: inline-block;
              margin-right: 6px;
            }

            .caret-down::before {
              transform: rotate(90deg);
            }

            .nested {
              display: none;
            }

            .active {
              display: block;
            }
            
            </style>
            <ul id="myUL">
                <li><span class="caret">AST Generation</span>
                    <ul class="nested">
        """.trimIndent())
    }
    override fun print(string: String) {
        generatedHtml.append("\n" + "    ".repeat(indent) + "<li>$string</li>")
    }

    override fun printIndented(title: String, block: () -> Unit) {
        generatedHtml.append("\n" + "    ".repeat(indent) + "<li><span class=\"caret\">$title</span>")
        indent++
        generatedHtml.append("\n" + "    ".repeat(indent) + "<ul class=\"nested\">")
        indent++
        block()
        indent--
        generatedHtml.append("\n" + "    ".repeat(indent) + "</ul>")
        indent--
        generatedHtml.append("\n" + "    ".repeat(indent) + "</li>")
    }

    fun generateHtml() : String = "$generatedHtml\n        </ul>\n    </li>\n</ul>" + """
            <script>
            var toggler = document.getElementsByClassName("caret");
            var i;

            for (i = 0; i < toggler.length; i++) {
                toggler[i].addEventListener("click", function() {
                    this.parentElement.querySelector(".nested").classList.toggle("active");
                    this.classList.toggle("caret-down");
                });
              toggler[i].parentElement.querySelector(".nested").classList.toggle("active");
              toggler[i].classList.toggle("caret-down");
            }
            </script>""".trimIndent()
}

class AstPrinter(private val printer: AstPrinterManager) : AstVisitor {

    override fun visitStatements(node: Statements) {
        node.statements.map { this.visit(it) }
    }

    override fun visitExpressionStatement(node: ExpressionStatement) {
        this.visit(node.expression)
    }

    override fun visitLookup(node: Lookup) {
        this.printer.print("Lookup (${node.name})")
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
            node.expressions.map { this.printer.printIndented("-") { this.visit(it) } }
        }
    }

    override fun visitTupleExpression(node: TupleExpression) {
        if (node.expressions.isEmpty()) {
            this.printer.print("Empty Tuple")
            return
        }

        this.printer.printIndented("Tuple") {
            node.expressions.map { this.printer.printIndented("-") { this.visit(it) } }
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

        this.printer.printIndented("Binary (${
            node.operator.name.lowercase().replaceFirstChar { it.uppercaseChar() }
        })") {
            this.printer.printIndented("Left") { this.visit(node.left) }
            this.printer.printIndented("Right") { this.visit(node.right) }
        }

    }

    override fun visitCall(node: Call) {
        this.printer.printIndented(node::class.simpleName.toString()) {
            this.printer.printIndented("Subject") { this.visit(node.subject) }

            this.printIndentedExpressionCollection("Argument", node.args())

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

    private fun printIndentedExpressionCollection(title: String, collection: Array<Expression>?) {
        if (collection != null) {
            if (collection.isEmpty()) {
                this.printer.print("No $title")
            } else {
                this.printer.printIndented("${title}s") {
                    for (arg in collection) {
                        this.printer.printIndented(title) { this.visit(arg) }
                    }
                }
            }
        } else {
            this.printer.print("No $title")
        }
    }

    override fun visitMacroCall(node: MacroCall) {
        this.printer.printIndented("Macro Call") {
            this.printer.printIndented("Subject") { this.visit(node.subject) }
            this.printIndentedExpressionCollection("Macro Argument", node.args)
        }
    }

    override fun visitGetAttribute(node: GetAttribute) {
        this.printer.printIndented("GetAttribute") {
            this.printer.print("Name (${node.name})")
            this.printer.printIndented("Subject") { this.visit(node.subject) }
        }
    }

    private fun printAssignment(assignment: SomeAssignment) {
        this.printer.printIndented("Assignment") {
            when (assignment) {
                is NameAssignment -> {
                    this.printer.print("Assign name (${assignment.name})")
                }

                is AttributeAssignment -> {
                    this.printer.printIndented("Assign attribute") {
                        this.printer.printIndented("Subject") { this.visit(assignment.subject) }
                        this.printer.print("Name (${assignment.name})")
                    }
                }

                is MacroAssignment -> {
                    this.printer.printIndented("Assign macro") {
                        this.printer.printIndented("Subject") { this.visit(assignment.subject) }
                        this.printer.printIndented("Arguments") {
                            this.printIndentedExpressionCollection(
                                "Macro Assignment Argument", assignment.macroArgs
                            )
                        }
                    }
                }
            }
        }
    }

    override fun visitAssignmentExpression(node: AssignmentExpression) {
        this.printAssignment(node.assignment)
        this.printer.printIndented("Value") { this.visit(node.expression) }
    }

    override fun visitAssignmentsExpression(node: AssignmentsExpression) {
        this.printer.printIndented("Assign Multiple") {
            this.printer.printIndented("Assignment targets") {
                node.assignments.map { this.printAssignment(it) }
            }
            this.printer.printIndented("Unpack-able? Value") { this.visit(node.expression) }
        }
    }

    override fun visitExpressionUnit(node: ExpressionUnit) {
        this.printer.printIndented("Expression Unit") {
            this.printIndentedExpressionCollection("Expression", node.expressions)
        }
    }
}