package com.purelymail.gpt2

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.thread

fun main(){
    MainServer()
}

class MainServer {
    val processInput = startProcess()
    val discord = connectDiscord()

    fun startProcess() : OutputStream {
        val process = ProcessBuilder()
            .directory(File("/gpt-2"))
            .command("python3", "src/interactive_conditional_samples.py", "--top_k", "40", "--length", "200")
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        thread {
            val buffer = ByteArrayOutputStream()
            while(true){
                val result = process.inputStream.read()
                if(result > 0){
                    val c = result.toChar()
                    if(c == '\n'){
                        outputLines.add(buffer.toByteArray().toString(Charsets.UTF_8))
                        buffer.reset()
                    } else {
                        buffer.write(result)
                    }
                } else {
                    break
                }
            }
        }
        return process.outputStream
    }

    val outputLines = LinkedBlockingDeque<String>()

    fun processRequest(prompt : String) : String {
        synchronized(this){
            val response = mutableListOf<String>()
            processInput.write((prompt.trim() + " \n").toByteArray())
            processInput.flush()
            var ended = false
            while(true){
                val rawLine = outputLines.takeFirst()
                println("Got line: $rawLine")
                val line = rawLine.replace("Model prompt >>>", "").replace("======================================== SAMPLE 1 ========================================", "")
                if(line.isEmpty()){
                    continue
                }
                if(line.trim() == "================================================================================"){
                    break
                }
                if(ended) {
                    continue
                }
                val parts = line.split("<|endoftext|>")
                if(parts.size > 1){
                    ended = true
                }
                response.add(parts[0])
            }
            return response.joinToString("\n")
        }
    }

    fun connectDiscord() : JDA {
        val token = System.getenv("DISCORD_TOKEN") ?: throw IllegalStateException("Discord token not supplied")
        return JDABuilder(token)
            .addEventListener(object : ListenerAdapter() {
                override fun onMessageReceived(event: MessageReceivedEvent) {
                    if(event.message.contentStripped.startsWith("!gpt ") && event.author != discord.selfUser){
                        val prompt = event.message.contentStripped.removePrefix("!gpt ")
                        println("Got prompt: $prompt")
                        val response = processRequest(prompt)
                        println("Response: $response")
                        event.channel.sendMessage("$prompt $response").queue()
                    }
                }
            })
            .build()
    }
}

