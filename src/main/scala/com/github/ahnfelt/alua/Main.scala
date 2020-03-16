package com.github.ahnfelt.alua

import java.nio.file.{Files, Path, Paths}

import com.github.ahnfelt.alua.language.Tokenizer
import com.github.ahnfelt.alua.language.Tokenizer.Token

object Main {
    def main(arguments : Array[String]) : Unit = {
        val bytes = Files.readAllBytes(Paths.get(arguments(0)))
        val moreBytes = (for(i <- 1 to 10000) yield bytes).toArray.flatten
        println(moreBytes.length + " bytes, " + moreBytes.count(_ == '\n') + " lines")
        for(i <- 1 to 10) process(moreBytes)
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
        println(label + ": " + (elapsed / 1000) + " microseconds")
        result
    }

}
