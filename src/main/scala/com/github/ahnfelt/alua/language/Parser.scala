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

    def parseStatements() : List[Term] = {
        var terms = List[Term]()
        while(
            peek().kind != L.keywordEnd &&
            peek().kind != L.keywordCase &&
            peek().kind != L.keywordElseif &&
            peek().kind != L.keywordElse
        ) {
            terms ::= parseTerm()
        }
        terms.reverse
    }

    def parseTerm() : Term = {
        parseControl()
    }

    def parseControl() : Term = {
        val token = peek()
        if(token.kind == L.keywordIf) {
            skipKind(L.keywordIf)
            val condition = parseTerm()
            skipKind(L.keywordThen)
            val body = parseStatements()
            var branches = List[IfBranch](IfBranch(condition, body))
            while(peek().kind == L.keywordElseif) {
                skipKind(L.keywordElseif)
                val condition = parseTerm()
                skipKind(L.keywordThen)
                val body = parseStatements()
                branches ::= IfBranch(condition, body)
            }
            val otherwise = if(peek().kind == L.keywordElse) {
                skipKind(L.keywordElse)
                parseStatements()
            } else List()
            skipKind(L.keywordEnd)
            EIf(token, branches.reverse, otherwise)
        } else if(token.kind == L.keywordWhile) {
            skipKind(L.keywordWhile)
            val condition = parseTerm()
            val body = parseStatements()
            skipKind(L.keywordEnd)
            EWhile(token, condition, body)
        } else if(token.kind == L.keywordLocal) {
            skipKind(L.keywordLocal)
            val name = skipKind(L.lower)
            val valueType = if(peek().kind == L.colon) {
                skipKind(L.colon)
                Some(parseType())
            } else None
            skipKind(L.equal)
            val value = parseTerm()
            ELocal(name, valueType, value)
        } else if(token.kind == L.keywordFunction) {
            var functions = List[FunctionDefinition]()
            while(peek().kind == L.keywordFunction) {
                skipKind(L.keywordFunction)
                val signature = parseSignature()
                val body = parseStatements()
                skipKind(L.keywordEnd)
                functions ::= FunctionDefinition(signature, body)
            }
            EFunctions(functions.reverse)
        } else {
            parseApply()
        }
    }

    def parseApply() : Term = {
        var left = parseAtomic()
        while(true) {
            val token = peek()
            if(token.kind == L.dot) {
                skipKind(L.dot)
                val name = skipKind(L.lower)
                if(peek().kind == L.squareImmediate || peek().kind == L.roundImmediate) {
                    val arguments = parseArguments()
                    left = ECall(left, name, arguments)
                } else {
                    left = EField(left, name)
                }
            } else if(token.kind == L.squareImmediate || token.kind == L.roundImmediate) {
                val arguments = parseArguments()
                left = ECall(left, token, arguments)
            } else {
                return left
            }
        }
        left
    }

    def parseAtomic() : Term = {
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
        } else if(token.kind == L.roundBegin) {
            skipKind(L.roundBegin)
            val result = parseTerm()
            skipKind(L.roundEnd)
            result
        } else if(token.kind == L.fatArrow || peekPeek().kind == L.fatArrow || peekPeek().kind == L.semicolon) {
            parseLambda()
        } else if(token.kind == L.lower) {
            skipKind(L.lower)
            EVariable(QualifiedName(List(), token))
        } else if(token.kind == L.upper) {
            val qualifiers = parseQualifiers()
            val token2 = peek()
            if(token2.kind == L.lower) {
                skipKind(L.lower)
                EVariable(QualifiedName(qualifiers.reverse, token2))
            } else if(token2.kind == L.upper) {
                skipKind(L.upper)
                val arguments = parseArguments()
                EVariant(QualifiedName(qualifiers.reverse, token2), arguments)
            } else {
                fail(token2, "Expected identifier")
            }
        } else {
            fail(token, "Unexpected token")
        }
    }

    def parseQualifiers() : List[Token] = {
        var qualifiers = List[Token]()
        while(peek().kind == L.upper && peekPeek().kind == L.dot) {
            qualifiers ::= peek()
            skipKind(L.upper)
            skipKind(L.dot)
        }
        qualifiers
    }

    def parseLambda() : ELambda = {
        val token = peek()
        if(token.kind == L.fatArrow) {
            skipKind(L.fatArrow)
            ELambda(token, List(), parseTerm())
        } else {
            var parameters = List[Option[Token]]()
            while(peek().kind != L.fatArrow) {
                parameters ::= {
                    if(peek().kind == L.lower) {
                        Some(skipKind(L.lower))
                    } else {
                        skipKind(L.underscore)
                        None
                    }
                }
                if(peek().kind != L.fatArrow) skipKind(L.semicolon)
            }
            skipKind(L.fatArrow)
            ELambda(token, parameters.reverse, parseTerm())
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

    def parseSignature() : Signature = {
        val name = skipKind(L.lower)
        var generics = List[TypeParameter]()
        if(peek().kind == L.squareImmediate) {
            skipKind(L.squareImmediate)
            while(peek().kind != L.squareEnd) {
                generics ::= parseTypeParameter()
                if(peek().kind != L.squareEnd) skipKind(L.comma)
            }
            skipKind(L.squareEnd)
        }
        var variadic = false
        var parameters = List[Parameter]()
        if(peek().kind == L.roundImmediate) {
            skipKind(L.roundImmediate)
            while(!variadic && peek().kind != L.roundEnd) {
                val parameterName = skipKind(L.lower)
                val parameterType = if(peek().kind == L.colon) {
                    skipKind(L.colon)
                    Some(parseType())
                } else None
                val defaultValue = if(peek().kind == L.equal) {
                    skipKind(L.equal)
                    if(peek().kind == L.dotDotDot) {
                        skipKind(L.dotDotDot)
                        variadic = true
                        None
                    } else {
                        Some(parseTerm())
                    }
                } else None
                parameters ::= Parameter(parameterName, parameterType, defaultValue)
                if(peek().kind != L.roundEnd) skipKind(L.comma)
            }
            skipKind(L.roundEnd)
        }
        val returnType = if(peek().kind == L.colon) {
            skipKind(L.colon)
            Some(parseType())
        } else None
        Signature(name, generics.reverse, parameters.reverse, variadic, returnType)
    }

    def parseTypeParameter() : TypeParameter = {
        val name = skipKind(L.upper)
        var typeClasses = List[QualifiedName]()
        while(peek().kind == L.colon) {
            skipKind(L.colon)
            val qualifiers = parseQualifiers()
            typeClasses ::= QualifiedName(qualifiers, skipKind(L.upper))
        }
        TypeParameter(name, typeClasses.reverse)
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
