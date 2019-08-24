package com.purelymail.gpt2

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import kotlin.concurrent.thread

fun main(){
    MainServer()
}

class MainServer {
    val process = startProcess()
    val discord = connectDiscord()

    fun startProcess() : Process {
        val process = ProcessBuilder()
            .directory(File("/gpt-2"))
            .command("python3", "src/bot_samples.py", "--top_k", "40", "--length", "200", "--model_name", "774M")
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        return process
    }

    private fun readResponse() : String {
        val buffer = ByteArrayOutputStream()
        while(true){
            val result = process.inputStream.read()
            if(result > 0){
                val c = result.toChar()
                if(c == '\n'){
                    return buffer.toByteArray().toString(Charsets.UTF_8)
                } else {
                    buffer.write(result)
                }
            } else {
                break
            }
        }
        throw java.lang.IllegalStateException("Process terminated")
    }

    fun processRequest(prompt : String) : String {
        synchronized(this){
            process.outputStream.write(Base64.getEncoder().encode(prompt.toByteArray()))
            process.outputStream.write("\n".toByteArray())
            process.outputStream.flush()

            val decoded = Base64.getDecoder().decode(readResponse()).toString(Charsets.UTF_8)
            val parts = decoded.split("<|endoftext|>")
            return parts[0]
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
                        val typing = thread {
                            try {
                                while (true) {
                                    event.channel.sendTyping().submit().get()
                                    Thread.sleep(5000)
                                }
                            } catch(t : InterruptedException){}
                        }
                        try {
                            val response = processRequest(prompt)
                            println("Response: $response")
                            event.channel.sendMessage("$prompt $response").queue()
                        } finally {
                            typing.interrupt()
                        }
                    }
                }
            })
            .build()
    }
}

