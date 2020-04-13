package com.github.ahnfelt.alua.stuff

import scala.collection.mutable.ArrayBuffer

/////////////////////////////////
// Tests
/////////////////////////////////

object Inference {

    def main(arguments : Array[String]) : Unit = {
        printInfer( // \x. x + x
            ELambda("x", EApply(EApply(EVariable("+"), EVariable("x")), EVariable("x")))
        )
    }

    val initialEnvironment = Map(
        "+" -> // Int -> Int -> Int
            TApply(TApply(TConstructor("Function"), TConstructor("Int")),
                TApply(TApply(TConstructor("Function"), TConstructor("Int")), TConstructor("Int"))
            )
    )

    def infer(expression : Expression) : Type = {
        val inference = new Inference()
        val t = inference.inferType(expression, initialEnvironment)
        inference.solveConstraints()
        inference.substitute(t)
    }

    def printInfer(expression : Expression) : Unit = {
        try {
            println(expression + " : " + infer(expression))
        } catch {
            case e : RuntimeException => println(e.getMessage)
        }
    }

}


/////////////////////////////////
// Syntax tree
/////////////////////////////////

sealed abstract class Expression
case class ELambda(x : String, e : Expression) extends Expression
case class EApply(e1 : Expression, e2 : Expression) extends Expression
case class EVariable(x : String) extends Expression

sealed abstract class Type
case class TConstructor(name : String) extends Type
case class TApply(t1 : Type, t2 : Type) extends Type
case class TVariable(index : Int) extends Type

sealed abstract class Constraint
case class CEquality(t1 : Type, t2 : Type) extends Constraint


/////////////////////////////////
// Type inference
/////////////////////////////////

class Inference() {

    val typeConstraints = ArrayBuffer[Constraint]()
    val typeVariables = ArrayBuffer[Type]()

    def freshTypeVariable() : TVariable = {
        val result = TVariable(typeVariables.length)
        typeVariables += result
        result
    }

    def inferType(expression : Expression, environment : Map[String, Type]) : Type = expression match {
        case ELambda(x, e) =>
            val t1 = freshTypeVariable()
            val environment2 = environment.updated(x, t1)
            val t2 = inferType(e, environment2)
            TApply(TApply(TConstructor("Function"), t1), t2)
        case EApply(e1, e2) =>
            val t1 = inferType(e1, environment)
            val t2 = inferType(e2, environment)
            t1 match {
                case TApply(TApply(TConstructor("Function"), t3), t4) =>
                    typeConstraints += CEquality(t3, t2)
                    t4
                case _ =>
                    throw new RuntimeException("Can't apply non-function: " + t1)
            }
        case EVariable(x) =>
            environment.getOrElse(x,
                throw new RuntimeException("Variable not in scope: " + x)
            )
    }

    def solveConstraints() : Unit = {
        for(CEquality(t1, t2) <- typeConstraints) unify(t1, t2)
        typeConstraints.clear()
    }

    def unify(t1 : Type, t2 : Type) : Unit = (t1, t2) match {
        case (TVariable(i), _) if typeVariables(i) != TVariable(i) => unify(typeVariables(i), t2)
        case (_, TVariable(i)) if typeVariables(i) != TVariable(i) => unify(t1, typeVariables(i))
        case (TVariable(i), _) =>
            if(occursIn(i, t2)) throw new RuntimeException("Infinite type: _" + i + " = " + t2)
            typeVariables(i) = t2
        case (_, TVariable(i)) =>
            if(occursIn(i, t1)) throw new RuntimeException("Infinite type: _" + i + " = " + t1)
            typeVariables(i) = t1
        case (TApply(t3, t4), TApply(t5, t6)) =>
            unify(t3, t5)
            unify(t4, t6)
        case (TConstructor(name1), TConstructor(name2)) if name1 == name2 => ()
        case _ => throw new RuntimeException("Type mismatch: " + t1 + " vs. " + t2)
    }

    def occursIn(index : Int, t : Type) : Boolean = t match {
        case TVariable(i) if typeVariables(i) != TVariable(i) => occursIn(index, typeVariables(i))
        case TVariable(i) => i == index
        case TApply(t1, t2) => occursIn(index, t1) || occursIn(index, t2)
        case TConstructor(_) => false
    }

    def substitute(t : Type) : Type = t match {
        case TVariable(i) if typeVariables(i) != TVariable(i) => substitute(typeVariables(i))
        case TApply(t1, t2) => TApply(substitute(t1), substitute(t2))
        case _ => t
    }

}



