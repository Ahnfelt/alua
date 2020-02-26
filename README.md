# Alua
Alua is an attempt to design a modern, typed, expression oriented programming language that you can "read out loud" like BASIC and Lua. The goal is that the language can be easily taught to beginners, yet scales to advanced users. There is no implementation yet.

Differences from Lua:

 * Alua has no global state, and offers control over effects.
 * Alua is typed and has local type inference.
 * Alua has `await` and type classes.
 * Alua never needs a statement separator - so it doesn't have one.
 * Arrays are not maps, indexes start from 0, there is no `nil`.

Alua features a unified type system, where all types are defined in the same manner, and which supports both functional and object oriented programming - although composition is used instead of inheritance. 


# A taste of Alua

```
function main(system: System): Task[Unit]
  copyFile(system.files, "in.txt", "out.txt")
end

function copyFile(fs: FileSystem, in: String, out: String): Task[Unit]
  local bytes = await fs.readBytes(in)
  await fs.writeBytes(out, bytes)
end
```


# Await

The `await` keyword is for so-called *monadic* computations. A single `await foo` is syntactic sugar for `foo.map(...)` where `...` is the rest of the code in the enclosing function. If there are multiple awaits, all but the last one are syntactic sugar for `.flatMap(...)`. If `await` is the last thing in the function body, it doesn't do anything other than forcing the previous awaits to be `flatMap`s.

One of the important use cases for this is asynchronous I/O, which is modelled by `Task[A]`, where `A` is the type of the value that will eventually be produced.


# System, FileSystem and other effects

Alua uses *object capabilities* to control which code can do what. 

It's a simple mechanism: The `main` function gets an instance of `System`, which allows you to do anything, and you can then delegate responsibility to subfunctions by passing them either `system` or one of its fields, such as `system.files`, which only lets you access the file system. Other examples are `system.network` and `system.environment` etc. 

Of course, when functions don't get an instance of these either via their scope or their arguments, they have no way of doing anything but pure computation. Since there is no global state, you can't leak capabilities via global variables. This means you can trust that libraries don't "phone home" or install viruses or randomware, unless you give them the capabilities they need to do that. Equally important, it means you can more easily see what code does, since effects are explicit.

# Functions

Functions are defined using the `function` keyword:

```
function fib(n: Int): Int
  if n < 2 then n else
    fib(n - 1) + fib(n - 2)
  end
end
```

Top level functions and local functions may be mutually recursive. 

Ordinarily, pattern matching is done with `match foo.bar case Baz is ... end`, but since it's so common to match on function arguments, it's allowed to use `case`s in the end of a function to match on the function arguments, e.g.

```
function area(shape: Shape): Float
  case Circle as s is
    Float.pi * s.radius * s.radius
  case Rectangle as s is
    s.width * s.height
end
```

Lambda functions use `=>` for their syntax, e.g. `x => x + 1`. Multiple parameters are separated by `;`. Example:

```
list.map(x => x * x).filter(x => x < 10).foldLeft(0, total; value => total + value)
```


# Type definitions

Alua has a uniform type system, where all types are defined in the same way. The built-in types are no different.

```
type Animal with
  variant Duck
    method fly(): String
  variant Leopard
    method run(): String
end
```

A type starts with a name, and has zero or more variants. Before the variants, shared *generics*, *fields*, *methods* and *modifiers* may be specified.

Each variant may add its own fields, methods and modifiers.

If no variants are specified, but there is a shared field list or one or more shared methods, a variant with the same name as the type is automatically created.

```
type Parser with
  method parseTerm(input: String): Term
end
```

Often you have types without methods:

```
type Option[T] with
  variant None
  variant Some(value: T)
end
```

It's also common to have types that have one variant and is essentially a record:

```
type Point(x: Float, y: Float)
```

Creating values of the types is done by mentioning the variants, e.g.

```
local point = Point(5.0, 7.0)
```

If the variant has methods, those can be supplied in a `with` ... `end` block after the variant, e.g.

```
function newDuck(metersToNextLake: Float): Animal
  local miles = (metersToNextLake * 0.000621).round()
  Duck with
    method fly()
      "The duck flew \(miles) miles to the next lake."
    end
  end
end
```

Note that the new instance of `Duck` has type `Animal`. There is no subtyping in Alua.

