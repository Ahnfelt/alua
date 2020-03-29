package com.github.ahnfelt.alua

import java.nio.file.{Files, Path, Paths}

import com.github.ahnfelt.alua.language.{Parser, Tokenizer}
import com.github.ahnfelt.alua.language.Tokenizer.Token

object Main {
    def main(arguments : Array[String]) : Unit = {
        val utf8 = Files.readAllBytes(Paths.get(arguments(0)))
        val tokens = Tokenizer.tokenize(utf8)
        //for(t <- tokens) println(new Token(t).kind)
        val term = new Parser(utf8, tokens).parseTerm()
        println(term)
    }

    def process(bytes : Array[Byte]) : Unit = {
        val tokens = measure("tokenize", Tokenizer.tokenize(bytes))
        var count = 0L
        for(tokenBits <- tokens.drop(8)) {
            val token = new Token(tokenBits)
            count += tokenBits
            //println(token.kind + " " + token.offset + " " + token.length)
        }
        println(count)
    }

    def measure[T](label : String, body : => T) = {
        val before = System.nanoTime()
        val result = body
        val elapsed = System.nanoTime() - before
        println(label + ": " + (elapsed / 1000000L) + " ms")
        result
    }

}
