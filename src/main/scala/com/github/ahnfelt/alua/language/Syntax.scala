package com.github.ahnfelt.alua.language

object Syntax {


    case class Location(file : String, line : Int, column : Int)


    sealed abstract class Definition { def at : Location }
    case class DFunction(value : FunctionDefinition) extends Definition { def at = value.at }
    case class DMethod(value : FunctionDefinition) extends Definition { def at = value.at }
    case class DValue(name : Name, valueType : Option[Type], value : Term) extends Definition { def at = name.at }
    case class DInstance(
        typeName : Name,
        typeGenerics : List[TypeParameter],
        typeClassName : Name,
        methods : List[FunctionDefinition],
    ) extends Definition { def at = typeName.at }
    case class DType(
        name : Name,
        generics : List[TypeParameter],
        parameters : List[Parameter],
        variadic : Boolean,
        methods : List[Signature],
        variants : List[Variant]
    ) extends Definition { def at = name.at }


    sealed abstract class Statement { def at : Location }
    case class STerm(term : Term) extends Statement { def at = term.at }
    case class SFunctions(functions : List[FunctionDefinition]) extends Statement { def at = functions.head.at }
    case class SLocal(name : Name, value : Term) extends Statement { def at = name.at }
    case class SAssign(name : Name, operator : Option[String], value : Term) extends Statement { def at = name.at }
    case class SLoop(at : Location, repeat : Boolean, condition : Term, statements : List[Statement]) extends Statement


    sealed abstract class Term { def at : Location }


    sealed abstract class Type { def at : Location }
    case class TConstructor(name : Name, arguments : List[Type]) extends Type { def at = name.at }
    case class TVariable(at : Location, index : Int) extends Type


    case class FunctionDefinition(
        signature : Signature,
        statements : List[Statement]
    ) { def at = signature.at }

    case class Variant(
        name : Name,
        parameters : List[Parameter],
        variadic : Boolean,
        methods : List[Signature],
    ) { def at = name.at }

    case class Signature(
        name : Name,
        generics : List[TypeParameter],
        parameters : List[Parameter],
        variadic : Boolean,
        returnType : Option[Type]
    ) { def at = name.at }

    case class Parameter(
        name : Name,
        parameterType : Option[Type],
        defaultValue : Option[Term]
    ) { def at = name.at }

    case class TypeParameter(
        name : Name,
        typeClasses : List[Name]
    ) { def at = name.at }

    case class Name(at : Location, name : String)


}
