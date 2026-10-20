package com.recai.app.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract

/**
 * Result of attempting to launch WhatsApp.
 */
enum class WhatsAppLaunchResult {
    SUCCESS,
    NOT_INSTALLED,
    INVALID_NUMBER,
    FAILED
}

/**
 * Reusable helper for phone-related intent actions:
 * - Opening the system dialer with pre-filled number (ACTION_DIAL)
 * - Launching the system Contacts app to add a contact (ACTION_INSERT)
 * - Launching the system SMS/messaging app with pre-filled number (ACTION_SENDTO)
 * - Launching WhatsApp with direct deep link conversation
 *
 * Never directly initiates calls (no CALL_PHONE), never silently sends SMS,
 * and never writes directly to ContactsContract (no WRITE_CONTACTS).
 */
object PhoneActionHelper {

    /**
     * Checks if a phone number string represents a usable, actionable number.
     * Returns false for null, blank, or private/restricted numbers.
     */
    fun isActionablePhoneNumber(phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrBlank()) return false
        val trimmed = phoneNumber.trim()
        if (trimmed.equals("Private / Unknown number", ignoreCase = true) ||
            trimmed.equals("Unknown caller", ignoreCase = true) ||
            trimmed.equals("Not available", ignoreCase = true) ||
            trimmed.equals("null", ignoreCase = true)
        ) {
            return false
        }
        val digits = trimmed.filter { it.isDigit() }
        return digits.length >= 7
    }

    /**
     * Checks if callerName represents an already known/saved contact name,
     * as opposed to being unknown or identical to the phone number.
     */
    fun isKnownCaller(callerName: String?, phoneNumber: String?): Boolean {
        if (callerName.isNullOrBlank()) return false
        val trimmed = callerName.trim()
        if (trimmed.equals("Unknown caller", ignoreCase = true)) return false
        if (phoneNumber != null && trimmed == phoneNumber.trim()) return false
        return true
    }

    /**
     * Opens the system dialer with [phoneNumber] pre-filled.
     * Uses [Intent.ACTION_DIAL]. Does NOT place the call automatically.
     *
     * @return true if the intent was successfully dispatched, false otherwise.
     */
    fun openDialer(context: Context, phoneNumber: String?): Boolean {
        if (!isActionablePhoneNumber(phoneNumber)) {
            return false
        }
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber?.trim())}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Builds the smsto: URI string for messaging.
     * Returns null if the phone number is not actionable.
     */
    fun buildSmsUriString(phoneNumber: String?): String? {
        if (!isActionablePhoneNumber(phoneNumber)) {
            return null
        }
        return "smsto:${phoneNumber?.trim()}"
    }

    /**
     * Opens the system SMS/messaging application with [phoneNumber] pre-filled.
     * Uses [Intent.ACTION_SENDTO] with smsto:<number>.
     * Does NOT send any message automatically.
     *
     * @return true if an SMS application was found and launched, false otherwise.
     */
    fun openSms(context: Context, phoneNumber: String?): Boolean {
        val uriString = buildSmsUriString(phoneNumber) ?: return false
        return try {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse(uriString)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(smsIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Converts a phone number to international digits-only format required by WhatsApp.
     *
     * Examples:
     * - "+91 88307 14901" -> "918830714901"
     * - "00918830714901"  -> "918830714901"
     * - "918830714901"    -> "918830714901"
     * - "08830714901"     -> "918830714901"
     * - "8830714901"      -> "918830714901"
     * - "+1 202 555 0123" -> "12025550123"
     * - null / invalid    -> null
     */
    fun formatWhatsAppNumber(phoneNumber: String?): String? {
        if (!isActionablePhoneNumber(phoneNumber)) {
            return null
        }

        val trimmed = phoneNumber!!.trim()
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length < 7) {
            return null
        }

        // 1. Indian format: starts with "0091" exit code followed by 10 digits (total 14 digits)
        if (digitsOnly.startsWith("0091") && digitsOnly.length == 14) {
            return digitsOnly.substring(2)
        }

        // 2. Generic international exit code "00"
        if (digitsOnly.startsWith("00") && digitsOnly.length > 4) {
            return digitsOnly.substring(2)
        }

        // 3. Indian format: 11 digits starting with "0" trunk prefix (e.g. "08830714901") -> "918830714901"
        if (digitsOnly.length == 11 && digitsOnly.startsWith("0")) {
            return "91" + digitsOnly.substring(1)
        }

        // 4. Indian format: 10 raw digits (e.g. "8830714901") -> default country code "918830714901"
        if (digitsOnly.length == 10) {
            return "91$digitsOnly"
        }

        // 5. Already has country code 91 with 12 digits (e.g. "918830714901")
        if (digitsOnly.length == 12 && digitsOnly.startsWith("91")) {
            return digitsOnly
        }

        // 6. Other international numbers (e.g. US "12025550123", UK "447911123456")
        return digitsOnly
    }

    /**
     * Builds the WhatsApp deep link URL string.
     */
    fun buildWhatsAppUriString(phoneNumber: String?): String? {
        val digits = formatWhatsAppNumber(phoneNumber) ?: return null
        return "https://api.whatsapp.com/send?phone=$digits"
    }

    /**
     * Opens WhatsApp directly to a conversation with [phoneNumber].
     * Tries standard WhatsApp ("com.whatsapp") first, then WhatsApp Business ("com.whatsapp.w4b").
     *
     * @return [WhatsAppLaunchResult.SUCCESS] if WhatsApp was launched,
     *         [WhatsAppLaunchResult.NOT_INSTALLED] if neither WhatsApp is installed,
     *         [WhatsAppLaunchResult.INVALID_NUMBER] if the number cannot be formatted.
     */
    fun openWhatsApp(context: Context, phoneNumber: String?): WhatsAppLaunchResult {
        val uriString = buildWhatsAppUriString(phoneNumber) ?: return WhatsAppLaunchResult.INVALID_NUMBER
        val uri = Uri.parse(uriString)

        // Try standard WhatsApp package
        try {
            val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(waIntent)
            return WhatsAppLaunchResult.SUCCESS
        } catch (e: Exception) {
            // Standard WhatsApp not found, try WhatsApp Business
            try {
                val w4bIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp.w4b")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(w4bIntent)
                return WhatsAppLaunchResult.SUCCESS
            } catch (e2: Exception) {
                return WhatsAppLaunchResult.NOT_INSTALLED
            }
        }
    }

    /**
     * Opens the system Contacts insertion UI with [phoneNumber] and optionally [callerName] pre-filled.
     * Uses [Intent.ACTION_INSERT] with [ContactsContract.Contacts.CONTENT_TYPE].
     *
     * @return true if the intent was successfully dispatched, false otherwise.
     */
    fun openAddContact(context: Context, phoneNumber: String?, callerName: String? = null): Boolean {
        if (!isActionablePhoneNumber(phoneNumber)) {
            return false
        }
        return try {
            val contactIntent = Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber?.trim())
                if (isKnownCaller(callerName, phoneNumber)) {
                    putExtra(ContactsContract.Intents.Insert.NAME, callerName?.trim())
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(contactIntent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
