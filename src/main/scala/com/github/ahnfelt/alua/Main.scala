package com.github.ahnfelt.alua

import java.nio.file.{Files, Path, Paths}

import com.github.ahnfelt.alua.language.{Parser, Tokenizer}
import com.github.ahnfelt.alua.language.Tokenizer.Token

object Main {
    def main(arguments : Array[String]) : Unit = {
        val utf8 = Files.readAllBytes(Paths.get(arguments(0)))
        /*
        val moreBytes = (for(i <- 1 to 10000) yield utf8).toArray.flatten
        println(moreBytes.length + " bytes, " + moreBytes.count(_ == '\n') + " lines")
        for(i <- 1 to 10) process(moreBytes)
         */
        val tokens = Tokenizer.tokenize(utf8)
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
