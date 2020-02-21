# Alua
Alua is a modern, typed, expression oriented programming language that you can "read out loud" like BASIC and Lua. The goal is that the language can be easily taught to beginners, and still scale to advanced users beyond the limits of e.g. Java and C#.

Differences from Lua:

 * Alua has no global state.
 * Alua is typed and has local type inference.
 * Alua has `await` and type classes.

Alua features a unified type system, where all types are defined in the same manner, and which supports both functional and object oriented programming - although composition is used instead of inheritance. 

The final difference is that Alua allows *effect transparency*.


# A taste of Alua

```
function main(system: System): Task[Unit]
  copyFile(system.files, "in.txt", "out.txt")
end

function copyFile(fs: FileSystem, in: String, out: String): Task[Unit]
  local bytes = await fs.readBytes(fs, in)
  await fs.writeBytes(fs, out, bytes)
end
```


# Await

The `await` keyword is for so-called *monadic* computations. A single `await foo` is syntactic sugar for `foo.map(...)` where `...` is the rest of the code in the enclosing function. If there are multiple awaits, all but the last one are syntactic sugar for `.flatMap(...)`. If `await` is the last thing in the function body, it doesn't do anything other than forcing the previous awaits to be `flatMap`s.

One of the important use cases for this is asynchronous I/O, which is modelled by `Task[A]`, where `A` is the type of the value that will eventually be produced.


# System, FileSystem and other effects

Alua uses *object capabilities* to control which code can do what. 

It's a simple mechanism: The `main` function gets an instance of `System`, which allows you to do anything, and you can then delegate responsibility to subfunctions by passing them either `system` or one of its fields, such as `system.files`, which only lets you access the file system. Other examples are `system.network` and `system.environment` etc. 

Of course, when functions don't get an instance of these either via their scope or their arguments, they have no way of doing anything but pure computation. Since there is no global state, you can't leak capabilities via global variables. This means you can trust that libraries don't "phone home" or install viruses or randomware, unless you give them the capabilities they need to do that. Equally important, it means you can more easily see what code does, since effects are explicit.

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
  | '.' '{' [fields] [','] '}' ['with' [method [...]] 'end']
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


# Type definitions

typeD = 
  'type' UPPER [generics] [parameters]
  ['with' [constructorD [...] | methodD [...]] 'end']

constructorD = 'variant' UPPER [parameters] ['has' [methodD [...]]]

methodD = 'method' VARIABLE signature

instanceD = 
  'instance' UPPER [generics] ':' UPPER 
  'with' [methodD block 'end' [...]] 'end'
```
