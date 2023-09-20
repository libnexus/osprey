import OspreyClass.Companion.SyntaxErrorOspreyClass

/**
 * The type enum class for giving the lexeme additional data ie what the lexeme was written to be as parsed.
 * Any type that is not mentioned is an illegal character or expression and will raise a syntax error by the lexer
 */
enum class Type {
    IDENTIFIER, KEYWORD, FLOAT, INTEGER, STRING, RAW_STRING, END_LINE, START_OF_FILE, END_OF_FILE, INDENT, DEDENT, AT, PLUS, OPEN_PARENS, LESS_THAN,
    SLASH, EQUALS, CLOSE_BRACKETS, STAR, MINUS, OPEN_BRACKETS, MORE_THAN, CLOSE_PARENS, OPEN_CURLY, DOT, QUESTION_MARK,
    CLOSE_CURLY, COLON, SEMI_COLON, COMMA, PERCENT, DOUBLE_QUESTION, DOUBLE_EQUALS, DOUBLE_STAR, DOUBLE_DOT, ELVIS,
    BANG_EQUALS, ARROW_LEFT, ARROW_RIGHT, WIDE_ARROW_RIGHT, LESS_THAN_OR_EQUALS, MORE_THAN_OR_EQUALS,
    WHITESPACE;

    /**
     * The String representation of the type. This is used by the parser to create error messages that give more context
     *
     * @return the string representation of the lexeme's type
     */
    fun looksLike(): String = when (this) {
        IDENTIFIER -> "Identifier"
        KEYWORD -> "Keyword"
        FLOAT -> "Float"
        INTEGER -> "Integer"
        STRING -> "String"
        RAW_STRING -> "String"
        END_LINE -> "End of line"
        START_OF_FILE -> "Start of file"
        END_OF_FILE -> "End of file"
        INDENT -> "Indent"
        DEDENT -> "Dedent"

        AT -> "'@'"
        PLUS -> "'+'"
        OPEN_PARENS -> "'('"
        LESS_THAN -> "'<'"
        SLASH -> "'/'"
        EQUALS -> "'='"
        CLOSE_BRACKETS -> "']'"
        STAR -> "'*'"
        MINUS -> "'-'"
        OPEN_BRACKETS -> "'['"
        MORE_THAN -> "'>'"
        CLOSE_PARENS -> "')'"
        OPEN_CURLY -> "'{'"
        DOT -> "'.'"
        QUESTION_MARK -> "'?'"
        CLOSE_CURLY -> "'}'"
        COLON -> "':'"
        SEMI_COLON -> "';'"
        COMMA -> "','"
        PERCENT -> "'%'"

        DOUBLE_QUESTION -> "'??'"
        DOUBLE_EQUALS -> "'=='"
        DOUBLE_STAR -> "'**'"
        DOUBLE_DOT -> "'..'"
        ELVIS -> "'?:'"
        BANG_EQUALS -> "'!='"
        ARROW_LEFT -> "'<-'"
        ARROW_RIGHT -> "'->'"
        WIDE_ARROW_RIGHT -> "'=>'"
        MORE_THAN_OR_EQUALS -> "'>='"
        LESS_THAN_OR_EQUALS -> "'<='"
        else -> "#"
    }
}

/**
 * The lexeme class which gives context to a token parsed by the lexer which can then be used to track errors in the parser
 * or errors created by an operation
 *
 * @property type the type of lexeme it is e.g "{" would be OPEN_CURLY
 * @property value the value of the lexeme which stores information for lexemes with a value: IDENTIFIER, KEYWORD, NUMBER, STRING and RAW_STRING otherwise an empty string
 * @property lexer the lexer that the lexeme was parsed in, this is stored for access to the source code and file name of the lexeme
 * @property lineNo the line number that the lexeme started at in the lexeme's source
 * @property linePos the position on the line that the lexeme started at in the lexeme's source
 * @property lineNoExit the line number that the lexeme finished at in the lexeme's source
 * @property linePosExit the position on the line that the lexeme finished at in the lexeme's source
 */
