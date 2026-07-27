package com.example.data.imports

import android.content.Context
import android.provider.ContactsContract

/**
 * Represents a single contact read from the device's contact list.
 */
data class ContactInfo(
    val name: String,
    val phone: String,
    val lookupKey: String // For deduplication
)

/**
 * Reads contacts from the device's contact list.
 */
object ContactsReader {

    /**
     * Read all contacts with at least one phone number.
     */
    fun readContacts(context: Context): List<ContactInfo> {
        val contacts = mutableMapOf<String, ContactInfo>()

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0)?.trim() ?: continue
                val phone = it.getString(1)?.trim() ?: continue
                val lookupKey = it.getString(2) ?: ""

                // Normalize phone number (basic cleanup)
                val cleanPhone = phone.replace(" ", "").replace("-", "").replace("+", "")

                // Skip contacts without a valid name
                if (name.isBlank()) continue

                // Use lookup key to deduplicate (a contact may have multiple phones)
                if (lookupKey !in contacts) {
                    contacts[lookupKey] = ContactInfo(
                        name = name,
                        phone = cleanPhone,
                        lookupKey = lookupKey
                    )
                }
            }
        }

        return contacts.values.toList()
    }
}
