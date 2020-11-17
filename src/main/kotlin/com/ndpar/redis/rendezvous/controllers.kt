package com.ndpar.redis.rendezvous

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.NotNull
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE as JSON

data class Message(
    @NotNull val id: String = "",
    @NotNull val message: String = "",
    val date: Date? = null
)

@RestController
@RequestMapping(path = ["/alice"])
class FirstController(
    @Autowired private val broker: MessageBroker
) {
    @PostMapping(consumes = [JSON], produces = [JSON])
    fun process(@Valid @RequestBody message: Message): Message {
        broker.leftSend(message.id, message.message)
        val body = broker.leftReceive(message.id)
        return if (body != null) {
            message.copy(message = body, date = Date())
        } else {
            message.copy(message = "TIMEOUT")
        }
    }
}

@RestController
@RequestMapping(path = ["/bob"])
class SecondController(
    @Autowired private val broker: MessageBroker
) {
    @PostMapping(consumes = [JSON], produces = [JSON])
    fun process(@Valid @RequestBody message: Message): Message {
        broker.rightSend(message.id, message.message)
        val body = broker.rightReceive(message.id)
        return if (body != null) {
            message.copy(message = body, date = Date())
        } else {
            message.copy(message = "TIMEOUT")
        }
    }
}