class Lexeme(
    val type: Type,
    val value: String,
    val lexer: Lexer,
    val lineNo: Int,
    val linePos: Int,
    val lineNoExit: Int,
    val linePosExit: Int
) {

    /**
     * Helper method for the parser to determine if the lexeme is a specific keyword. No other lexeme requires type and value
     * checking the same way that a keyword does repeatedly if at all
     *
     * @return if the lexeme is a keyword equal to the given [keyword]
     */
    fun isKeyword(keyword: String): Boolean = this.type == Type.KEYWORD && this.value == keyword

    /**
     * Method for debugging to give a simple representation of the token and it's properties
     */
    override fun toString(): String {
        return if (this.value == "") {
            "Token(type=%s at %d:%d-%d:%d)".format(
                this.type.toString(), this.lineNo, this.linePos, this.lineNoExit, this.linePosExit
            )
        } else {
            "Token(type=%s value='%s' at %d:%d-%d:%d)".format(
                this.type.toString(), this.value, this.lineNo, this.linePos, this.lineNoExit, this.linePosExit
            )
        }
    }

    /**
     * Method for returning two lines of string, the first line being the line text that the lexeme first appears on,
     * the second line being carets to denote the span of the lexeme
     */
    fun lineText(): Pair<String, String> {
        val lineText = "    " + this.lexer.lineTextOfLine(this.lineNo)
        val underLine = if (this.lineNo == this.lineNoExit)
            "    " + " ".repeat(this.linePos - 1) + "^".repeat(this.linePosExit - this.linePos)
        else
            "    " + " ".repeat(this.linePos - 1) + "^".repeat(lineText.length - this.linePos) + " \\..."
        return Pair(lineText, underLine)
    }
}


/**
 * An array of keywords that the language uses, taken directly from the grammar file for the language and arranged in
 * alphabetical order
 */
val keywords = arrayOf(
    "and", "as", "break", "catch", "class", "continue", "deref", "else", "false", "finally", "for", "fun", "gen", "if",
    "import", "in", "instance", "is", "not", "nothing", "or", "ref", "return", "throw", "true", "try", "val", "var",
    "while",
)


private val WHITESPACE = Regex("[\r\t ]+")
private val IDENTIFIER = Regex("[a-zA-Z_$]+[a-zA-Z0-9_$]*")
private val INTEGER = Regex("\\d+(?:e[+-]?\\d+)?")
private val FLOAT = Regex("\\d+\\.\\d*(?:e[+-]?\\d+)?")
private val STRING = Regex("\"([^\n]*?)\"")
private val LONG_STRING = Regex("\"\"\"([^\n]*?)\"\"\"")
private val RAW_STRING = Regex("'([^\\n]*?)'")
private val LONG_RAW_STRING = Regex("'''([^\\n]*?)'''")
private val COMMENT = Regex("#[^\\n]*")


/**
 * The lexer class which takes the [source] to parse from and the [fileName] of the source it's parsing from to generate
 * lexemes using the [scanNextLexeme] method
 */
open class Lexer(val source: String, val fileName: String) {
    var pos: Int = 0
    var line: Int = 1
    var linePos: Int = 1
    private val eof: Lexeme by lazy {
        Lexeme(
            Type.END_OF_FILE, "End of File", this, this.line, this.linePos, this.line, this.linePos
        )
    }

    /**
     * Line text of line
     *
     * @param line the line number to get text of
     * @return the text for the given [line]
     */
    fun lineTextOfLine(line: Int): String = this.source.split("\n")[line - 1]

