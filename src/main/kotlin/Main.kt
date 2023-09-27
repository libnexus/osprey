import ast.AstBuilder
import ast.AstPrinter
import ast.HtmlAstPrinter
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
        val file = File("main.osp")
        val astBuilder = AstBuilder(file.readText(), "main.osp")
        val ast = astBuilder.build()

        val out = File("main.osp.html")
        val htmlAstPrinter = HtmlAstPrinter()
        AstPrinter(htmlAstPrinter).visit(ast)
        out.writeText(htmlAstPrinter.generateHtml())
        println("AST generated to ${out.absolutePath}")
    }
}