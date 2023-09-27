import OspreyClass.Companion.SyntaxErrorOspreyClass

/**
 * The type enum class for giving the lexeme additional data ie what the lexeme was written to be as parsed.
 * Any type that is not mentioned is an illegal character or expression and will raise a syntax error by the lexer
 */
enum class Type {
    IDENTIFIER, KEYWORD, FLOAT, INTEGER, STRING, RAW_STRING, END_LINE, START_OF_FILE, END_OF_FILE, INDENT, DEDENT, AT, PLUS, OPEN_PARENS, LESS_THAN, SLASH, EQUALS, CLOSE_BRACKETS, STAR, MINUS, OPEN_BRACKETS, MORE_THAN, CLOSE_PARENS, OPEN_CURLY, DOT, QUESTION_MARK, CLOSE_CURLY, COLON, SEMI_COLON, COMMA, PERCENT, DOUBLE_QUESTION, DOUBLE_EQUALS, DOUBLE_STAR, DOUBLE_DOT, ELVIS, BANG_EQUALS, ARROW_LEFT, ARROW_RIGHT, WIDE_ARROW_RIGHT, LESS_THAN_OR_EQUALS, MORE_THAN_OR_EQUALS, WHITESPACE;

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
}


/**
 * An array of keywords that the language uses, taken directly from the grammar file for the language and arranged in
 * alphabetical order
 */
val keywords = arrayOf(
    "and",
    "as",
    "break",
    "catch",
    "class",
    "continue",
    "deref",
    "else",
    "extends",
    "false",
    "finally",
    "for",
    "fun",
    "gen",
    "if",
    "import",
    "in",
    "instance",
    "is",
    "lambda",
    "not",
    "nothing",
    "or",
    "ref",
    "return",
    "throw",
    "true",
    "try",
    "val",
    "var",
    "while",
)

/*
private val WHITESPACE = Regex("[\r\t ]+")
private val IDENTIFIER = Regex("[a-zA-Z_$]+[a-zA-Z0-9_$]*")
private val INTEGER = Regex("\\d+(?:e[+-]?\\d+)?")
private val FLOAT = Regex("\\d+\\.\\d*(?:e[+-]?\\d+)?")
private val STRING = Regex("\"([^\n]*?)\"")
private val LONG_STRING = Regex("\"\"\"([^\n]*?)\"\"\"")
private val RAW_STRING = Regex("'([^\\n]*?)'")
private val LONG_RAW_STRING = Regex("'''([^\\n]*?)'''")
private val COMMENT = Regex("#[^\\n]*")
*/

/**
 * The lexer class which takes the [source] to parse from and the [fileName] of the source it's parsing from to generate
 * lexemes using the [scanNextLexeme] method
 */
open class Lexer(private val source: String, val fileName: String) {
    private var pos = 0
    private var line = 1
    private var linePos = 1

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

    private fun pushPastEndLine() {
        if (this.source[this.pos] == '\n' || this.source[this.pos] == '\r') {
            this.pos++
            if (this.source[this.pos] == '\n' || this.source[this.pos] == '\r') {
                this.pos++
            }
        }
        this.line++
        this.linePos = 1
    }

