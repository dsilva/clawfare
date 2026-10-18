package com.clawfare

import com.clawfare.web.clawfareModule
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerTest {
    @Test
    fun sampleAndStatusStayOnTheServer() = testApplication {
        application { clawfareModule() }
        val sample = client.get("/api/sample")
        assertEquals(HttpStatusCode.OK, sample.status)
        val body = sample.bodyAsText()
        assertTrue(body.contains("BA 211"))
        assertTrue(body.contains("3341"))
        assertFalse(body.contains("api_key"))
        assertFalse(body.contains("SERPAPI"))

        val status = client.get("/api/status")
        assertEquals(HttpStatusCode.OK, status.status)
        val statusBody = status.bodyAsText()
        assertTrue(statusBody.contains("serpapi"))
        assertTrue(statusBody.contains("duffel"))
        assertFalse(statusBody.contains("api_key="))

        val page = client.get("/")
        assertEquals(HttpStatusCode.OK, page.status)
        assertTrue(page.bodyAsText().contains("window.__SNAPSHOT__ = null"))

        val snapshot = client.get("/api/sample/export.html")
        assertEquals(HttpStatusCode.OK, snapshot.status)
        val html = snapshot.bodyAsText()
        assertTrue(html.contains("BA 211"))
        assertTrue(html.contains("BudgetAir"))
        assertFalse(html.contains("window.__SNAPSHOT__ = null"))
        assertFalse(html.contains("api_key"))
    }
}
