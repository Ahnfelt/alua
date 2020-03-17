package com.github.ahnfelt.alua.language

import com.github.ahnfelt.alua.language.Parser.ParseException
import com.github.ahnfelt.alua.language.Syntax._
import com.github.ahnfelt.alua.language.Tokenizer.Token
import com.github.ahnfelt.alua.language.{Tokenizer => L}

class Parser(utf8 : Array[Byte], tokens : Array[Long]) {

    private var offset = 8

    private def current() : Token = new Token(tokens(offset))
    private def peek() : Token = new Token(tokens(offset + 1))

    def fail(at : Token, message : String) = {
        throw ParseException(at, utf8, message)
    }

    private def skipKind(kind : Int) : Token = {
        val token = current()
        if(token.kind != kind) {
            fail(token, "Expected token kind #" + kind)
        }
        offset += 1
        token
    }

    private def skipText(text : String) : Token = {
        val token = current()
        if(token.text(utf8) != text) {
            fail(token, "Expected token text '" + text + "'")
        }
        token
    }

    def parseTerm() : Term = {
        val token = current()
        if(token.kind == L.integer) {
            skipKind(L.integer)
            EInt(token)
        } else if(token.kind == L.float) {
            skipKind(L.float)
            EFloat(token)
        } else if(token.kind == L.string) {
            skipKind(L.string)
            EString(token)
        } else if(token.kind == L.lower) {
            skipKind(L.lower)
            EVariable(QualifiedName(List(), token))
        } else if(token.kind == L.upper) {
            skipKind(L.upper)
            val arguments = parseArguments()
            EVariant(QualifiedName(List(), token), arguments)
        } else {
            fail(token, "Unexpected token")
        }
    }

    def parseArguments() : Arguments = {
        var generics = List[Type]()
        if(current().kind == L.squareImmediate) {
            skipKind(L.squareImmediate)
            while(current().kind != L.squareEnd && generics.isEmpty != (current().kind == L.comma)) {
                if(current().kind == L.comma) skipKind(L.comma)
                generics ::= parseType()
            }
            if(current().kind == L.comma) skipKind(L.comma)
            skipKind(L.squareEnd)
        }
        var arguments = List[Argument]()
        if(current().kind == L.roundImmediate) {
            skipKind(L.roundImmediate)
            while(current().kind != L.roundEnd && arguments.isEmpty != (current().kind == L.comma)) {
                if(current().kind == L.comma) skipKind(L.comma)
                val name = if(current().kind == L.lower && peek().kind == L.equal) Some {
                    val result = skipKind(L.lower)
                    skipKind(L.equal)
                    result
                } else None
                arguments ::= Argument(name, parseTerm())
            }
            if(current().kind == L.comma) skipKind(L.comma)
            skipKind(L.roundEnd)
        }
        Arguments(generics.reverse, arguments.reverse)
    }

    def parseType() : Type = {
        val token = current()
        if(token.kind == L.upper) {
            skipKind(L.upper)
            var arguments = List[Type]()
            if(current().kind == L.squareImmediate) {
                skipKind(L.squareImmediate)
                while(current().kind != L.squareEnd && arguments.isEmpty != (current().kind == L.comma)) {
                    if(current().kind == L.comma) skipKind(L.comma)
                    arguments ::= parseType()
                }
                if(current().kind == L.comma) skipKind(L.comma)
                skipKind(L.squareEnd)
            }
            TType(QualifiedName(List(), token), arguments.reverse)
        } else {
            fail(token, "Unexpected token")
        }
    }

}


object Parser {
    case class ParseException(at : Token, utf8 : Array[Byte], message : String) extends RuntimeException({
        val (line, column) = at.findLineAndColumn(utf8)
        message + " at line " + line + ", column " + column + ": " + at.text(utf8)
    })
}
