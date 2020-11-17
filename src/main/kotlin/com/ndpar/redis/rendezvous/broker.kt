package com.ndpar.redis.rendezvous

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit.MILLISECONDS

private const val TIMEOUT = 20_000L

interface MessageBroker {
    fun leftSend(key: String, value: String)
    fun leftReceive(key: String): String?

    fun rightSend(key: String, value: String)
    fun rightReceive(key: String): String?
}

@Component
class RedisMessageBroker(
    @Autowired private val template: RedisTemplate<String, String>
) : MessageBroker {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun leftSend(key: String, value: String) = send(key.left, value)
    override fun leftReceive(key: String): String? = receive(key.right)

    override fun rightSend(key: String, value: String) = send(key.right, value)
    override fun rightReceive(key: String): String? = receive(key.left)

    private fun send(key: String, value: String) {
        logger.debug("Setting k/v: {}={}", key, value)
        template.opsForList().leftPush(key, value)
        template.expire(key, TIMEOUT, MILLISECONDS)
    }

    private fun receive(key: String): String? {
        logger.debug("Waiting for k={}", key)
        return template.opsForList().leftPop(key, TIMEOUT, MILLISECONDS)
    }

    private val String.left: String
        get() = "LEFT:$this"

    private val String.right: String
        get() = "RIGHT:$this"
}