A variant may have the `unboxed` modifier, which is a hint to the compiler that it should avoid a boxed representation of the type. When there's a single `unboxed` variant with a single field, it's guaranteed to use the same representation as the field type. If there are multiple variants or fields, it may not be possible get an unboxed representation, depending on the compilation target.

One common use of this is to give different IDs different types, so that they can't be confused with each other, while not incurring any runtime overhead:

```
type UserId(value: Int) unboxed
type GroupId(value: Int) unboxed
```

# Top level methods

Top-level methods can be added for types defined in the same file with the `method` keyword:

```
method map[A, B](values: Array[A], body: A => B): Array[B]
  local result = ArrayBuilder.empty[B]
  for value in values do
    result.push(body(value))
  end
  result.toArray()
end
```

The first parameter of top level methods corresponds to "`this`", so you call the above like `myArray.map(x => x + 1)`.


# Type class instances

In Alua all ordinary types with a single type parameter can be used as a *type class*. You can make *instances* of these available with the `instance` keyword, e.g.

```
type Order[T] with
  method less(a: T, b: T): Bool
end

instance Bool: Order with
  method less(a, b)
    case False, True is
      True
    case _, _ is
      False
  end
end

function sort[T: Order](values: Array[T]): Array[T]
  local order = resolve T: Order
  # ... 
end
```

The type parameters of an instance can be constrained: `instance List[T: Order]: Order with ... end`. Note the parameters to a type class are always type parameters, and never types. In Haskell parlance, there's no `FlexibleInstances` - but you can create an auxiliary type class to constrain the inner parameter if necessary. 

# All calls are method calls

In Alua, only methods can be called. When you use call syntax for a non-method, e.g. `f(x)`, the parser expands it to `f.call(x)`. This keeps the semantics of Alua simple.


# Modules and imports

The module system of Alua is very simple - a file is a module, which in turn is simply a namespace. All modules and types from the working path of the compiler are in scope. There are no import statements.

Symbols are resolved by the longest matching suffix. When ambiguous, the symbol defined nearest to the current file being compiled wins, where the distance into a directory each is measured as 1 and the distance out of a directory each is measured as 100. Functions and values from other modules must be prefixed with at least one module name.


# Incomplete grammar

```
block 
  = [statement [...]]

statement 
  = atomic | bind | loop

bind
  = 'function' VARIABLE signature block 'end'
  | 'local' var [',' var] '=' expression
  | VARIABLE ('=' | '+=' | '-=') expression

loop
  = 'while' expression 'do' block 'end'
  | 'repeat' block 'until' expression 'end'
  | 'for' var [',' var] 'in' expression 'do' block 'end'

atomic
  = atomic more
  | VARIABLE
  | '{' [['...'] expression [',' ...]] [','] '}'
  | '{' (fields | '=') [','] '}'
  | VARIABLE [';' ...] '=>' expression
  | UPPER [typeArguments] [arguments] ['with' [method [...]] 'end']
  | 'await' expression
  | if
  | match

expression 
  = atomic 
  | '-' expression 
  | '(' block ')' [more]

more
  = '.' LOWER
  | '.' '{' [fields] [','] '}'
  | [typeArguments] arguments
  | OP expression

if = 
  'if' expression 'then' block 
  ['elseif' expression 'then' block [...]]
  ['else' block]
  'end'

match =
  'match' expression [',' ...]
  ['case' (pattern [',' ...]) ['when' expression] 'is' block [...]]
  'end'

pattern
  = UPPER ['(' [pattern [',' ...]] [','] ')'] 'as' VARIABLE
  | '{' [['...'] pattern [',' ...]] '}'
  | '_'

fields = (LOWER '=' expression | '=' LOWER) [',' ...]
arguments = '(' [[VARIABLE '='] expression [',' ...]] [','] ')'

var = VARIABLE [':' type]

type = UPPER ['[' [type [...]] ']']

signature = [generics] parameters [':' type]

parameters = '(' [var [',' ...]] ')'

generics = '[' [UPPER [':' UPPER [...]] [',' ...]] ']'

typeArguments = '[' [type [',' ...]] [','] ']'

method = 'method' signature block 'end'


typeD = 
  'type' UPPER [generics] [parameters]
  ['with' [constructorD [...] | methodD [...]] 'end']

constructorD = 'variant' UPPER [parameters] [methodD [...]]

methodD = 'method' VARIABLE signature

instanceD = 
  'instance' UPPER [generics] ':' UPPER 
  'with' [method [...]] 'end'
```
