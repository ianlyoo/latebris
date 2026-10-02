package com.example.gpstick.data.preset

import com.example.gpstick.core.network.JsonHttpClient
import com.example.gpstick.core.network.StubHttpURLConnection
import java.net.URL
import java.net.URLDecoder
import org.junit.Assert.*
import org.junit.Test

class OpenCellIdClientTest {
    private val preset = LocationPreset(
        id = "fixture", name = "Fixture", summary = "Synthetic",
        gps = GpsPreset(37.0, 127.0, 30.0, 5f),
        cellTowers = listOf(CellTower(1, 2, 3, 4, 5, 6)),
    )

    @Test
    fun missingKeyKeepsPresetWithoutOpeningConnection() {
        val client = OpenCellIdClient(JsonHttpClient(openConnection = { error("Unexpected request") }), { " " })
        assertSame(preset, client.autoFill(preset))
    }

    @Test
    fun mapsCellsWithLegacyAreaCodesDefaultsAndDeduplication() {
        val connection = StubHttpURLConnection(body = """{"cells":[
            {"cellid":10,"mcc":450,"mnc":5,"lac":8},
            {"cellid":10,"mcc":450,"mnc":5,"lac":8},
            {"cellid":11,"lac":8,"tac":9,"pci":7,"earfcn":1800},
            {"mcc":450}
        ]}""")
        var requestedUrl: URL? = null
        val client = OpenCellIdClient(JsonHttpClient(openConnection = { requestedUrl = it; connection }), { "fixture&key" })

        val result = client.autoFill(preset)

        assertEquals(preset.copy(cellTowers = listOf(
            CellTower(450, 5, 10, 0, 8, 0),
            CellTower(0, 0, 11, 7, 9, 1800),
        )), result)
        val query = requestedUrl!!.query.split('&').associate {
            val (name, value) = it.split('=', limit = 2)
            name to URLDecoder.decode(value, "UTF-8")
        }
        assertEquals("fixture&key", query["key"])
        assertEquals("50", query["limit"])
        assertEquals("json", query["format"])
        val bounds = query.getValue("BBOX").split(',').map(String::toDouble)
        assertTrue(bounds[0] < 37.0 && bounds[2] > 37.0)
        assertTrue(bounds[1] < 127.0 && bounds[3] > 127.0)
        assertTrue(connection.disconnected)
    }

    @Test
    fun rejectedOrMalformedResponsesKeepExistingCellData() {
        for (connection in listOf(StubHttpURLConnection(status = 503), StubHttpURLConnection(body = "{invalid"))) {
            val client = OpenCellIdClient(JsonHttpClient(openConnection = { connection }), { "fixture" })
            assertSame(preset, client.autoFill(preset))
            assertTrue(connection.disconnected)
        }
    }

    @Test
    fun successfulEmptyAreaClearsOldCells() {
        val connection = StubHttpURLConnection(body = """{"cells":null}""")
        val client = OpenCellIdClient(JsonHttpClient(openConnection = { connection }), { "fixture" })
        assertEquals(preset.copy(cellTowers = emptyList()), client.autoFill(preset))
    }
}
