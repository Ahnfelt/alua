package com.github.ahnfelt.alua

import java.nio.file.{Files, Path, Paths}

import com.github.ahnfelt.alua.language.Tokenizer
import com.github.ahnfelt.alua.language.Tokenizer.Token

import scala.io.Source

object Main {
    def main(arguments : Array[String]) : Unit = {
        val bytes = Files.readAllBytes(Paths.get(arguments(0)))
        val tokens = Tokenizer.tokenize(bytes)
        for(tokenBits <- tokens.drop(8)) {
            val token = new Token(tokenBits)
            println(token.kind + " " + token.offset + " " + token.length)
        }
    }
}
