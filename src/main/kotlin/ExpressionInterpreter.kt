import ast.AstBuilder
import ast.AstPrinter
import ast.PlainAstPrinter

fun main() {
    while (true) {
        osprey {
            print(">>> ")
            val input = readln()
            val astBuilder = AstBuilder(input, "input")
            val ast = astBuilder.build()
            AstPrinter(PlainAstPrinter()).visit(ast)
        }
    }
}