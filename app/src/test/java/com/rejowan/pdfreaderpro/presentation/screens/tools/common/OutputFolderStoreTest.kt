package com.rejowan.pdfreaderpro.presentation.screens.tools.common

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OutputFolderStoreTest {

    private val prefs = FakePrefs()
    private val resolver: ContentResolver = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val treeUri: Uri = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { context.contentResolver } returns resolver
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns treeUri
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    private fun store() = OutputFolderStore(context)

    @Test
    fun resetToDefaults_forgetsFolderAndTurnsAskEveryTimeOff() {
        prefs.values["tree_uri"] = "content://tree/Download"
        prefs.values["hint_uri"] = "content://tree/Download/hint"
        prefs.values["ask_every_time"] = true
        val store = store()
        assertTrue(store.askEveryTime)

        store.resetToDefaults()

        assertFalse(store.askEveryTime)
        assertFalse(prefs.values.containsKey("tree_uri"))
        assertFalse(prefs.values.containsKey("hint_uri"))
        assertNull(store.initialUri())
        verify { resolver.releasePersistableUriPermission(treeUri, any()) }
    }

    @Test
    fun resetToDefaults_worksWhenOnlyAskEveryTimeIsOn() {
        val store = store()
        store.askEveryTime = true

        store.resetToDefaults()

        assertFalse(store.askEveryTime)
        assertEquals(false, prefs.values["ask_every_time"])
    }

    @Test
    fun resetToDefaults_isSafeOnAFreshStore() {
        val store = store()

        store.resetToDefaults()

        assertFalse(store.askEveryTime)
        assertNull(store.initialUri())
    }

    @Test
    fun clear_keepsAskEveryTime() {
        // clear() is what tools use for an unusable folder; it must not touch the user's choice.
        prefs.values["tree_uri"] = "content://tree/Download"
        prefs.values["ask_every_time"] = true
        val store = store()

        store.clear()

        assertFalse(prefs.values.containsKey("tree_uri"))
        assertTrue(store.askEveryTime)
    }

    /** Minimal in-memory SharedPreferences (the unit-test android.jar has no implementation). */
    private class FakePrefs : SharedPreferences {
        val values = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values
        override fun getString(key: String, defValue: String?) = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int) = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long) = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float) = values[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean) = values[key] as? Boolean ?: defValue
        override fun contains(key: String) = values.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()

            override fun putString(key: String, value: String?) = this.also { pending[key] = value }
            override fun putStringSet(key: String, value: MutableSet<String>?) = this.also { pending[key] = value }
            override fun putInt(key: String, value: Int) = this.also { pending[key] = value }
            override fun putLong(key: String, value: Long) = this.also { pending[key] = value }
            override fun putFloat(key: String, value: Float) = this.also { pending[key] = value }
            override fun putBoolean(key: String, value: Boolean) = this.also { pending[key] = value }
            override fun remove(key: String) = this.also { removed += key }
            override fun clear() = this.also { removed += values.keys }
            override fun commit(): Boolean {
                removed.forEach { values.remove(it) }
                values.putAll(pending)
                return true
            }

            override fun apply() {
                commit()
            }
        }
    }
}
