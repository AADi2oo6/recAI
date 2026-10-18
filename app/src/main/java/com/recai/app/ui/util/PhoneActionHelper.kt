package com.recai.app.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.recai.app.data.parser.PhoneNumberMatcher

/**
 * Reusable helper for phone-related intent actions:
 * - Opening the system dialer with pre-filled number (ACTION_DIAL)
 * - Launching the system Contacts app to add a contact (ACTION_INSERT)
 *
 * Never directly initiates calls (no CALL_PHONE) and never writes directly to ContactsContract (no WRITE_CONTACTS).
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
            trimmed.equals("Not available", ignoreCase = true)
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
