# Alua
Alua is a modern, typed, expression oriented programming language that you can "read out loud" like BASIC and Lua. The goal is that the language can be easily taught to beginners, yet scales to advanced users.

Differences from Lua:

 * Alua has no global state, and offers control over effects.
 * Alua is typed and has local type inference.
 * Alua has `await` and type classes.
 * Alua never needs a statement separator - so it doesn't have one.
 * Arrays are not also maps and indexes start from 0.

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

The type can have *common generics* and *common fields*. There are zero or more *variants* of the type, each with zero or more fields, which are used to construct instances of the type and which can be pattern matched on. Each variant has zero or more *methods*.

If there is only one variant, and it has the same name as the type, the `variant Foo` part can be left out, e.g.

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


# Top level methods

Top-level methods can be added for types defined in the same file with the `method` keyword:

```
method map[A, B](values: Array[A], body: A => B): Array[B]
  local result = ArrayBuilder[A](values.size())
  for index, value in values.pairs() do
    result.set(index, body(value))
  end
  result.toArray()
end
```

The first parameter of top level methods corresponds to "`this`", so you call the above like `myArray.map(x => x + 1)`.


# Type class instances

In Alua all ordinary types with a single type parameter can be used as a *type class*. You can make *instances* of these available with the `instance` keyword, e.g.

```
type Ordered[T] with
  method less(a: T, b: T): Bool
end

instance Bool: Ordered with
  method less(a, b)
    case False, True is
      True
    case _, _ is
      False
  end
end

function sort[T: Ordered](values: Array[T]): Array[T]
  # ...
end
```

# All calls are method calls

In Alua, only methods can be called. When you use call syntax for a non-method, e.g. `f(x)`, the parser expands it to `f.call(x)`. This keeps the semantics of Alua simple.


# Modules and imports

The module system of Alua is very simple - a file is a module, which in turn is simply a namespace. All modules and types from the working path of the compilers are in scope. 

Symbols are resolved by the longest matching suffix. When ambiguous, the symbol defined nearest to the current file being compiled wins, where the distance into and out of directories is both 1 each. Functions and values from other modules must be prefixed with at least one module name.


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
