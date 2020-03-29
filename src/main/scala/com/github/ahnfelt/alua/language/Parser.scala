package com.github.ahnfelt.alua.language

import com.github.ahnfelt.alua.language.Parser.ParseException
import com.github.ahnfelt.alua.language.Syntax._
import com.github.ahnfelt.alua.language.Tokenizer.Token
import com.github.ahnfelt.alua.language.{Tokenizer => L}

class Parser(utf8 : Array[Byte], tokens : Array[Long]) {

    private var offset = 8

    private def peek() : Token = new Token(tokens(offset))
    private def peekPeek() : Token = new Token(tokens(offset + 1))
    private def peekPeekPeek() : Token = new Token(tokens(offset + 2))

    def fail(at : Token, message : String) = {
        throw ParseException(at, utf8, message)
    }

    private def skipKind(kind : Int) : Token = {
        val token = peek()
        if(token.kind != kind) {
            fail(token, "Expected token kind #" + kind)
        }
        offset += 1
        token
    }

    private def skipText(text : String) : Token = {
        val token = peek()
        if(token.text(utf8) != text) {
            fail(token, "Expected token text '" + text + "'")
        }
        token
    }

    def parseTerm() : Term = {
        val token = peek()
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
            if(peek().kind != L.dot) {
                val arguments = parseArguments()
                EVariant(QualifiedName(List(), token), arguments)
            } else {
                skipKind(L.dot)
                var qualifiers = List(token)
                while(peek().kind == L.upper && peekPeek().kind == L.dot) {
                    qualifiers ::= peek()
                    skipKind(L.upper)
                    skipKind(L.dot)
                }
                val token2 = peek()
                if(token2.kind == L.lower) {
                    EVariable(QualifiedName(qualifiers.reverse, token2))
                } else if(token2.kind == L.upper) {
                    val arguments = parseArguments()
                    EVariant(QualifiedName(qualifiers.reverse, token2), arguments)
                } else {
                    fail(token, "Expected identifier")
                }
            }
        } else {
            fail(token, "Unexpected token")
        }
    }

    def parseArguments() : Arguments = {
        var generics = List[Type]()
        if(peek().kind == L.squareImmediate) {
            skipKind(L.squareImmediate)
            while(peek().kind != L.squareEnd) {
                generics ::= parseType()
                if(peek().kind != L.squareEnd) skipKind(L.comma)
            }
            skipKind(L.squareEnd)
        }
        var arguments = List[Argument]()
        if(peek().kind == L.roundImmediate) {
            skipKind(L.roundImmediate)
            while(peek().kind != L.roundEnd) {
                val name = if(peek().kind == L.lower && peekPeek().kind == L.equal) Some {
                    val result = skipKind(L.lower)
                    skipKind(L.equal)
                    result
                } else None
                arguments ::= Argument(name, parseTerm())
                if(peek().kind != L.roundEnd) skipKind(L.comma)
            }
            skipKind(L.roundEnd)
        }
        Arguments(generics.reverse, arguments.reverse)
    }

    def parseType() : Type = {
        val token = peek()
        if(token.kind == L.upper) {
            skipKind(L.upper)
            var arguments = List[Type]()
            if(peek().kind == L.squareImmediate) {
                skipKind(L.squareImmediate)
                while(peek().kind != L.squareEnd) {
                    arguments ::= parseType()
                    if(peek().kind != L.squareEnd) skipKind(L.comma)
                }
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
