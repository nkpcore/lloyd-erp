package com.lloyd.attendance.feature.receipts

import com.google.gson.Gson
import com.lloyd.attendance.api.ErpApiClient
import com.lloyd.attendance.api.Models
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeeReceiptsDataTest {

    private val gson = Gson()

    @Test
    fun testFormatToIndianDate_validUtcIso() {
        val raw = "2026-10-21T13:04:13.053990"
        val formatted = Models.formatToIndianDate(raw)
        // 13:04:13 UTC is 18:34:13 IST (06:34 PM)
        assertTrue("Formatted date should contain 21 Oct 2026", formatted.contains("21 Oct 2026"))
        assertTrue("Formatted date should contain 06:34 PM", formatted.contains("06:34 PM"))
    }

    @Test
    fun testFormatToIndianDate_dateOnly() {
        val raw = "2026-08-09"
        val formatted = Models.formatToIndianDate(raw)
        assertTrue("Formatted date should contain 09 Aug 2026", formatted.contains("09 Aug 2026"))
    }

    @Test
    fun testFormatToIndianDate_nullOrBlank() {
        assertEquals("", Models.formatToIndianDate(null))
        assertEquals("", Models.formatToIndianDate(""))
        assertEquals("   ", Models.formatToIndianDate("   "))
    }

    @Test
    fun testFeeReceiptItem_deserializesTxAmount() {
        val json = """
            {
                "id": 270565,
                "receipt_no": "REC-270565",
                "receipt_date": "2026-10-21T13:04:13.053990",
                "tx_amount": 75000.0,
                "fee_head": "Tuition Fee",
                "paid_log_id": 99123
            }
        """.trimIndent()

        val item = gson.fromJson(json, Models.FeeReceiptItem::class.java)
        assertNotNull(item)
        assertEquals(270565L, item.id)
        assertEquals("REC-270565", item.receiptNo)
        assertEquals(75000.0, item.amount, 0.001)
        assertEquals(99123L, item.paidLogId)
    }

    @Test
    fun testFeeReceiptItem_deserializesNetAndFeeAmountAlternates() {
        val jsonWithNetAmount = """{"id": 1, "net_amount": 42000.50}"""
        val item1 = gson.fromJson(jsonWithNetAmount, Models.FeeReceiptItem::class.java)
        assertEquals(42000.50, item1.amount, 0.001)

        val jsonWithFeeAmount = """{"id": 2, "fee_amount": 55000.0}"""
        val item2 = gson.fromJson(jsonWithFeeAmount, Models.FeeReceiptItem::class.java)
        assertEquals(55000.0, item2.amount, 0.001)

        val jsonWithPaidAmount = """{"id": 3, "paid_amount": 31500.0}"""
        val item3 = gson.fromJson(jsonWithPaidAmount, Models.FeeReceiptItem::class.java)
        assertEquals(31500.0, item3.amount, 0.001)
    }

    @Test
    fun testFeeTransactionItem_deserializesTxAmount() {
        val json = """
            {
                "id": 101,
                "transaction_id": "TXN_987654321",
                "tx_amount": 125000.0,
                "mode": "Online Gateway",
                "status": "Success"
            }
        """.trimIndent()

        val item = gson.fromJson(json, Models.FeeTransactionItem::class.java)
        assertNotNull(item)
        assertEquals("TXN_987654321", item.transactionId)
        assertEquals(125000.0, item.amount, 0.001)
        assertEquals("Success", item.status)
    }

    @Test
    fun testReceiptData_distinguishesPdfFromHtml() {
        val pdfHeaderBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34) // %PDF-1.4
        val isPdfHeader = pdfHeaderBytes.size >= 4 &&
                pdfHeaderBytes[0] == 0x25.toByte() &&
                pdfHeaderBytes[1] == 0x50.toByte() &&
                pdfHeaderBytes[2] == 0x44.toByte() &&
                pdfHeaderBytes[3] == 0x46.toByte()

        assertTrue("Header should be identified as PDF", isPdfHeader)

        val receiptData = ErpApiClient.ReceiptData(
            isPdf = true,
            pdfBytes = pdfHeaderBytes
        )
        assertTrue(receiptData.isPdf)
        assertNotNull(receiptData.pdfBytes)

        val htmlData = ErpApiClient.ReceiptData(
            isPdf = false,
            htmlContent = "<html><body>Receipt</body></html>"
        )
        assertFalse(htmlData.isPdf)
        assertEquals("<html><body>Receipt</body></html>", htmlData.htmlContent)
    }
}
