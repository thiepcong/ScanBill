package com.littletrickster.scanner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.Normalizer

object ConvertToBillInfo {
    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }


    fun imageToBillInfo(
        bitmap: Bitmap,
        rotation: Int,
        onSuccess: (BillInfo) -> Unit,
        onError: (Exception) -> Unit
    ) {
        imageToText(
            bitmap,
            rotation,
            onSuccess = {
                val billInfo = extractPaymentInfo(it)
                onSuccess.invoke(billInfo)
            },
            onError
        )
    }

    fun imagePathToBillInfo(
        context: Context,
        file: File?,
        onSuccess: (BillInfo) -> Unit,
        onError: (Exception) -> Unit
    ) {
        imagePathToText(
            context,
            file,
            onSuccess = {
                val billInfo = extractPaymentInfo(it)
                onSuccess.invoke(billInfo)
            },
            onError
        )
    }

    private fun imageToText(
        bitmap: Bitmap,
        rotation: Int,
        onSuccess: (Text) -> Unit,
        onError: (Exception) -> Unit
    ) {
        bitmap.let {
            val image: InputImage
            try {
                Log.d(
                    "TAG",
                    "imageToTextInput, currentTimeMillis: ${System.currentTimeMillis()}"
                )
                image = InputImage.fromBitmap(bitmap, rotation)
                textRecognizer.process(image).addOnSuccessListener {
                    Log.d("TAG", "imagePathToTextSuccess, output: ${it.text}")

                    Log.d(
                        "TAG",
                        "imagePathToTextSuccess, currentTimeMillis: ${System.currentTimeMillis()}"
                    )
                    onSuccess.invoke(it)
                }.addOnFailureListener {
                    onError.invoke(it)
                    Log.d(
                        "TAG",
                        "imagePathToTextError, currentTimeMillis: ${System.currentTimeMillis()}"
                    )
                }
            } catch (e: IOException) {
                onError.invoke(e)
            }
        }
    }

    private fun imagePathToText(
        context: Context,
        file: File?,
        onSuccess: (Text) -> Unit,
        onError: (Exception) -> Unit
    ) {
        file?.let {
            val image: InputImage
            try {
                Log.d(
                    "TAG",
                    "imagePathToTextInput, currentTimeMillis: ${System.currentTimeMillis()}"
                )
                image = InputImage.fromFilePath(context, Uri.fromFile(file))
                textRecognizer.process(image).addOnSuccessListener {
                    Log.d("TAG", "imagePathToTextSuccess, output: ${it.text}")

                    Log.d(
                        "TAG",
                        "imagePathToTextSuccess, currentTimeMillis: ${System.currentTimeMillis()}"
                    )
                    onSuccess.invoke(it)
                }.addOnFailureListener {
                    onError.invoke(it)
                    Log.d(
                        "TAG",
                        "imagePathToTextError, currentTimeMillis: ${System.currentTimeMillis()}"
                    )
                }
            } catch (e: IOException) {
                onError.invoke(e)
            }
        } ?: onError.invoke(Exception("File is null"))
    }

    private fun extractPaymentInfo(text: Text): BillInfo {
        fun normalize(input: String): String {
            val accentsMap = mapOf(
                "[\u00E0-\u00E3\u00E2\u1EA7-\u1EAB\u1EB1-\u1EB7]" to "a",
                "[\u00E8-\u00EB\u1EC1-\u1EC5\u1EC7]" to "e",
                "[\u00EC-\u00EF]" to "i",
                "[\u00F2-\u00F5\u00F4\u1ED3-\u1ED7\u1ED9-\u1EDD]" to "o",
                "[\u00F9-\u00FC\u1EE7-\u1EEB]" to "u",
                "[\u1EF3-\u1EF9]" to "y",
                "[\u0111]" to "d"
            )
            var normalizedText = input
            for ((pattern, replacement) in accentsMap) {
                normalizedText =
                    normalizedText.replace(Regex(pattern, RegexOption.IGNORE_CASE), replacement)
            }
            return normalizedText
        }

        // Sử dụng Normalizer để loại bỏ dấu
        fun removeAccents(input: String): String {
            val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            return normalized.replace(Regex("[^\\p{ASCII}]"), "")
        }

        val normalizedInput = normalize(text.text)
        val removeAccentsInput = removeAccents(text.text)


        fun findBankName(): String {
            if ((removeAccents(
                    normalizedInput
                ).split("\n").firstOrNull() ?: "").contains(Regex("mb\$", RegexOption.IGNORE_CASE))
            ) {
                return "MB"
            }
            val bankNames = listOf(
                // Ngân hàng thương mại nhà nước
                "Vietcombank",         // Ngân hàng TMCP Ngoại thương Việt Nam
                "VietinBank",          // Ngân hàng TMCP Công Thương Việt Nam
                "Agribank",            // Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam
                "BIDV",                // Ngân hàng TMCP Đầu tư và Phát triển Việt Nam

                // Ngân hàng TMCP tư nhân
                "Techcombank",         // Ngân hàng TMCP Kỹ Thương Việt Nam
                "MBBank",             // Ngân hàng TMCP Quân Đội
                "VPBank",              // Ngân hàng TMCP Việt Nam Thịnh Vượng
                "Sacombank",           // Ngân hàng TMCP Sài Gòn Thương Tín
                "ACB",                 // Ngân hàng TMCP Á Châu
                "HDBank",              // Ngân hàng TMCP Phát triển Nhà TP.HCM
                "VIB",                 // Ngân hàng TMCP Quốc tế Việt Nam
                "SHB",                 // Ngân hàng TMCP Sài Gòn - Hà Nội
                "TPBank",              // Ngân hàng TMCP Tiên Phong
                "SeABank",             // Ngân hàng TMCP Đông Nam Á
                "OCB",                 // Ngân hàng TMCP Phương Đông
                "SCB",                 // Ngân hàng TMCP Sài Gòn
                "Eximbank",            // Ngân hàng TMCP Xuất Nhập Khẩu Việt Nam
                "LienVietPostBank",    // Ngân hàng TMCP Bưu điện Liên Việt
                "BacABank",          // Ngân hàng TMCP Bắc Á
                "DongABank",          // Ngân hàng TMCP Đông Á
                "BaoVietBank",         // Ngân hàng TMCP Bảo Việt
                "NamABank",          // Ngân hàng TMCP Nam Á
                "PVcomBank",           // Ngân hàng TMCP Đại Chúng Việt Nam
                "VietABank",           // Ngân hàng TMCP Việt Á
                "Saigonbank",          // Ngân hàng TMCP Sài Gòn Công Thương
                "PGBank",             // Ngân hàng TMCP Xăng dầu Petrolimex
                "CBBank",              // Ngân hàng Xây Dựng (CB Bank)
                "OceanBank",           // Ngân hàng TMCP Đại Dương (OceanBank)
                "GPBank",              // Ngân hàng Dầu Khí Toàn Cầu (GPBank)

                // Ngân hàng chính sách và ngân hàng nhà nước
                "NHCSXH",              // Ngân hàng Chính sách Xã hội Việt Nam
                "NHPT",                // Ngân hàng Phát triển Việt Nam (VDB)

                // Ngân hàng nước ngoài hoạt động tại Việt Nam
                "StandardChartered",  // Ngân hàng Standard Chartered
                "HSBC",                // Ngân hàng HSBC
                "Citibank",            // Ngân hàng Citibank
                "ANZ",                 // Ngân hàng ANZ
                "UOB",                 // Ngân hàng UOB
                "ShinhanBank",        // Ngân hàng Shinhan Bank
                "WooriBank",          // Ngân hàng Woori Bank
                "PublicBank",         // Ngân hàng Public Bank Berhad
                "HongLeongBank",     // Ngân hàng Hong Leong
                "BangkokBank",        // Ngân hàng Bangkok Bank
                "Maybank",             // Ngân hàng Maybank
                "DBSBank",            // Ngân hàng DBS Bank
                "MUFGBank",           // Ngân hàng MUFG Bank (Mitsubishi UFJ)
                "SumitomoMitsuiBank",// Ngân hàng Sumitomo Mitsui Banking Corporation (SMBC)
                "MizuhoBank"          // Ngân hàng Mizuho Bank
            )

            return removeAccents(normalizedInput)
                .lines()
                .firstNotNullOfOrNull { line ->
                    bankNames.firstOrNull { bankName ->
                        Regex(
                            ".*${Regex.escape(bankName)}.*",
                            RegexOption.IGNORE_CASE
                        ).containsMatchIn(line)
                    }
                } ?: removeAccents(normalizedInput)
                .lines()
                .firstOrNull {
                    Regex("MB\\s*$", RegexOption.IGNORE_CASE).containsMatchIn(it)
                }?.replace(Regex("[^mb]*", RegexOption.IGNORE_CASE), "") ?: ""
        }

        val bankName = findBankName()

        fun findTID(): String {
            var tidLine: Text.Line? = null
            var closestOy: Float? = null

            val pattern = Regex(
                "(?<!(pos|ref)\\s)(tid\\s*|mid\\s*|id:\\s*|ti0\\s*|tio\\s*)",
                RegexOption.IGNORE_CASE
            )
            outerLoop2@ for (textBlock in text.textBlocks) {
                for (line in textBlock.lines) {
                    if (line.text.contains(pattern)) {

                        val (_, y) = line.boundingBox?.let { it.exactCenterX() to it.exactCenterY() }
                            ?: (null to null)
                        closestOy = y
                        tidLine = line
                        break@outerLoop2
                    }
                }
            }

            if (tidLine != null) {
                val regex = Regex("(\\w*\\s*\\w+)\\s*mid(?!.*tid)", RegexOption.IGNORE_CASE)
                val matchResult = regex.find(tidLine.text)

                if (matchResult != null && matchResult.groupValues.getOrNull(1) != null && !bankName.contains(
                        Regex(
                            "mb\$",
                            RegexOption.IGNORE_CASE
                        )
                    )
                ) {
                    return matchResult.groupValues[1]
                }

                val find = tidLine.text.split(":").getOrNull(1)
                if (find != null && find.trim().isNotEmpty()) {
                    if (bankName.contains(
                            Regex(
                                "mb\$",
                                RegexOption.IGNORE_CASE
                            )
                        ) && find.contains(Regex("\\d"))
                    ) {
                        return find.replace(Regex("seq.*", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("[^a-zA-Z0-9]"), "")
                    }
                    if (find.trim()
                            .contains(Regex("\\b[\\w\\d]{6,}-(\\w{6,})\\b|\\b[\\w\\d]{6,}/(\\w{6,})\\b"))
                    ) {
                        return find.split("-").getOrNull(1) ?: find.split("/")
                            .getOrNull(1) ?: ""
                    }
                    if (find.contains(Regex("\\d"))) return find.replace(
                        Regex(
                            "(mid|mio).*",
                            RegexOption.IGNORE_CASE
                        ), ""
                    ).trim()
                }
                val nearestLines = text.textBlocks
                    .asSequence()
                    .flatMap { it.lines }
                    .filterNot { it.text == tidLine.text }
                    .map { line ->
                        val (_, y) = line.boundingBox?.let { i -> i.exactCenterX() to i.exactCenterY() }
                            ?: (Float.MAX_VALUE to Float.MAX_VALUE)
                        val yDiff =
                            if (closestOy != null) kotlin.math.abs(y - closestOy) else Float.MAX_VALUE
                        line to yDiff // Trả về một cặp (line, yDiff)
                    }
                    .sortedBy { it.second } // Sắp xếp theo yDiff
                    .take(3) // Lấy 3 phần tử có yDiff ngắn nhất
                    .map { it.first }
                    .toList()
                if (bankName.contains("eximbank", true) && nearestLines.isNotEmpty()) {
                    val result0 = nearestLines.firstOrNull {
                        it.text.contains(Regex("(mid|mio).*", RegexOption.IGNORE_CASE))
                    }?.text?.replace(Regex("(mid|mio).*", RegexOption.IGNORE_CASE), "")
                        ?.replace(Regex("[()oO]"), "0")
                        ?.split(Regex("[^\\d]"))?.firstOrNull()
                    if (!result0.isNullOrEmpty()) {
                        return result0
                    }
                    val result1 = nearestLines.firstOrNull {
                        it.text.matches(Regex("\\d+"))
                    }?.text?.split(Regex("[^\\d]"))?.firstOrNull()
                    if (!result1.isNullOrEmpty()) {
                        return result1
                    }
                    val result = nearestLines.firstOrNull()?.text?.replace(Regex("[()oO]"), "0")
                        ?.split(Regex("[^\\d]"))?.firstOrNull()
                    if (result != null) {
                        return result
                    }
                }
                nearestLines.forEach { line ->
                    if (line.text.contains(Regex("\\b[\\w\\d]{6,}-(\\w{6,})\\b|\\b[\\w\\d]{6,}/(\\w{6,})\\b"))) {
                        return line.text.split("-").getOrNull(1) ?: line.text.split("/")
                            .getOrNull(1) ?: ""
                    }
                }
            }

            val match = Regex(
                "MID-TID:\\s*\\w+-([\\w\\d]+)|\\b[\\w\\d]{6,}-(\\w{6,})\\b|MID/TID:\\s*\\w+/([\\w\\d]+)|\\b[\\w\\d]{6,}/(\\w{6,})\\b",
                RegexOption.IGNORE_CASE
            ).find(text.text)
            return match?.groups?.get(1)?.value ?: match?.groups?.get(2)?.value
            ?: match?.groups?.get(3)?.value ?: match?.groups?.get(4)?.value ?: ""
        }

        fun findCustomerName(): String {
            val potentialNames = removeAccentsInput.split("\n")
                .filter { it.matches(Regex("^[A-Z 0/:1]{2,}")) }
                .map { it.trim() }
                .filterNot { name ->
                    listOf(
                        "AI LY",
                        "DIA CHI",
                        "SIMART",
                        "MID",
                        "TID",
                        "MASTERCARD",
                        "SALE",
                        "TOTAL",
                        "VNPAY",
                        "TC",
                        "APP",
                        "TAGREE",
                        "SIGNATURE",
                        "ACCORDING",
                        "REFUND",
                        "VERSION",
                        "EMAIL",
                        "DATE",
                        "AID",
                        "TAGREE",
                        "APPV CODE",
                        "NOT",
                        "REQUIRE",
                        "HOA DON",
                        "KHONG HOAN TIEN",
                        "HOME FARM",
                        "THANH TOAN",
                        "CHU KY",
                        "CARDHOLDER",
                        "VISA", "UISA",
                        "NGAY",
                        "GIO",
                        "HOA PON", "RELATIVE", "NAME",
                        "GAS", "NAY LA", "NINH BINH", "HA NOI", "SO LO",
                        "CARD", "F", "J", "W", "Z", "TONG CONG", "HOAN TRA", "CHU THE"
                    ).any { blacklist -> name.contains(blacklist) } || name.split(Regex("[^\\w]")).size < 2
                }
                .toList()
            if (bankName.contains("eximbank", true)) {
                var result =
                    potentialNames.firstOrNull {
                        it.contains(
                            Regex(
                                "^(ten\\s*:?\\s*|tin\\s*:?\\s*|name\\s*:?\\s*|en\\s*:?|1en\\s*:?)",
                                RegexOption.IGNORE_CASE
                            )
                        )
                    }
                        ?.replace(
                            Regex(
                                "^(ten\\s*:?\\s*|tin\\s*:?\\s*|name\\s*:?\\s*|en\\s*:?|1en\\s*:?)",
                                RegexOption.IGNORE_CASE
                            ), ""
                        )?.trim()
                if (result != null) {
                    val parts = result.split("/").map { it.trim() }
                    if (parts.size == 2) {
                        result = "${parts[1]} ${parts[0]}"
                    }
                    return result
                }
            }
            val likelyNames = potentialNames.filter {
                it.split(Regex("\\s+|/")).filter { i -> i.isNotEmpty() }.size in 2..4
            }
                .sortedByDescending { it.split(Regex("\\s+|/")).size == 3 }
            var name =
                likelyNames.firstOrNull { it.contains("/") } ?: likelyNames.firstOrNull() ?: ""

            if (name.contains("/")) {
                val parts = name.split("/").map { it.trim() }
                if (parts.size == 2) {
                    name = "${parts[1]} ${parts[0]}"
                }
            }
            return name
        }

        fun findLast4DigitsOfCard(): String {
            return removeAccentsInput.split(Regex("\r?\n"))
                .filter {
                    it.length <= 20 ||
                            it.contains(
                                Regex(
                                    "\\((chip|c)\\)|chip|c\\)|\\(c|c]|\\[c",
                                    RegexOption.IGNORE_CASE
                                )
                            )
                }
                .firstNotNullOfOrNull { line ->
                    val trimmedLine = line.trim()
                    Regex(".{6,}\\*\\*(\\w{4})\\b").find(trimmedLine)?.groupValues?.get(1)
                        ?.replace(Regex("[sS]"), "5")
                        ?.replace(Regex("B"), "8")
                        ?.replace(Regex("[oO()]"), "0")
                        ?.replace(Regex("[^\\d]"), "")
                        ?: Regex(
                            "\\d{4}(?=\\s*(\\((chip|c)\\)|chip|c\\)|\\(c|c]|\\[c))",
                            RegexOption.IGNORE_CASE
                        )
                            .find(trimmedLine)?.value
                            ?.replace(Regex("[^\\d]"), "")
                        ?: Regex("(?<![0-9\\-/])\\d{4}\$").find(trimmedLine)?.value
                } ?: ""
        }

        fun findBatchNumber(): String {
            var closestLine: Text.Line? = null
            var closestY: Float? = null

            val pattern = Regex(
                "(batch\\s*|[Ll][ou]?:\\s*|s[uo] l[ou])",
                RegexOption.IGNORE_CASE
            )
            outerLoop@ for (textBlock in text.textBlocks) {

                for (line in textBlock.lines) {
                    if (removeAccents(line.text).contains(pattern)) {

                        val (_, y) = line.boundingBox?.let { it.exactCenterX() to it.exactCenterY() }
                            ?: (null to null)

                        closestY = y
                        closestLine = line
                        break@outerLoop
                    }
                }
            }

            if (closestLine != null) {
                val find = Regex("[0-9]+").find(
                    if (closestLine.text.contains(Regex("[.:]"))) closestLine.text.split(Regex("[.:]"))
                        .getOrNull(1) ?: "" else closestLine.text
                )
                if (find != null) return find.value

                val nearestLines = text.textBlocks
                    .asSequence()
                    .flatMap { it.lines }
                    .filterNot { it.text == closestLine.text }
                    .map { line ->
                        val (_, y) = line.boundingBox?.let { i -> i.exactCenterX() to i.exactCenterY() }
                            ?: (Float.MAX_VALUE to Float.MAX_VALUE)
                        val yDiff =
                            if (closestY != null) kotlin.math.abs(y - closestY) else Float.MAX_VALUE
                        line to yDiff // Trả về một cặp (line, yDiff)
                    }
                    .sortedBy { it.second } // Sắp xếp theo yDiff
                    .take(3) // Lấy 3 phần tử có yDiff ngắn nhất
                    .map { it.first }
                    .toList()
                if (bankName.contains("eximbank", true) && nearestLines.isNotEmpty()) {
                    val result0 = nearestLines.firstOrNull {
                        it.text.contains(
                            Regex(
                                "(hoa don\\s*:|invoice\\s*|trace\\s*|h[.\\s]\\s?d?[od]n)",
                                RegexOption.IGNORE_CASE
                            )
                        )
                    }?.text?.replace(Regex("[^\\d]"), "")
                    if (!result0.isNullOrEmpty()) {
                        return result0
                    }
                    val result = nearestLines.firstOrNull()?.text?.replace(Regex("[^\\d]"), "")
                    if (!result.isNullOrEmpty()) {
                        return result
                    }
                }
                nearestLines.forEach { line ->
                    if (line.text.matches(Regex("^[0-9 O()oS]+\$"))) {
                        return line.text.replace(Regex("[()oO]"), "0").replace("S", "5")
                    }
                }
            }
            val match =
                Regex("(Batch|L\u00F4)[:\\s]*([0-9]+)", RegexOption.IGNORE_CASE).find(text.text)
            if (match != null) return match.groups[2]?.value ?: ""

            val shortestNumber = Regex("^[0-9 OS]+$", RegexOption.MULTILINE)
                .findAll(text.text)
                .map { it.value.replace(Regex("[()oO]"), "0").replace("S", "5") }
                .minByOrNull { it.length }
            return shortestNumber ?: ""
        }

        val batchNumber = findBatchNumber()

        fun findInvoiceNo(): String {
            var closestLine3: Text.Line? = null
            var closestY3: Float? = null

            val pattern =
                Regex(
                    "(hoa don\\s*:|invoice\\s*|trace\\s*|h[.\\s]\\s?d?[odu][mn])",
                    RegexOption.IGNORE_CASE
                )
            outerLoop@ for (textBlock in text.textBlocks) {

                for (line in textBlock.lines) {
                    if (removeAccents(line.text).contains(pattern)) {

                        val (_, y) = line.boundingBox?.let { it.exactCenterX() to it.exactCenterY() }
                            ?: (null to null)
                        closestY3 = y
                        closestLine3 = line
                        break@outerLoop
                    }
                }
            }

            if (closestLine3 != null) {
                val find = Regex("[0-9]+").find(closestLine3.text.split(":").getOrNull(1) ?: "")
                if (find != null) return find.value
                val nearestLines = text.textBlocks
                    .asSequence()
                    .flatMap { it.lines }
                    .filterNot {
                        it.text == closestLine3.text || it.text.contains(batchNumber) || removeAccents(
                            it.text
                        ).contains(
                            Regex("(batch\\s*|[Ll]o?:\\s*|so)", RegexOption.IGNORE_CASE)
                        )
                    }
                    .map { line ->
                        val (_, y) = line.boundingBox?.let { i -> i.exactCenterX() to i.exactCenterY() }
                            ?: (Float.MAX_VALUE to Float.MAX_VALUE)
                        val yDiff =
                            if (closestY3 != null) kotlin.math.abs(y - closestY3) else Float.MAX_VALUE
                        line to yDiff // Trả về một cặp (line, yDiff)
                    }
                    .sortedBy { it.second } // Sắp xếp theo yDiff
                    .take(5) // Lấy 5 phần tử có yDiff ngắn nhất
                    .map { it.first }
                    .toList()

                nearestLines.forEach { line ->
                    if (line.text.matches(Regex("^[0-9 O()oS]+\$"))) {
                        return line.text.replace(Regex("[()oO]"), "0").replace("S", "5")
                    }
                }
            }
            val match =
                Regex(
                    "(hoa don\\s*:|invoice\\s*|trace\\s*|No\\.?/s|h[.,\\s]d?[od]n)[:\\s]*([0-9]+)",
                    RegexOption.IGNORE_CASE
                ).find(text.text)
            if (match != null) return match.groups[2]?.value ?: ""

            val shortestNumber = Regex("^[0-9 OS]+$", RegexOption.MULTILINE)
                .findAll(text.text)
                .map { it.value.replace(Regex("[()oO]"), "0").replace("S", "5") }
                .minByOrNull { it.length }
            return shortestNumber ?: ""
        }

        fun findPosID(): String? {
            var closestLine4: Text.Line? = null
            var closestY4: Float? = null

            val pattern = Regex(
                "(Pos\\s*)",
                RegexOption.IGNORE_CASE
            )
            outerLoop@ for (textBlock in text.textBlocks) {

                for (line in textBlock.lines) {
                    if (line.text.contains(pattern)) {

                        val (_, y) = line.boundingBox?.let { it.exactCenterX() to it.exactCenterY() }
                            ?: (null to null)
                        closestY4 = y
                        closestLine4 = line
                        break@outerLoop
                    }
                }
            }

            if (closestLine4 != null) {
                val find =
                    closestLine4.text.trim().split(Regex("[^a-z0-9]+", RegexOption.IGNORE_CASE))
                        .lastOrNull()
                if (find != null) return find
                val nearestLines = text.textBlocks
                    .asSequence()
                    .flatMap { it.lines }
                    .filterNot { it.text == closestLine4.text }
                    .map { line ->
                        val (_, y) = line.boundingBox?.let { i -> i.exactCenterX() to i.exactCenterY() }
                            ?: (Float.MAX_VALUE to Float.MAX_VALUE)
                        val yDiff =
                            if (closestY4 != null) kotlin.math.abs(y - closestY4) else Float.MAX_VALUE
                        line to yDiff // Trả về một cặp (line, yDiff)
                    }
                    .sortedBy { it.second } // Sắp xếp theo yDiff
                    .take(3) // Lấy 3 phần tử có yDiff ngắn nhất
                    .map { it.first }
                    .toList()

                nearestLines.forEach { line ->
                    if (line.text.matches(Regex("^[0-9a-z]+\$", RegexOption.IGNORE_CASE))) {
                        return line.text
                    }
                }
            }
            return null
        }

        fun findCardType(): String? {
            val cardPatterns = mapOf(
                "Visa" to Regex("[vu]isa", RegexOption.IGNORE_CASE),
                "MasterCard" to Regex("master", RegexOption.IGNORE_CASE),
                "JCB" to Regex("jcb", RegexOption.IGNORE_CASE),
                "American Express" to Regex("amex", RegexOption.IGNORE_CASE),
                "Discover" to Regex("discover", RegexOption.IGNORE_CASE),
                "Diners Club" to Regex("diners", RegexOption.IGNORE_CASE)
            )

            for ((cardType, pattern) in cardPatterns) {
                if (pattern.containsMatchIn(removeAccentsInput)) {
                    return cardType
                }
            }
            return null
        }

        fun findTotalAmount(): String {
            val priorityKeywords = listOf(
                "VND",
                "UND",
                "YND",
                "VHD",
                "UMD",
                "VMD",
                "00d",
                "00 d",
                "00g",
                "00 g",
            )
            val cleanedInput = removeAccentsInput
                .replace(Regex("(VNO|UND|YND|VHD|UMD|VMD)"), "VND")
                .replace(Regex("(?<=\\d)[()oO]|[()oO](?=\\d)"), "0")
                .replace(Regex("[.,]000[048]"), ".000d")

            val match = priorityKeywords
                .firstNotNullOfOrNull { keyword ->
                    Regex(".*$keyword.*", RegexOption.IGNORE_CASE).find(cleanedInput)
                }

            if (match != null) {
                val amount = Regex("\\d{1,3}(\\s?[.,]\\s?\\d{3})*|\\d+").find(match.value)?.value
                return amount?.replace(Regex("[^\\d]"), "") ?: ""
            }
            val amountMatch = Regex("\\b\\d{1,3}([.,]\\d{3})+\\b").find(cleanedInput)
            if (amountMatch != null) {
                return amountMatch.value.replace(Regex("[^\\d]"), "")
            }
            return ""
        }


        return BillInfo(
            tid = findTID(),
            customerName = findCustomerName(),
            last4DigitsOfCard = findLast4DigitsOfCard(),
            bankName = bankName,
            invoiceNo = findInvoiceNo(),
            batchNumber = batchNumber,
            cardType = findCardType(),
            posId = findPosID(),
            totalAmount = findTotalAmount()
        )
    }
}