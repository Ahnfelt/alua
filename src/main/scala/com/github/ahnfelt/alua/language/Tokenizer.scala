package com.github.ahnfelt.alua.language

import com.github.ahnfelt.alua.language.Tokenizer.Token
import com.github.ahnfelt.alua.language.{Tokenizer => L}


class Tokenizer(utf8 : Array[Byte]) {

    private var offset = 0
    private var tokens = new Array[Long](8 * 1024)
    private var nextToken = 8

    private def addToken(token : Token) : Unit = {
        if(nextToken + 8 >= tokens.length) {
            val newTokens = new Array[Long](tokens.length * 2)
            tokens.copyToArray(newTokens)
            tokens = newTokens
        }
        tokens(nextToken) = token.bits
        nextToken += 1
    }

    private[Tokenizer] def tokenize() : Array[Long] = {
        tokenizeInner()
        var i = 8
        while(new Token(tokens(i)).kind != L.endOfFile) {
            if(
                new Token(tokens(i)).kind == L.lower &&
                new Token(tokens(i - 1)).kind != L.dot &&
                new Token(tokens(i + 1)).kind != L.roundImmediate &&
                new Token(tokens(i + 1)).kind != L.squareImmediate &&
                new Token(tokens(i + 1)).kind != L.curlyImmediate
            ) {
                replaceWithKeyword(i)
            }
            i += 1
        }
        tokens
    }

