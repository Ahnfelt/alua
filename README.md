# Alua
Alua is a modern, typed, expression oriented programming language that you can "read out loud" like BASIC and Lua. The goal is that the language can be easily taught to beginners, and still scale to advanced users beyond the limits of e.g. Java and C#.

# A taste of Alua

```
function main(system: System): Task[Unit]
  await copyFile(system.files, "in.txt", "out.txt")
end

function copyFile(fs: FileSystem, in: String, out: String): Task[Unit]
  local bytes = await fs.readBytes(fs, in)
  await fs.writeBytes(fs, out, bytes)
end
```

