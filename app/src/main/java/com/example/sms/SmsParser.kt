package com.example.sms

import java.util.regex.Pattern

data class ParsedSmsResult(
    val isBankTransaction: Boolean,
    val amount: Double = 0.0,
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val merchant: String = "Merchant",
    val category: String = "Other",
    val accountOrBank: String = "Bank Account",
    val referenceId: String = "", // Extracted UTR, UPI Ref, Bank Txn ID, or Ref Number
    val ignoreReason: String = ""
)

object SmsParser {

    private val OTP_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(otp|one time password|verification code|secret code)\\b"),
        Pattern.compile("(?i)\\b(do not share|valid for|is your code|login code)\\b")
    )

    private val TRANSACTION_KEYWORDS = listOf(
        "debited", "credited", "spent", "paid", "withdrawn", "transferred",
        "received", "deposited", "refund", "cashback", "salary", "inr", "rs.", "₹", "txn", "utr"
    )

    private val AMOUNT_REGEX = Pattern.compile(
        "(?i)(?:inr|rs\\.?|₹)\\s*([0-9]{1,3}(?:,[0-9]{2,3})+(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"
    )

    private val ALT_AMOUNT_REGEX = Pattern.compile(
        "([0-9]{1,3}(?:,[0-9]{2,3})+(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:inr|rs\\.?|debited|credited)"
    )

    private val ACCOUNT_REGEX = Pattern.compile(
        "(?i)(?:a/c|acct|account|card)(?:\\s+no\\.?)?\\s*(?:ending)?\\s*([xX*]*[0-9]{3,4})"
    )

    // Priority 1: UTR / UPI Transaction ID
    private val UTR_UPI_PATTERNS = listOf(
        // UTR: ABC123456, UTR no. ABC123456, UTR-ABC123456, UTR ABC123456
        Pattern.compile("(?i)\\butr(?:\\s*(?:id|no|num|number|#)?\\.?)?\\s*[:/=\\s-]+\\s*([A-Za-z0-9]{6,35})\\b"),
        // UPI Ref: 123456, UPI txn id: 123456, UPI ref no 123456, UPI ID / txn: 123456
        Pattern.compile("(?i)\\bupi\\s*(?:ref(?:erence)?|txn|transaction)?(?:\\s*(?:id|no|num|number|#)?\\.?)?\\s*[:/=-]+\\s*([A-Za-z0-9]{6,35})\\b"),
        Pattern.compile("(?i)\\bupi\\s+(?:ref(?:erence)?|txn|transaction)(?:\\s*(?:id|no|num|number|#)?\\.?)?\\s*[:/=\\s-]*([A-Za-z0-9]{6,35})\\b"),
        // UPI/CR/123456789012 or UPI/123456789012
        Pattern.compile("(?i)\\bupi/(?:[A-Za-z0-9]{2}/)?([A-Za-z0-9]{6,35})\\b")
    )

    // Priority 2: Bank Transaction ID
    private val BANK_TXN_ID_PATTERNS = listOf(
        // Bank Txn ID: 123456, Txn ID: 123456, Txn No: 123456, Transaction ID: 123456
        Pattern.compile("(?i)\\b(?:bank\\s*txn|bank\\s*transaction|txn|transaction)\\s*(?:(?:id|no|num|number|#)\\.?\\s*[:/=\\s-]*|[:/=-]+\\s*)([A-Za-z0-9]{6,35})\\b")
    )

    // Priority 3: Reference Number
    private val REFERENCE_PATTERNS = listOf(
        // Ref No: 123456, Ref no 3482910, Reference No: 123456, RRN: 123456, IMPS Ref: 123456
        Pattern.compile("(?i)\\b(?:ref(?:erence)?|rrn|imps\\s*ref)\\s*(?:(?:id|no|num|number|#)\\.?\\s*[:/=\\s-]*|[:/=-]+\\s*|\\s+no\\.?\\s+)([A-Za-z0-9]{6,35})\\b")
    )

    fun parse(smsBody: String, sender: String = ""): ParsedSmsResult {
        if (smsBody.isBlank()) {
            return ParsedSmsResult(isBankTransaction = false, ignoreReason = "Empty body")
        }

        val lowerBody = smsBody.lowercase()

        // 1. Check for OTP messages
        for (otpPattern in OTP_PATTERNS) {
            if (otpPattern.matcher(smsBody).find() && !lowerBody.contains("debited") && !lowerBody.contains("spent") && !lowerBody.contains("utr")) {
                return ParsedSmsResult(isBankTransaction = false, ignoreReason = "Ignored: OTP / Security code")
            }
        }

        // 2. Check if bank transaction keywords exist
        var hasKeyword = false
        for (kw in TRANSACTION_KEYWORDS) {
            if (lowerBody.contains(kw)) {
                hasKeyword = true
                break
            }
        }
        if (!hasKeyword) {
            return ParsedSmsResult(isBankTransaction = false, ignoreReason = "Not a financial transaction")
        }

        // 3. Extract Amount
        var amount = 0.0
        val matcher1 = AMOUNT_REGEX.matcher(smsBody)
        if (matcher1.find()) {
            val amountStr = matcher1.group(1)?.replace(",", "")
            amount = amountStr?.toDoubleOrNull() ?: 0.0
        } else {
            val matcher2 = ALT_AMOUNT_REGEX.matcher(smsBody)
            if (matcher2.find()) {
                val amountStr = matcher2.group(1)?.replace(",", "")
                amount = amountStr?.toDoubleOrNull() ?: 0.0
            }
        }

        if (amount <= 0.0) {
            return ParsedSmsResult(isBankTransaction = false, ignoreReason = "Could not extract valid amount")
        }

        // 4. Determine Type: EXPENSE or INCOME
        val isCredit = lowerBody.contains("credited") ||
                lowerBody.contains("received") ||
                lowerBody.contains("deposited") ||
                lowerBody.contains("refund") ||
                lowerBody.contains("cashback") ||
                (lowerBody.contains("salary") && !lowerBody.contains("debited"))

        val type = if (isCredit) "INCOME" else "EXPENSE"

        // 5. Extract Bank Name & Account
        val bankName = extractBankName(smsBody, sender)
        val accMatcher = ACCOUNT_REGEX.matcher(smsBody)
        val accountSuffix = if (accMatcher.find()) {
            val rawAcc = accMatcher.group(1) ?: ""
            if (rawAcc.startsWith("X", ignoreCase = true) || rawAcc.startsWith("*")) rawAcc else "XX$rawAcc"
        } else ""

        val accountOrBank = if (accountSuffix.isNotEmpty()) "$bankName $accountSuffix" else bankName

        // 6. Extract Merchant / Beneficiary
        val merchant = extractMerchant(smsBody, lowerBody, type)

        // 7. Auto-categorize
        val category = determineCategory(merchant, lowerBody, type)

        // 8. Extract Transaction Reference (Priority order: 1. UTR/UPI Txn ID -> 2. Bank Txn ID -> 3. Reference No)
        val referenceId = extractTransactionReference(smsBody)

        return ParsedSmsResult(
            isBankTransaction = true,
            amount = amount,
            type = type,
            merchant = merchant,
            category = category,
            accountOrBank = accountOrBank,
            referenceId = referenceId
        )
    }

    /**
     * Extracts unique transaction identifier in strict priority order:
     * 1. UTR / UPI Transaction ID
     * 2. Bank Transaction ID
     * 3. Reference Number
     */
    fun extractTransactionReference(body: String): String {
        // Priority 1: UTR / UPI Transaction ID
        for (pattern in UTR_UPI_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val ref = matcher.group(1)?.trim() ?: ""
                if (isValidReference(ref)) return ref.uppercase()
            }
        }

        // Priority 2: Bank Transaction ID
        for (pattern in BANK_TXN_ID_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val ref = matcher.group(1)?.trim() ?: ""
                if (isValidReference(ref)) return ref.uppercase()
            }
        }

        // Priority 3: Reference Number
        for (pattern in REFERENCE_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val ref = matcher.group(1)?.trim() ?: ""
                if (isValidReference(ref)) return ref.uppercase()
            }
        }

        return ""
    }

    private fun isValidReference(ref: String): Boolean {
        if (ref.length < 6 || ref.length > 35) return false
        val lower = ref.lowercase()
        val noiseWords = listOf("account", "balance", "debited", "credited", "customer", "ending", "available", "towards")
        if (noiseWords.contains(lower)) return false
        return true
    }

    private fun extractBankName(body: String, sender: String): String {
        val s = (sender + " " + body).uppercase()
        return when {
            s.contains("HDFC") -> "HDFC Bank"
            s.contains("SBI") || s.contains("STATE BANK") -> "SBI"
            s.contains("ICICI") -> "ICICI Bank"
            s.contains("AXIS") -> "Axis Bank"
            s.contains("KOTAK") -> "Kotak Bank"
            s.contains("PAYTM") -> "Paytm Bank"
            s.contains("YES") -> "Yes Bank"
            s.contains("PNB") -> "PNB"
            s.contains("UNION") -> "Union Bank"
            s.contains("CANARA") || s.contains("CANBNK") -> "Canara Bank"
            s.contains("IDFC") -> "IDFC First Bank"
            s.contains("INDUS") || s.contains("INDUSB") -> "IndusInd Bank"
            s.contains("BOB") || s.contains("BARODA") -> "Bank of Baroda"
            s.contains("GPAY") || s.contains("GOOGLE PAY") -> "Google Pay"
            s.contains("PHONEPE") -> "PhonePe"
            else -> "Bank Account"
        }
    }

    private fun extractMerchant(body: String, lower: String, type: String): String {
        val knownMerchants = listOf(
            "Swiggy", "Zomato", "Blinkit", "Zepto", "Instamart", "BigBasket", "Amazon", "Flipkart",
            "Myntra", "Ajio", "Uber", "Ola", "Rapido", "Netflix", "Spotify", "BookMyShow", "PVR",
            "Inox", "Airtel", "Jio", "Apollo", "PharmEasy", "1mg", "Zerodha", "Groww", "Starbucks",
            "McDonald's", "KFC", "Domino's", "D-Mart", "Croma", "Indian Oil", "BPCL", "HPCL",
            "Tata Neu", "MakeMyTrip", "IRCTC", "Electricity Board", "ATM Cash"
        )
        for (m in knownMerchants) {
            if (lower.contains(m.lowercase())) {
                return m
            }
        }

        if (lower.contains("atm") || lower.contains("cash wdl")) {
            return "ATM Cash Withdrawal"
        }

        // Check if there is "Merchant: X" format
        val merchantLabelPattern = Pattern.compile("(?i)(?:merchant|store)\\s*[:/=\\s-]+\\s*([A-Za-z0-9&.' -]{3,30})")
        val labelMatcher = merchantLabelPattern.matcher(body)
        if (labelMatcher.find()) {
            val candidate = labelMatcher.group(1)?.trim() ?: ""
            if (candidate.isNotBlank()) return candidate.take(24)
        }

        // Try extracting after phrases like "towards", "at", "to", "for", "vpa"
        val merchantPattern = Pattern.compile("(?i)(?:towards|at|to|for|vpa|paid to)\\s+([A-Za-z0-9&.' -]{3,25})(?:\\s+on|\\s+via|\\s+ref|\\s+avl|\\s+\\.|\$)")
        val mMatcher = merchantPattern.matcher(body)
        if (mMatcher.find()) {
            val candidate = mMatcher.group(1)?.trim() ?: ""
            if (candidate.isNotEmpty() && !candidate.equals("your", ignoreCase = true) && !candidate.equals("a/c", ignoreCase = true)) {
                return candidate.take(24)
            }
        }

        return if (type == "INCOME") "Bank Transfer / Salary" else "Merchant / UPI"
    }

    private fun determineCategory(merchant: String, lower: String, type: String): String {
        if (type == "INCOME") {
            return if (lower.contains("salary") || lower.contains("payroll")) "Salary"
            else if (lower.contains("refund") || lower.contains("cashback")) "Refund / Cashback"
            else if (lower.contains("dividend") || lower.contains("interest")) "Investment"
            else "Income"
        }

        val text = (merchant + " " + lower).lowercase()
        return when {
            text.contains("swiggy") || text.contains("zomato") || text.contains("starbucks") ||
                    text.contains("mcdonald") || text.contains("kfc") || text.contains("domino") ||
                    text.contains("cafe") || text.contains("restaurant") || text.contains("dining") ||
                    text.contains("burger") || text.contains("pizza") || text.contains("food") -> "Food & Dining"

            text.contains("blinkit") || text.contains("zepto") || text.contains("instamart") ||
                    text.contains("bigbasket") || text.contains("dmart") || text.contains("grocer") ||
                    text.contains("supermarket") || text.contains("milk") || text.contains("vegetable") -> "Groceries"

            text.contains("amazon") || text.contains("flipkart") || text.contains("myntra") ||
                    text.contains("ajio") || text.contains("meesho") || text.contains("nykaa") ||
                    text.contains("croma") || text.contains("zara") || text.contains("shopping") ||
                    text.contains("retail") || text.contains("mall") -> "Shopping"

            text.contains("airtel") || text.contains("jio") || text.contains("bescom") ||
                    text.contains("electricity") || text.contains("bill") || text.contains("broadband") ||
                    text.contains("recharge") || text.contains("gas") || text.contains("cylinder") ||
                    text.contains("tata play") || text.contains("tatasky") || text.contains("water") -> "Bills & Utilities"

            text.contains("uber") || text.contains("ola") || text.contains("rapido") ||
                    text.contains("petrol") || text.contains("diesel") || text.contains("fuel") ||
                    text.contains("indian oil") || text.contains("bpcl") || text.contains("hpcl") ||
                    text.contains("shell") || text.contains("irctc") || text.contains("metro") ||
                    text.contains("fastag") || text.contains("toll") -> "Transport & Fuel"

            text.contains("netflix") || text.contains("spotify") || text.contains("bookmyshow") ||
                    text.contains("pvr") || text.contains("inox") || text.contains("hotstar") ||
                    text.contains("cinema") || text.contains("prime video") || text.contains("movie") -> "Entertainment"

            text.contains("apollo") || text.contains("pharmeasy") || text.contains("1mg") ||
                    text.contains("hospital") || text.contains("clinic") || text.contains("pharmacy") ||
                    text.contains("medic") || text.contains("doctor") -> "Health & Medical"

            text.contains("zerodha") || text.contains("groww") || text.contains("angel") ||
                    text.contains("upstox") || text.contains("mutual fund") || text.contains("sip") ||
                    text.contains("lic") -> "Investment"

            text.contains("atm") || text.contains("cash wdl") || text.contains("withdrawal") -> "Cash & ATM"

            else -> "Other Expenses"
        }
    }
}
