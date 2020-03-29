package com.github.ahnfelt.alua.language

import com.github.ahnfelt.alua.language.Tokenizer.Token

// Mnemonics:
// E for expression (aka term)
// T for type
// P for pattern
// D for definition

object Syntax {


    sealed abstract class Definition { def at : Token }
    case class DFunction(value : FunctionDefinition) extends Definition { def at = value.at }
    case class DMethod(value : FunctionDefinition) extends Definition { def at = value.at }
    case class DValue(name : Token, valueType : Option[Type], value : Term) extends Definition { def at = name }
    case class DInstance(
        typeName : QualifiedName,
        typeGenerics : List[TypeParameter],
        typeClassName : QualifiedName,
        methods : List[FunctionDefinition],
    ) extends Definition { def at = typeName.at }
    case class DInstanceUsing(
        typeName : QualifiedName,
        typeGenerics : List[TypeParameter],
        typeClassName : QualifiedName,
        using : Term,
    ) extends Definition { def at = typeName.at }
    case class DType(
        name : Token,
        generics : List[TypeParameter],
        parameters : List[Parameter],
        variadic : Boolean,
        methods : List[Signature],
        variants : List[Variant]
    ) extends Definition { def at = name }


    sealed abstract class Term { def at : Token }
    case class EString(at : Token) extends Term
    case class EInt(at : Token) extends Term
    case class EFloat(at : Token) extends Term
    case class ELambda(at : Token, parameters : List[Option[Token]], body : Term) extends Term
    case class EFunctions(functions : List[FunctionDefinition]) extends Term { def at = functions.head.at }
    case class ELocal(name : Token, valueType : Option[Type], value : Term) extends Term { def at = name }
    case class EAssign(name : Token, operator : Option[Token], value : Term) extends Term { def at = name }
    case class EIf(at : Token, branches : List[IfBranch], otherwise : List[Term]) extends Term
    case class EWhile(at : Token, condition : Term, body : List[Term]) extends Term
    case class EUnary(at : Token, operator : Option[Token], value : Term) extends Term
    case class EBinary(at : Token, operator : Option[Token], left : Term, right : Term) extends Term
    case class EVariable(name : QualifiedName) extends Term { def at = name.at }
    case class EVariant(name : QualifiedName, arguments : Arguments) extends Term { def at = name.at }
    case class EField(value : Term, name : Token) extends Term { def at = name }
    case class ECall(value : Term, name : Token, arguments : Arguments) extends Term { def at = name }
    case class EMatch(at : Token, values : List[Term], cases : List[MatchCase]) extends Term
    case class EAwait(at : Token, value : Term) extends Term


    sealed abstract class Type { def at : Token }
    case class TType(typeName : QualifiedName, arguments : List[Type]) extends Type { def at = typeName.at }
    case class TVariant(typeName : QualifiedName, variantName : Token, arguments : List[Type]) extends Type { def at = variantName }
    case class TVariable(at : Token, index : Int) extends Type


    sealed abstract class MatchPattern { def at : Token }
    case class PAs(variantName : Token, variableName : Token) extends MatchPattern { def at = variantName }
    case class PWildcard(at : Token) extends MatchPattern


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
        generics : List[Type],
        arguments : List[Argument]
    )

    case class Argument(
        name : Option[Token],
        value : Term
    )

    case class FunctionDefinition(
        signature : Signature,
        body : List[Term]
    ) { def at = signature.at }

    case class Variant(
        name : Token,
        parameters : List[Parameter],
        variadic : Boolean,
        methods : List[Signature],
    ) { def at = name }

    case class Signature(
        name : Token,
        generics : List[TypeParameter],
        parameters : List[Parameter],
        variadic : Boolean,
        returnType : Option[Type]
    ) { def at = name }

    case class Parameter(
        name : Token,
        parameterType : Option[Type],
        defaultValue : Option[Term]
    ) { def at = name }

    case class TypeParameter(
        name : Token,
        typeClasses : List[QualifiedName]
    ) { def at = name }

    case class QualifiedName(
        module : List[Token],
        name : Token
    ) { def at = name }


}
