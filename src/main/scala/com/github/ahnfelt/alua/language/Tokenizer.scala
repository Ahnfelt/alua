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
            if(offset >= utf8.length) return tokens
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
                    addToken(Token(L.dotDot, from, offset))
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
        tokens
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

    val keyword             = 20
    val lower               = 21
    val upper               = 22
    val underscore          = 23
    val dot                 = 24
    val dotDot              = 25
    val comma               = 26
    val semicolon           = 27
    val colon               = 28
    val fatArrow            = 29

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


}
