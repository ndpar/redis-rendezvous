package com.ndpar.redis.rendezvous

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit.MILLISECONDS

private const val TIMEOUT = 20_000L

interface ChannelSubscription {
    fun subscribe(key: String, store: Channel<String>)
    fun unsubscribe(key: String)
}

@Component
class RedisTopicListener : MessageListener, ChannelSubscription {
    private val subscriptions: ConcurrentMap<String, Channel<String>> = ConcurrentHashMap()

    override fun subscribe(key: String, store: Channel<String>) {
        println("Subscribing: $key=$store")
        subscriptions.putIfAbsent(key, store)
    }

    override fun unsubscribe(key: String) {
        println("Unsubscribing: $key")
        subscriptions.remove(key)?.close()
    }

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val key = String(message.channel)
        println("On message: $key=$message")
        notify(key, message.toString())
    }

    private fun notify(key: String, value: String) {
        GlobalScope.launch(Dispatchers.IO) {
            subscriptions.get(key)?.send(value)
        }
    }
}

interface MessageBroker {
    fun leftSend(key: String, value: String)
    fun leftReceive(key: String): String?

    fun rightSend(key: String, value: String)
    fun rightReceive(key: String): String?
}

@Component
class RedisMessageBroker(
    @Autowired private val template: StringRedisTemplate,
    @Autowired private val listener: ChannelSubscription
) : MessageBroker {

    override fun leftSend(key: String, value: String) = send(key.left, value)
    override fun leftReceive(key: String): String? = receive(key.right)

    override fun rightSend(key: String, value: String) = send(key.right, value)
    override fun rightReceive(key: String): String? = receive(key.left)

    private fun send(key: String, value: String) =
        with(template) {
            opsForValue().set(key, value, TIMEOUT, MILLISECONDS)
            convertAndSend(key, value)
        }

    private fun receive(key: String): String? =
        (template.opsForValue().get(key) ?: subscribe(key)).also { template.delete(key) }

    private fun subscribe(key: String): String? = runBlocking {
        val rendezvous = Channel<String>()
        listener.subscribe(key, rendezvous)
        val result = withTimeoutOrNull(TIMEOUT) {
            rendezvous.receive()
        }
        listener.unsubscribe(key)
        result
    }

    private val String.left: String
        get() = "LEFT:$this"

    private val String.right: String
        get() = "RIGHT:$this"
}