    /**
     * Scans the next lexeme of the source
     *
     * @throws OspreyThrowable
     * @return the next lexeme at the current lexing position of the source
     */
    open fun scanNextLexeme(): Lexeme {
        val comment = COMMENT.matchAt(this.source, this.pos)

        if (comment != null) {
            for (c: Char in comment.value) {
                if (c == '\n') {
                    this.line++
                    this.linePos = 1
                } else {
                    this.linePos++
                }
                this.pos++
            }
        }

        val line: Int = this.line
        val linePos: Int = this.linePos

        val whitespace = WHITESPACE.matchAt(this.source, this.pos)
        if (whitespace != null) {
            this.linePos += whitespace.value.length
            this.pos += whitespace.value.length
            return Lexeme(Type.WHITESPACE, whitespace.value, this, line, linePos, this.line, this.linePos)
        }

        if (this.pos >= this.source.length) {
            return this.eof
        }

        var type: Type? = null

        if (this.source[this.pos] == '\n') {
            this.line++
            this.linePos = 1
            this.pos++
            return Lexeme(Type.END_LINE, "End of Line", this, line, linePos, this.line, this.linePos)
        }

        val identifier = IDENTIFIER.matchAt(this.source, this.pos)
        if (identifier != null) {
            this.linePos += identifier.value.length
            this.pos += identifier.value.length

            return Lexeme(
                if (keywords.contains(identifier.value)) {
                    Type.KEYWORD
                } else {
                    Type.IDENTIFIER
                }, identifier.value, this, line, linePos, this.line, this.linePos
            )
        }

        val floatNumber = FLOAT.matchAt(this.source, this.pos)
        if (floatNumber != null) {
            this.linePos += floatNumber.value.length
            this.pos += floatNumber.value.length
            return Lexeme(Type.FLOAT, floatNumber.value, this, line, linePos, this.line, this.linePos)
        }

        val intNumber = INTEGER.matchAt(this.source, this.pos)
        if (intNumber != null) {
            this.linePos += intNumber.value.length
            this.pos += intNumber.value.length
            return Lexeme(Type.INTEGER, intNumber.value, this, line, linePos, this.line, this.linePos)
        }

        val string = STRING.matchAt(this.source, this.pos)
        if (string != null) {
            this.linePos += string.value.length
            this.pos += string.value.length
            return Lexeme(Type.STRING, string.groupValues[1], this, line, linePos, this.line, this.linePos)
        }

        val longString = LONG_STRING.matchAt(this.source, this.pos)
        if (longString != null) {
            this.linePos += longString.value.length
            this.pos += longString.value.length
            return Lexeme(Type.STRING, longString.groupValues[1], this, line, linePos, this.line, this.linePos)
        }

        val rawLongString = LONG_RAW_STRING.matchAt(this.source, this.pos)
        if (rawLongString != null) {
            this.linePos += rawLongString.value.length
            this.pos += rawLongString.value.length
            return Lexeme(Type.RAW_STRING, rawLongString.groupValues[1], this, line, linePos, this.line, this.linePos)
        }

        val rawString = RAW_STRING.matchAt(this.source, this.pos)
        if (rawString != null) {
            this.linePos += rawString.value.length
            this.pos += rawString.value.length
            return Lexeme(Type.RAW_STRING, rawString.groupValues[1], this, line, linePos, this.line, this.linePos)
        }

        if (this.pos + 2 <= this.source.length) {
            val sub2 = this.source.substring(this.pos, this.pos + 2)
            when (sub2) {
                "??" -> type = Type.DOUBLE_QUESTION
                "==" -> type = Type.DOUBLE_EQUALS
                "**" -> type = Type.DOUBLE_STAR
                ".." -> type = Type.DOUBLE_DOT
                "?:" -> type = Type.ELVIS
                "!=" -> type = Type.BANG_EQUALS
                "<-" -> type = Type.ARROW_LEFT
                "->" -> type = Type.ARROW_RIGHT
                "=>" -> type = Type.WIDE_ARROW_RIGHT
                ">=" -> type = Type.MORE_THAN_OR_EQUALS
                "<=" -> type = Type.LESS_THAN_OR_EQUALS
            }

            if (type != null) {
                this.pos += 2
                this.linePos += 2

                return Lexeme(type, sub2, this, line, linePos, this.line, this.linePos)
            }
        }

        when (this.source[this.pos]) {
            '@' -> type = Type.AT
            '+' -> type = Type.PLUS
            '(' -> type = Type.OPEN_PARENS
            '<' -> type = Type.LESS_THAN
            '/' -> type = Type.SLASH
            '=' -> type = Type.EQUALS
            ']' -> type = Type.CLOSE_BRACKETS
            '*' -> type = Type.STAR
            '-' -> type = Type.MINUS
            '[' -> type = Type.OPEN_BRACKETS
            '>' -> type = Type.MORE_THAN
            ')' -> type = Type.CLOSE_PARENS
            '{' -> type = Type.OPEN_CURLY
            '.' -> type = Type.DOT
            '?' -> type = Type.QUESTION_MARK
            '}' -> type = Type.CLOSE_CURLY
            ':' -> type = Type.COLON
            ';' -> type = Type.SEMI_COLON
            ',' -> type = Type.COMMA
            '%' -> type = Type.PERCENT
        }

        if (type != null) {
            this.pos++
            this.linePos++

            return Lexeme(type, this.source[this.pos - 1].toString(), this, line, linePos, this.line, this.linePos)
        }

        throw OspreyThrowable(
            SyntaxErrorOspreyClass, "Unrecognised symbol at line %d, %d in file \"%s\":\n    %s\n    %s".format(
                line,
                linePos,
                this.fileName,
                this.lineTextOfLine(line),
                " ".repeat(linePos - 1) + "^".repeat(this.linePos - linePos)
            )
        )
    }

