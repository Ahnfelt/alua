package com.github.ahnfelt.alua.language

// Mnemonics:
// E for expression (aka term)
// T for type
// P for pattern
// D for definition

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


    sealed abstract class Term { def at : Location }
    case class EString(at : Location, value : String) extends Term
    case class EInt(at : Location, value : String) extends Term
    case class EFloat(at : Location, value : String) extends Term
    case class ELambda(at : Location, parameters : List[String], body : Term) extends Term
    case class EFunctions(functions : List[FunctionDefinition]) extends Term { def at = functions.head.at }
    case class ELocal(name : Name, valueType : Option[Type], value : Term) extends Term { def at = name.at }
    case class EAssign(name : Name, operator : Option[String], value : Term) extends Term { def at = name.at }
    case class ELoop(at : Location, repeat : Boolean, condition : Term, body : List[Term]) extends Term
    case class EIf(at : Location, branches : List[IfBranch], otherwise : List[Term]) extends Term
    case class EUnary(at : Location, operator : Option[String], value : Term) extends Term
    case class EBinary(at : Location, operator : Option[String], left : Term, right : Term) extends Term
    case class EVariable(module : List[Name], name : Name) extends Term { def at = name.at }
    case class EVariant(module : List[Name], name : Name, arguments : Arguments) extends Term { def at = name.at }
    case class EField(value : Term, name : Name) extends Term { def at = name.at }
    case class ECall(value : Term, name : Name, arguments : Arguments) extends Term { def at = name.at }
    case class EMatch(at : Location, values : List[Term], cases : List[MatchCase]) extends Term
    case class EAwait(at : Location, value : Term) extends Term


    sealed abstract class Type { def at : Location }
    case class TConstructor(name : Name, arguments : List[Type]) extends Type { def at = name.at }
    case class TVariable(at : Location, index : Int) extends Type


    sealed abstract class MatchPattern { def at : Location }
    case class PAs(variantName : Name, variableName : Name) extends MatchPattern { def at = variantName.at }
    case class PWildcard(at : Location) extends MatchPattern


    case class IfBranch(
        condition : Term,
        body : List[Term]
    )

    case class MatchCase(
        patterns : List[MatchPattern],
        condition : Option[Term],
        body : List[Term]
    )

    case class Arguments(
        name : Name,
        generics : List[Type],
        arguments : List[Argument]
    )

    case class Argument(
        name : Option[Name],
        value : Term
    )

    case class FunctionDefinition(
        signature : Signature,
        body : List[Term]
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
