import Ast.AstBuilder
import Ast.AstPrinter

fun main() {
    while (true) {
        osprey {
            print(">>> ")
            val input = readln()
            val astBuilder = AstBuilder(input, "input")
            val ast = astBuilder.build()
            AstPrinter().visit(ast)
        }
    }
}