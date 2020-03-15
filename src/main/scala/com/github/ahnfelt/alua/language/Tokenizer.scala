package com.github.ahnfelt.alua.language

import com.github.ahnfelt.alua.language.Tokenizer._


class Tokenizer(utf8 : Array[Byte]) {
    var offset : Int = 0
    var lastToken : Token = Token(endOfFile, 0, 0)

}


object Tokenizer {


    final class Token(val bits : Long) extends AnyVal {
        // A token is represented as 8 bytes: k oooo lll - kind, offset and length.
        def kind : Int = (bits >> (8 * 7)).toInt
        def offset : Int = ((bits & 0x00ffffffffffffffL) >> (8 * 3)).toInt
        def length : Int = (bits & 0x0000000000ffffffL).toInt
        def until : Int = offset + length
    }

    object Token {
        def apply(kind : Int, offset : Int, until : Int) = new Token(
            (kind.toLong << (8 * 7)) |
            (offset.toLong << (8 * 3)) |
            (until - offset).toLong
        )
    }


    def offsetToLineAndColumn(utf8 : Array[Byte], targetOffset : Int) : (Int, Int) = {
        var offset = 0
        var line = 1
        var column = 1
        while(offset < targetOffset) {
            val c = utf8(offset)
            if(c == '\n') {
                line += 1
                column = 1
            } else if((c & 0xC0).toByte != 0x80b) {
                column += 1
            }
            offset += 1
        }
        (line, column)
    }


    val endOfFile           = 1

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
    val upperUnderscore     = 23
    val underscore          = 24
    val dot                 = 25
    val comma               = 26
    val semicolon           = 27
    val colon               = 28
    val fatArrow            = 29

    val string              = 30
    val integer             = 34
    val float               = 37

    val append              = 40
    val add                 = 41
    val minus               = 42
    val multiply            = 43
    val divide              = 44
    val plus                = 45
    val power               = 46

    val equal               = 50
    val equalEqual          = 51
    val notEqual            = 52
    val less                = 53
    val lessEqual           = 54
    val more                = 55
    val moreEqual           = 56


}
