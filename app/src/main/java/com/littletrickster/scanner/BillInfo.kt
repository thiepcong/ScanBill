package com.littletrickster.scanner

data class BillInfo(
    val imagePath: String? = null,
    val tid: String? = null,
    val customerName: String? = null,
    val last4DigitsOfCard: String? = null,
    val bankName: String? = null,
    val batchNumber: String? = null,
    val cardType: String? = null,
    val posId: String? = null,
    val invoiceNo: String? = null,
    val totalAmount: String? = null
) {
    override fun toString(): String {
        return """
BillInfo(
imagePath:$imagePath,
tid=$tid,
customerName=$customerName,
last4DigitsOfCard=$last4DigitsOfCard,
bankName=$bankName,
batchNumber=$batchNumber,
cardType=$cardType,
posId=$posId,
invoiceNo=$invoiceNo,
totalAmount=$totalAmount)
        """.trimIndent()
    }

    fun toMap(): Map<String, String?> {
        return mapOf(
            "imagePath" to imagePath,
            "tid" to tid,
            "customerName" to customerName,
            "last4DigitsOfCard" to last4DigitsOfCard,
            "bankName" to bankName,
            "batchNumber" to batchNumber,
            "cardType" to cardType,
            "posId" to posId,
            "invoiceNo" to invoiceNo,
            "totalAmount" to totalAmount
        )
    }
}