    private def tokenizeInner() : Unit = {
        while(true) {
            while(offset < utf8.length && {
                val c = utf8(offset)
                c == ' ' || c == '\t' || c == '\r' || c == '\n'
            }) offset += 1
            if(offset + 1 < utf8.length && utf8(offset) == '/' && utf8(offset + 1) == '*') {
                offset += 2
                while(offset + 1 < utf8.length && {
                    utf8(offset) != '*' || utf8(offset + 1) != '/'
                }) offset += 1
                offset = Math.min(offset + 2, utf8.length)
                while(offset < utf8.length && {
                    val c = utf8(offset)
                    c == ' ' || c == '\t' || c == '\r' || c == '\n'
                }) offset += 1
            }
            if(offset >= utf8.length) return
            val from = offset
            val c = utf8(offset)
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                val tokenKind = if(c >= 'a' && c <= 'z') L.lower else L.upper
                while(offset < utf8.length && {
                    val c = utf8(offset)
                    (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                }) offset += 1
                addToken(Token(tokenKind, from, offset))
            } else if(c >= '0' && c <= '9') {
                while(offset < utf8.length && {
                    val c = utf8(offset)
                    c >= '0' && c <= '9'
                }) offset += 1
                if(offset + 1 < utf8.length && utf8(offset) == '.' && {
                    val c = utf8(offset + 1)
                    c >= '0' && c <= '9'
                }) {
                    offset += 1
                    while(offset < utf8.length && {
                        val c = utf8(offset)
                        c >= '0' && c <= '9'
                    }) offset += 1
                    addToken(Token(L.float, from, offset))
                } else {
                    addToken(Token(L.integer, from, offset))
                }
            } else if(c == '"') {
                var ignoreNext = true
                while(offset < utf8.length && {
                    val c = utf8(offset)
                    val proceed = ignoreNext || c != '"'
                    ignoreNext = !ignoreNext && c == '\\'
                    proceed
                }) offset += 1
                offset += 1
                addToken(Token(L.string, from, offset))
            } else if(c == '+') {
                offset += 1
                if(offset < utf8.length && utf8(offset) == '+') {
                    offset += 1
                    addToken(Token(L.append, from, offset))
                } else {
                    addToken(Token(L.plus, from, offset))
                }
            } else if(c == '-') {
                offset += 1
                addToken(Token(L.minus, from, offset))
            } else if(c == '*') {
                offset += 1
                addToken(Token(L.multiply, from, offset))
            } else if(c == '/') {
                offset += 1
                addToken(Token(L.divide, from, offset))
            } else if(c == '^') {
                offset += 1
                addToken(Token(L.power, from, offset))
            } else if(c == '=') {
                offset += 1
                if(offset < utf8.length && utf8(offset) == '=') {
                    offset += 1
                    addToken(Token(L.equalEqual, from, offset))
                } else if(offset < utf8.length && utf8(offset) == '>') {
                    offset += 1
                    addToken(Token(L.fatArrow, from, offset))
                } else {
                    addToken(Token(L.equal, from, offset))
                }
            } else if(c == '<') {
                offset += 1
                if(offset < utf8.length && utf8(offset) == '=') {
                    offset += 1
                    addToken(Token(L.lessEqual, from, offset))
                } else if(offset < utf8.length && utf8(offset) == '>') {
                    offset += 1
                    addToken(Token(L.notEqual, from, offset))
                } else {
                    addToken(Token(L.less, from, offset))
                }
            } else if(c == '>') {
                offset += 1
                if(offset < utf8.length && utf8(offset) == '=') {
                    offset += 1
                    addToken(Token(L.moreEqual, from, offset))
                } else {
                    addToken(Token(L.more, from, offset))
                }
            } else if(c == '(') {
                val tokenKind =
                    if(new Token(tokens(nextToken - 1)).until == offset) L.roundImmediate else L.roundBegin
                offset += 1
                addToken(Token(tokenKind, from, offset))
            } else if(c == '[') {
                val tokenKind =
                    if(new Token(tokens(nextToken - 1)).until == offset) L.squareImmediate else L.squareBegin
                offset += 1
                addToken(Token(tokenKind, from, offset))
            } else if(c == '{') {
                val tokenKind =
                    if(new Token(tokens(nextToken - 1)).until == offset) L.curlyImmediate else L.curlyBegin
                offset += 1
                addToken(Token(tokenKind, from, offset))
            } else if(c == ')') {
                offset += 1
                addToken(Token(L.roundEnd, from, offset))
            } else if(c == ']') {
                offset += 1
                addToken(Token(L.squareEnd, from, offset))
            } else if(c == '}') {
                offset += 1
                addToken(Token(L.curlyEnd, from, offset))
            } else if(c == '_') {
                offset += 1
                addToken(Token(L.underscore, from, offset))
            } else if(c == '.') {
                offset += 1
                if(offset < utf8.length && utf8(offset) == '.') {
                    offset += 1
                    if(offset < utf8.length && utf8(offset) == '.') {
                        offset += 1
                        addToken(Token(L.dotDotDot, from, offset))
                    } else {
                        addToken(Token(L.unexpected, from, offset))
                    }

                } else {
                    addToken(Token(L.dot, from, offset))
                }
            } else if(c == ',') {
                offset += 1
                addToken(Token(L.comma, from, offset))
            } else if(c == ':') {
                offset += 1
                addToken(Token(L.colon, from, offset))
            } else if(c == ';') {
                offset += 1
                addToken(Token(L.semicolon, from, offset))
            } else {
                while(offset < utf8.length && {
                    val c = utf8(offset)
                    !(c == ' ' || c == '\t' || c == '\r' || c == '\n')
                }) offset += 1
                addToken(Token(L.unexpected, from, offset))
            }
        }
    }

    val keywords = List(
        "do" -> L.keywordDo,
        "end" -> L.keywordEnd,
        "function" -> L.keywordFunction,
        "import" -> L.keywordImport,
        "include" -> L.keywordInclude,
        "instance" -> L.keywordInstance,
        "let" -> L.keywordLet,
        "local" -> L.keywordLocal,
        "method" -> L.keywordMethod,
        "mutable" -> L.keywordMutable,
        "new" -> L.keywordNew,
        "private" -> L.keywordPrivate,
        "public" -> L.keywordPublic,
        "resolve" -> L.keywordResolve,
        "type" -> L.keywordType,
        "unboxed" -> L.keywordUnboxed,
        "using" -> L.keywordUsing,
        "variant" -> L.keywordVariant,
        "while" -> L.keywordWhile,
        "if" -> L.keywordIf,
        "then" -> L.keywordThen,
        "elseif" -> L.keywordElseif,
        "else" -> L.keywordElse,
        "match" -> L.keywordMatch,
        "case" -> L.keywordCase,
        "as" -> L.keywordAs,
        "is" -> L.keywordIs,
    ).map { case (word, number) =>
        word.getBytes("UTF-8") -> number
    }.toArray

