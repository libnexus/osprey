import ast.AstBuilder
import ast.AstPrinter
import java.io.File

fun osprey(block: () -> Unit) {
    try {
        block()
    } catch (osp: OspreyThrowable) {
        printErrorStack()
    }
}

fun main(args: Array<String>) {
    osprey {
        val file = File("main.osp").readText()
        val astBuilder = AstBuilder(file, "main.osp")
        val ast = astBuilder.build()
        AstPrinter().visit(ast)
    }
}