    /**
     * Scans the next lexeme of the source
     *
     * @throws OspreyThrowable
     * @return the next lexeme at the current lexing position of the source
     */
    open fun scanNextLexeme(): Lexeme {
        if (this.pos < this.source.length && this.source[this.pos] == '#') {
            this.pos++
            while (this.pos < this.source.length && !(this.source[this.pos] == '\n' || this.source[this.pos] == '\r')) {
                this.pos++
            }
        }

        val line: Int = this.line
        val linePos: Int = this.linePos

        if (pos < this.source.length && (this.source[pos] == ' ' || this.source[pos] == '\t')) {
            val whitespace = StringBuilder()
            while (pos < this.source.length && (this.source[pos] == ' ' || this.source[pos] == '\t')) {
                whitespace.append(this.source[pos])
                pos++
                this.linePos++
            }
            return Lexeme(Type.WHITESPACE, whitespace.toString(), this, line, linePos, this.line, this.linePos)
        }

        if (this.pos >= this.source.length) {
            return this.eof
        }

        var type: Type? = null

        if (this.source[this.pos] == '\n' || this.source[this.pos] == '\r') {
            this.pushPastEndLine()
            return Lexeme(Type.END_LINE, "", this, line, linePos, line, linePos)
        }

        if (this.pos + 1 < this.source.length) {
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

        if (this.source[this.pos].isLetter() || this.source[this.pos] == '_') {
            val identifierNameBuilder = StringBuilder()
            identifierNameBuilder.append(this.source[this.pos])
            this.pos++
            this.linePos++

            while (this.pos < this.source.length && (this.source[this.pos].isLetterOrDigit() || this.source[this.pos] == '_')) {
                identifierNameBuilder.append(this.source[this.pos])
                this.pos++
                this.linePos++
            }

            val identifierName = identifierNameBuilder.toString()

            return Lexeme(
                if (keywords.contains(identifierName)) {
                    Type.KEYWORD
                } else {
                    Type.IDENTIFIER
                }, identifierName, this, line, linePos, this.line, this.linePos
            )
        } else if (this.source[this.pos].isDigit()) {
            val num = StringBuilder()

            while (this.pos < this.source.length && this.source[this.pos].isDigit()) {
                num.append(this.source[this.pos])
                this.pos++
                this.linePos++
            }

            if (this.pos < this.source.length && this.source[this.pos] == '.') {
                while (this.pos < this.source.length && this.source[this.pos].isDigit()) {
                    num.append(this.source[this.pos])
                    this.pos++
                    this.linePos++
                }

                return Lexeme(Type.FLOAT, num.toString(), this, line, linePos, this.line, this.linePos)
            }

            return Lexeme(Type.INTEGER, num.toString(), this, line, linePos, this.line, this.linePos)

        } else if (this.source[this.pos] == '"' || this.source[this.pos] == '\'' || this.source[this.pos] == '`') {
            val stringEnd = this.source[this.pos]
            val stringValue = StringBuilder()
            this.pos++

            while (this.pos < this.source.length && this.source[this.pos] != stringEnd) {
                when (this.source[this.pos]) {
                    '\\' -> {
                        this.pos++
                        this.linePos++
                        if (this.pos < this.source.length && this.source[this.pos] == stringEnd) {
                            this.pos++
                            this.linePos++
                            stringValue.append(stringEnd)
                        } else {
                            throw OspreyThrowable(
                                SyntaxErrorOspreyClass, this.syntaxErrorAt(
                                    "Unexpected escape character %c".format(this.source[this.pos]), line, linePos
                                )
                            )
                        }
                    }

                    '\n', '\r' -> {
                        if (stringEnd == '`') {
                            stringValue.append(this.source[this.pos])
                            this.pos++
                            this.linePos = 1
                            this.pushPastEndLine()
                        } else {
                            throw OspreyThrowable(
                                SyntaxErrorOspreyClass,
                                this.syntaxErrorAt("Unclosed string broken outside of a grave string", line, linePos)
                            )
                        }
                    }

                    else -> {
                        stringValue.append(this.source[this.pos])
                        this.pos++
                        this.linePos++
                    }
                }
            }

            if (this.pos > this.source.length) {
                throw OspreyThrowable(
                    SyntaxErrorOspreyClass,
                    this.syntaxErrorAt("Unclosed string broken outside of a grave string", line, linePos)
                )
            }
            this.pos++

            return Lexeme(
                when (stringEnd) {
                    '`' -> Type.STRING
                    '"' -> Type.STRING
                    '\'' -> Type.RAW_STRING
                    else -> throw OspreyThrowable(
                        SyntaxErrorOspreyClass, "Impossible string return type error"
                    )
                }, stringValue.toString(), this, line, linePos, this.line, this.linePos
            )
        }

        val symbol = StringBuilder()
        while (pos < this.source.length && (this.source[pos] != '\n' || this.source[pos] != '\r')) {
            symbol.append(this.source[pos])
            pos++
            this.linePos++
        }

        throw OspreyThrowable(
            SyntaxErrorOspreyClass, "Unrecognised symbol \"%s\" at line %d, %d in file \"%s\":\n    %s\n    %s".format(
                symbol.toString().replace("\n", "<\\n>").replace("\r", "<\\r>").replace("\t", "<\\t>")
                    .replace("\b", "<\\b>"),
                line,
                linePos,
                this.fileName,
                this.lineTextOfLine(line),
                " ".repeat(linePos - 1) + "^".repeat(this.linePos - linePos)
            )
        )
    }

    /**
     * Parses all the indentation for the program, requires optimization
     */
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

                if (parensLevel > 0) {
                    continue
                }

                if (parsedLexemes.isNotEmpty() && !arrayOf(
                        Type.END_LINE, Type.DEDENT
                    ).contains(parsedLexemes[parsedLexemes.size - 1].type)
                ) {
                    parsedLexemes.add(endLine)
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
                            throw OspreyThrowable(
                                SyntaxErrorOspreyClass,
                                this.syntaxError("indent goes against established indent pattern", tail[0])
                            )
                        }

                        if (currentIndentLevel == indentLevel) {
                            continue
                        }

                        if (currentIndentLevel < indentLevel) {
                            while (currentIndentLevel < indentLevel) {
                                parsedLexemes.add(
                                    Lexeme(
                                        Type.DEDENT,
                                        "",
                                        this,
                                        tail[0].lineNo,
                                        tail[0].linePos,
                                        tail[0].lineNoExit,
                                        tail[0].linePosExit
                                    )
                                )
                                indentLevel--
                            }
                        } else if (currentIndentLevel == (indentLevel + 1)) {
                            indentLevel++
                            parsedLexemes.add(
                                Lexeme(
                                    Type.INDENT,
                                    "",
                                    this,
                                    tail[0].lineNo,
                                    tail[0].linePos,
                                    tail[0].lineNoExit,
                                    tail[0].linePosExit
                                )
                            )
                        } else {
                            throw OspreyThrowable(
                                SyntaxErrorOspreyClass,
                                this.syntaxError("double (or more) indent detected out of place", tail[0])
                            )
                        }
                    } else {
                        indentShape = tail[0].value
                        indentLevel++
                        parsedLexemes.add(
                            Lexeme(
                                Type.INDENT,
                                "",
                                this,
                                tail[0].lineNo,
                                tail[0].linePos,
                                tail[0].lineNoExit,
                                tail[0].linePosExit
                            )
                        )
                    }
                } else {
                    while (indentLevel != 0) {
                        parsedLexemes.add(
                            Lexeme(
                                Type.DEDENT,
                                "",
                                this,
                                tail[0].lineNo,
                                tail[0].linePos,
                                tail[0].lineNoExit,
                                tail[0].linePosExit
                            )
                        )
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

        if (parsedLexemes.isNotEmpty() && !arrayOf(
                Type.END_LINE, Type.DEDENT
            ).contains(parsedLexemes[parsedLexemes.size - 1].type)
        ) {
            parsedLexemes.add(
                Lexeme(
                    Type.END_LINE,
                    "",
                    this,
                    parsedLexemes[parsedLexemes.size - 1].lineNo,
                    parsedLexemes[parsedLexemes.size - 1].linePos,
                    parsedLexemes[parsedLexemes.size - 1].lineNoExit,
                    parsedLexemes[parsedLexemes.size - 1].linePosExit
                )
            )
        }

        while (indentLevel != 0) {
            parsedLexemes.add(
                Lexeme(
                    Type.DEDENT,
                    "",
                    this,
                    parsedLexemes[parsedLexemes.size - 1].lineNo,
                    parsedLexemes[parsedLexemes.size - 1].linePos,
                    parsedLexemes[parsedLexemes.size - 1].lineNoExit,
                    parsedLexemes[parsedLexemes.size - 1].linePosExit
                )
            )
            indentLevel--
        }

        parsedLexemes.add(eof)

        return parsedLexemes.toTypedArray()
    }

    fun syntaxError(message: String, lexeme: Lexeme): String =
        this.syntaxErrorAt(message, lexeme.lineNo, lexeme.linePos)

    private fun syntaxErrorAt(message: String, lineNo: Int, linePos: Int): String =
        "%s at line %d, %d in file \"%s\":\n    %s\n    %s".format(
            message, lineNo, linePos, this.fileName, this.lineTextOfLine(lineNo), " ".repeat(linePos - 1) + "^"
        )
}