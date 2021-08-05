package svcs

import java.io.File
import java.security.MessageDigest


fun main(args: Array<String>) {
    createDir("vcs")
    val arg = if (args.size > 1) args[1] else ""
    when (args.firstOrNull()) {
        null -> printHelp()
        "--help" -> printHelp()
        "add" -> add(arg)
        "checkout" -> checkout(arg)
        "config" -> config(arg)
        "commit" -> commit(arg)
        "log" -> log()
        else -> println("'${args[0]}' is not a SVCS command.")
    }
}


fun createDir(dir: String) {
    if (!File(dir).exists()) {
        File(dir).mkdir()
    }
}


fun printHelp() = println(
    """These are SVCS commands:
config     Get and set a username.
add        Add a file to the index.
log        Show commit logs.
commit     Save changes.
checkout   Restore a file.""")


fun add(arg: String) {
    val indexFile = File("vcs\\index.txt")
    if (!indexFile.exists() && arg.isEmpty()) {
        indexFile.createNewFile()
        println("Add a file to the index.")
        return
    }

    if (arg.isNotEmpty()) {
        if (File(arg).exists()) {
            indexFile.appendText("$arg\n")
            println("The file '$arg' is tracked.")
        } else {
            println("Can't find '$arg'.")
        }
        return
    }

    val files = indexFile.readLines()
    println("Tracked files:")
    files.forEach(::println)
}


fun checkout(arg: String) {
    if (arg.isEmpty()) {
        println("Commit id was not passed.")
        return
    }

    val commit = File("vcs\\commits").listFiles()?.find { arg in it.name }
    if (commit == null) {
        println("Commit does not exist.")
        return
    }

    for (file in commit.listFiles()!!) {
        file.copyTo(File(file.name), true)
    }
    println("Switched to commit $arg.")
}


fun config(arg: String) {
    val configFile = File("vcs\\config.txt")
    if (!configFile.exists() && arg.isEmpty()) {
        configFile.createNewFile()
        println("Please, tell me who you are.")
        return
    }

    if (arg.isNotEmpty()) {
        configFile.writeText(arg)
    }
    println("The username is ${configFile.readText()}.")
}


fun commit(arg: String) {
    if (arg.isEmpty()) {
        println("Message was not passed.")
        return
    }

    val indexFile = File("vcs\\index.txt")
    if (!indexFile.exists() || indexFile.readText().isEmpty()) {
        println("Nothing to commit.")
        return
    }

    createDir("vcs\\commits")
    val logFile = File("vcs\\log.txt")
    val fileNames = indexFile.readLines()
    val hash = computeHash(fileNames)
    if (!logFile.exists()) {
        logFile.createNewFile()
    } else {
        val lastHash = logFile.readLines().last().split("\\0").first()
        if (lastHash == hash) {
            println("Nothing to commit.")
            return
        }
    }
    makeCommit(hash, fileNames)
    logFile.appendText("$hash\\0${File("vcs\\config.txt").readText()}\\0$arg\n")
    println("Changes are committed.")
}

fun makeCommit(hash: String, names: List<String>) {
    createDir("vcs\\commits\\$hash")
    for (name in names) {
        File(name).copyTo(File("vcs\\commits\\$hash\\$name"))
    }
}

fun computeHash(names: List<String>): String {
    val md = MessageDigest.getInstance("SHA-1")
    md.reset()
    for (name in names) {
        val fileContent = File(name).readBytes()
        md.update(fileContent)
    }

    return md.digest().joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}


fun log() {
    val logFile = File("vcs\\log.txt")
    if (!logFile.exists()) {
        println("No commits yet.")
        return
    }

    val lines = logFile.readLines()
    for (line in lines.reversed()) {
        val info = line.split("\\0")
        println("commit ${info[0]}\nAuthor: ${info[1]}\n${info[2]}")
    }
}
