package com.simats.burnouttracker.data.models

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Contract tests against the JSON the backend ACTUALLY returns.
 *
 * These exist because of a defect that cost a full debugging session. Four DTOs
 * declared `@SerialName("_id")` — a Mongo-ism — while every Express route
 * responds with `{ id: doc.id, ...doc.data() }`. Deserializing the POST response
 * therefore threw, ApiClient caught it and returned `success = false`, and the
 * caller concluded the write had failed *while the server had already committed
 * it*. Nothing logged an error; sleep nights were re-POSTed forever and study
 * sessions were never assigned an id to stop them with.
 *
 * The payloads below are copied from the real route handlers, including the
 * nulls the backend genuinely stores (`sleepDuration ?? null` and friends).
 * Every DTO here must survive them.
 */
class ApiDtoContractTest {

    /** The same configuration ApiClient installs. */
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    // ── sleepMoodLogs ────────────────────────────────────────────────────────

    /** As written by SleepMonitoringEngine.syncSession and read back by GET /logs. */
    private val automaticSleepJson = """
        {
          "id": "knj65AANMfXR9bomcOiPBz1ozc53_2026-08-15_automatic",
          "userId": "knj65AANMfXR9bomcOiPBz1ozc53",
          "date": "2026-08-15",
          "sleepDuration": 3.1166666666666667,
          "sleepQuality": 20,
          "mood": "Auto-detected",
          "moodScore": 5,
          "notes": null,
          "sleepStart": 1786741459826,
          "sleepEnd": 1786752681759,
          "awakeningCount": 0,
          "disturbanceScore": 80,
          "source": "automatic",
          "createdAt": "2026-08-15T15:26:59.419Z",
          "updatedAt": "2026-08-16T00:10:05.000Z"
        }
    """.trimIndent()

    @Test
    fun `automatic sleep log decodes from the real backend shape`() {
        val log = json.decodeFromString<RemoteSleepLog>(automaticSleepJson)

        assertEquals("knj65AANMfXR9bomcOiPBz1ozc53_2026-08-15_automatic", log.id)
        assertEquals("2026-08-15", log.date)
        assertEquals(1786741459826L, log.sleepStart)
        assertEquals(1786752681759L, log.sleepEnd)
        assertEquals(20, log.sleepQuality)
        assertEquals(80, log.disturbanceScore)
        assertEquals("automatic", log.source)
    }

    @Test
    fun `the id field is named id and not _id`() {
        // The exact regression: a payload with `id` must populate the model. If
        // the annotation ever returns to `_id` this fails rather than silently
        // turning every write into a reported failure.
        val log = json.decodeFromString<RemoteSleepLog>("""{"id":"abc","date":"2026-08-15"}""")
        assertEquals("abc", log.id)
    }

    @Test
    fun `a mood-only manual log decodes despite null sleep fields`() {
        // POST /log persists `sleepDuration ?? null`, so this is a real document.
        val manual = """
            {
              "id": "azWrUnunRP3QVsKYYFdq",
              "userId": "knj65AANMfXR9bomcOiPBz1ozc53",
              "date": "2026-08-14",
              "sleepDuration": null,
              "sleepQuality": null,
              "mood": "stressed",
              "moodScore": 3,
              "notes": null,
              "sleepStart": null,
              "sleepEnd": null,
              "source": "manual"
            }
        """.trimIndent()

        val log = json.decodeFromString<RemoteSleepLog>(manual)

        assertNull(log.sleepDuration)
        assertNull(log.sleepStart)
        assertEquals("manual", log.source)
    }

    @Test
    fun `a logs response containing both manual and automatic records decodes whole`() {
        // One unparseable record used to fail the entire array.
        val body = """
            {
              "success": true,
              "logs": [ $automaticSleepJson,
                        {"id":"FYeh87MnvFCH7UPtmZD2","date":"2026-08-14","mood":"ok","moodScore":5} ],
              "canonical": $automaticSleepJson,
              "canonicalAvailable": true
            }
        """.trimIndent()

        val response = json.decodeFromString<SleepMoodLogsResponse>(body)

        assertEquals(2, response.logs.size)
        assertEquals("2026-08-15", response.logs[0].date)
    }

    @Test
    fun `a save response decodes so a successful write is not reported as failure`() {
        val body = """{"success": true, "log": $automaticSleepJson}"""

        val response = json.decodeFromString<SleepMoodLogResponse>(body)

        assertEquals(true, response.success)
        assertNotNull(response.log)
        assertEquals("2026-08-15", response.log?.date)
    }

    // ── studySessions ────────────────────────────────────────────────────────

    @Test
    fun `study session start response yields the id needed to stop it`() {
        // server/routes/study.js: res.json({ success: true, session: { id: doc.id, ... } })
        val body = """
            {
              "success": true,
              "session": {
                "id": "9tKq2mVrX1aBcD3eF4gH",
                "userId": "knj65AANMfXR9bomcOiPBz1ozc53",
                "subject": "Physics",
                "duration": 0,
                "startTime": "2026-08-16T01:00:00.000Z",
                "endTime": null,
                "isActive": true
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<StudySessionResponse>(body)

        assertEquals(true, response.success)
        // Without this id, stopStudySession is never called and the server row
        // stays isActive forever.
        assertEquals("9tKq2mVrX1aBcD3eF4gH", response.session?.id)
        assertEquals(true, response.session?.isActive)
    }

    @Test
    fun `study session stop response decodes with endTime populated`() {
        val body = """
            {
              "success": true,
              "session": {
                "id": "9tKq2mVrX1aBcD3eF4gH",
                "subject": "Physics",
                "duration": 45,
                "startTime": "2026-08-16T01:00:00.000Z",
                "endTime": "2026-08-16T01:45:00.000Z",
                "isActive": false
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<StudySessionResponse>(body)

        assertEquals(45, response.session?.duration)
        assertEquals(false, response.session?.isActive)
    }

    // ── burnoutAssessments ───────────────────────────────────────────────────

    @Test
    fun `burnout assessment response decodes including nested factors`() {
        val body = """
            {
              "success": true,
              "assessment": {
                "id": "assess_123",
                "userId": "knj65AANMfXR9bomcOiPBz1ozc53",
                "date": "2026-08-16",
                "riskScore": 61,
                "riskLevel": "moderate",
                "factors": [
                  {"name": "Study Hours", "score": 70},
                  {"name": "Sleep Quality", "score": 40}
                ],
                "wellbeingDimensions": {
                  "physical": 6, "emotional": 5, "social": 4,
                  "intellectual": 7, "occupational": 5
                },
                "warnings": ["Low sleep"],
                "recommendations": ["Sleep earlier"]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<BurnoutAssessmentResponse>(body)

        assertEquals(61, response.assessment?.riskScore)
        assertEquals(2, response.assessment?.factors?.size)
        assertEquals(6, response.assessment?.wellbeingDimensions?.physical)
    }

    @Test
    fun `burnout assessment decodes when optional collections are absent`() {
        // The backend stores only what was sent, so these keys can be missing.
        val body = """
            {"success": true, "assessment": {"id":"a1","date":"2026-08-16","riskScore":30}}
        """.trimIndent()

        val response = json.decodeFromString<BurnoutAssessmentResponse>(body)

        assertEquals(30, response.assessment?.riskScore)
        assertNull(response.assessment?.factors)
        assertNull(response.assessment?.warnings)
    }
}
