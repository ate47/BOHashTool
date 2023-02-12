# BOHashTool

Tool to try hashes in a directory of GSC files, I made it like that, you can use something else if you want.

For a Java implementation of the hashing methods, the [fr.atesab.bo4hash.Hash](src/main/java/fr/atesab/bo4hash/Hash.java) is here

I use it to explore this fork repository: https://github.com/ate47/t8-src

## Usage

From the release files or via these commands to compile:

```powershell
./gradlew build
java -jar build/libs/BOHashTool-*.jar
```

## Demo

You can search for an unhashed string in a particular repo (the indexing can take ~20s or more for slower disks)

![example 1](docs/example_1.png)

It can be used to search for multiple objects at once

![example 2](docs/example_2.png)

Or path

![example 3](docs/example_3.png)