    def replaceWithKeyword(tokenIndex : Int) : Unit = {
        val token = new Token(tokens(tokenIndex))
        var j = 0
        while(j < keywords.length) {
            val (word, number) = keywords(j)
            if(
                word.size == token.length && {
                    var result = true
                    var i = 0
                    while(i < word.size) {
                        result &&= utf8(token.offset + i) == word(i)
                        i += 1
                    }
                    result
                }
            ) {
                tokens(tokenIndex) = Token(number, token.offset, token.offset + token.length).bits
                return
            }
            j += 1
        }
    }

}


object Tokenizer {


    def tokenize(utf8 : Array[Byte]) : Array[Long] = new Tokenizer(utf8).tokenize()


    final class Token(val bits : Long) extends AnyVal {
        // A token is represented as 8 bytes: k oooo lll - kind, offset and length.
        def kind : Int = (bits >> (8 * 7)).toInt
        def offset : Int = ((bits & 0x00ffffffffffffffL) >> (8 * 3)).toInt
        def length : Int = (bits & 0x0000000000ffffffL).toInt
        def until : Int = offset + length
        def text(utf8 : Array[Byte]) = new String(utf8, offset, length, "UTF-8")
        def findLineAndColumn(utf8 : Array[Byte]) : (Int, Int) = {
            var current = 0
            var line = 1
            var column = 1
            while(current < offset) {
                val c = utf8(current)
                if(c == '\n') {
                    line += 1
                    column = 1
                } else if((c & 0xC0).toByte != 0x80b) {
                    column += 1
                }
                current += 1
            }
            (line, column)
        }

        override def toString = {
            +kind + ":" + offset + ":" + length
        }
    }

    object Token {
        def apply(kind : Int, offset : Int, until : Int) = new Token(
            (kind.toLong << (8 * 7)) |
            (offset.toLong << (8 * 3)) |
            (until - offset).toLong
        )
    }


    val endOfFile           = 0
    val unexpected          = 1

    val roundImmediate      = 10
    val roundBegin          = 11
    val roundEnd            = 12
    val squareImmediate     = 13
    val squareBegin         = 14
    val squareEnd           = 15
    val curlyImmediate      = 16
    val curlyBegin          = 17
    val curlyEnd            = 18

    val lower               = 20
    val upper               = 21
    val underscore          = 22
    val dot                 = 23
    val dotDotDot           = 24
    val comma               = 25
    val semicolon           = 26
    val colon               = 27
    val fatArrow            = 28

    val string              = 30
    val integer             = 34
    val float               = 37

    val append              = 40
    val plus                = 41
    val minus               = 42
    val multiply            = 43
    val divide              = 44
    val power               = 46

    val equal               = 50
    val equalEqual          = 51
    val notEqual            = 52
    val less                = 53
    val lessEqual           = 54
    val more                = 55
    val moreEqual           = 56

    val keywordDo           = 70
    val keywordEnd          = 71
    val keywordFunction     = 72
    val keywordImport       = 73
    val keywordInclude      = 74
    val keywordInstance     = 75
    val keywordLet          = 76
    val keywordLocal        = 77
    val keywordMethod       = 78
    val keywordMutable      = 79
    val keywordNew          = 80
    val keywordPrivate      = 81
    val keywordPublic       = 82
    val keywordResolve      = 83
    val keywordType         = 84
    val keywordUnboxed      = 85
    val keywordUsing        = 86
    val keywordVariant      = 87
    val keywordWhile        = 88
    val keywordIf           = 89
    val keywordThen         = 90
    val keywordElseif       = 91
    val keywordElse         = 92
    val keywordMatch        = 93
    val keywordCase         = 94
    val keywordAs           = 95
    val keywordIs           = 96

}
