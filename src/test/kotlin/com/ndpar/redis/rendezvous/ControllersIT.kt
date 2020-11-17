package com.ndpar.redis.rendezvous

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.*
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE as JSON

@SpringBootTest
class ControllersIT(
    @Autowired private val wac: WebApplicationContext
) {
    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setupMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build()
    }

    @Test
    fun `alice starts first bob second`() = runBlocking {
        val id = UUID.randomUUID().toString()

        val alice = async { alice(id) }
        val bob = async { bob(id) }

        val a = alice.await()
        val b = bob.await()

        assertEquals(id, a.id)
        assertEquals(id, b.id)

        assertEquals("hello from /bob", a.message)
        assertEquals("hello from /alice", b.message)
    }

    @Test
    fun `bob starts first alice second`() = runBlocking {
        val id = UUID.randomUUID().toString()

        val bob = async { bob(id) }
        val alice = async { alice(id) }

        val a = alice.await()
        val b = bob.await()

        assertEquals(id, a.id)
        assertEquals(id, b.id)

        assertEquals("hello from /bob", a.message)
        assertEquals("hello from /alice", b.message)
    }

    @Test
    @Disabled("Timeout 20 seconds")
    fun `alice times out when bob does not come`() = runBlocking {
        val alice = withContext(Dispatchers.IO) { alice("alice-only") }
        assertEquals("TIMEOUT", alice.message)
    }

    @Test
    @Disabled("Timeout 20 seconds")
    fun `bob times out when alice does not come`() = runBlocking {
        val bob = withContext(Dispatchers.IO) { bob("bob-only") }
        assertEquals("TIMEOUT", bob.message)
    }

    private suspend fun alice(id: String) = post("/alice", id)

    private suspend fun bob(id: String) = post("/bob", id)

    private suspend fun post(endpoint: String, id: String) = withContext(Dispatchers.IO) {
        mockMvc.perform(
            post(endpoint)
                .contentType(JSON)
                .content(Message(id = id, message = "hello from $endpoint").toJson())
        )
            .andExpect(status().isOk)
            .andReturn().response.getContent<Message>()
    }

    private fun Any.toJson(): String = ObjectMapper().writeValueAsString(this)

    private inline fun <reified T> MockHttpServletResponse.getContent(): T =
        ObjectMapper().readValue(contentAsString)
}