    fun parseLexemes(): Array<Lexeme> {
        val lexemes = ArrayList<Lexeme>()
        var lexeme: Lexeme
        val eof: Lexeme
        while (true) {
            lexeme = this.scanNextLexeme()
            if (lexeme.type == Type.END_OF_FILE) {
                eof = lexeme
                break
            }
            lexemes.add(lexeme)
        }

        var indentShape = ""
        var indentLevel = 0
        var parensLevel = 0
        val parsedLexemes = ArrayList<Lexeme>()

        if (lexemes.isEmpty()) {
            return parsedLexemes.toTypedArray()
        }

        while (lexemes.isNotEmpty()) {
            val head = lexemes[0]
            val tail = lexemes.slice(1..<lexemes.size)
            if (head.type == Type.END_LINE) {
                val endLine = lexemes.removeAt(0)

                if (parsedLexemes.isNotEmpty() && !arrayOf(
                        Type.END_LINE,
                        Type.DEDENT
                    ).contains(parsedLexemes[parsedLexemes.size - 1].type)
                ) {
                    parsedLexemes.add(endLine)
                }

                if (parensLevel > 0) {
                    continue
                }

                if (lexemes.isEmpty()) {
                    break
                } 
                
                if (tail[0].type == Type.WHITESPACE) {
                    lexemes.removeAt(0)
                    if (indentShape.isNotEmpty()) {
                        var currentIndent = tail[0].value
                        var currentIndentLevel = 0
                        while (currentIndent.startsWith(indentShape)) {
                            currentIndentLevel++
                            currentIndent = currentIndent.substring(indentShape.length)
                        }
                        
                        if (currentIndent.isNotEmpty()) {
                            throw OspreyThrowable(SyntaxErrorOspreyClass, this.syntaxError("indent goes against established indent pattern", tail[0]))
                        }

                        if (currentIndentLevel == indentLevel) {
                            continue
                        }

                        if (currentIndentLevel < indentLevel) {
                            while (currentIndentLevel < indentLevel) {
                                parsedLexemes.add(Lexeme(Type.DEDENT, "", this, tail[0].lineNo, tail[0].linePos, tail[0].lineNoExit, tail[0].linePosExit))
                                indentLevel--
                            }
                        } else if (currentIndentLevel == (indentLevel + 1)) {
                            indentLevel++
                            parsedLexemes.add(Lexeme(Type.INDENT, "", this, tail[0].lineNo, tail[0].linePos, tail[0].lineNoExit, tail[0].linePosExit))
                        } else {
                            throw OspreyThrowable(SyntaxErrorOspreyClass, this.syntaxError("double (or more) indent detected out of place", tail[0]))
                        }
                    } else {
                        indentShape = tail[0].value
                        indentLevel++
                        parsedLexemes.add(Lexeme(Type.INDENT, "", this, tail[0].lineNo, tail[0].linePos, tail[0].lineNoExit, tail[0].linePosExit))
                    }
                } else {
                    while (indentLevel != 0) {
                        parsedLexemes.add(Lexeme(Type.DEDENT, "", this, tail[0].lineNo, tail[0].linePos, tail[0].lineNoExit, tail[0].linePosExit))
                        indentLevel--
                    }
                }
            } else {
                lexemes.removeAt(0)
                if (head.type != Type.WHITESPACE) {
                    parsedLexemes.add(head)
                    when (head.type) {
                        Type.OPEN_PARENS, Type.OPEN_BRACKETS, Type.OPEN_CURLY -> parensLevel++
                        Type.CLOSE_PARENS, Type.CLOSE_BRACKETS, Type.CLOSE_CURLY -> parensLevel--
                        else -> continue
                    }
                }
            }
        }

        if (parsedLexemes.isNotEmpty() && !arrayOf(Type.END_LINE, Type.DEDENT).contains(parsedLexemes[parsedLexemes.size - 1].type)) {
            parsedLexemes.add(Lexeme(Type.END_LINE, "", this, parsedLexemes[parsedLexemes.size - 1].lineNo, parsedLexemes[parsedLexemes.size - 1].linePos, parsedLexemes[parsedLexemes.size - 1].lineNoExit, parsedLexemes[parsedLexemes.size - 1].linePosExit))
        }

        while (indentLevel != 0) {
            parsedLexemes.add(Lexeme(Type.DEDENT, "", this, parsedLexemes[parsedLexemes.size - 1].lineNo, parsedLexemes[parsedLexemes.size - 1].linePos, parsedLexemes[parsedLexemes.size - 1].lineNoExit, parsedLexemes[parsedLexemes.size - 1].linePosExit))
            indentLevel--
        }

        parsedLexemes.add(eof)

        return parsedLexemes.toTypedArray()
    }

    fun syntaxError(message: String, lexeme: Lexeme): String = "%s at line %d, %d in file \"%s\":\n    %s\n    %s".format(
        message,
        lexeme.lineNo,
        lexeme.linePos,
        this.fileName,
        this.lineTextOfLine(lexeme.lineNo),
        " ".repeat(lexeme.linePos - 1) + "^"
    )
}