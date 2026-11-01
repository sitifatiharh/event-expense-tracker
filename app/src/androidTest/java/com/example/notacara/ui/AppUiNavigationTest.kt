package com.example.notacara.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.notacara.data.local.entity.AcaraEntity
import com.example.notacara.data.local.entity.NotaEntity
import com.example.notacara.data.local.entity.TransaksiEntity
import com.example.notacara.data.model.AnalysisResult
import com.example.notacara.data.repository.NotaRepository
import com.example.notacara.ui.screens.EditNotaScreen
import com.example.notacara.ui.screens.HomeScreen
import com.example.notacara.ui.screens.ProcessingScreen
import com.example.notacara.ui.theme.NotacaraTheme
import com.example.notacara.ui.viewmodel.HomeViewModel
import com.example.notacara.ui.viewmodel.NotacaraViewModelFactory
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUiNavigationTest {

    // Menggunakan createAndroidComposeRule agar Host Activity diluncurkan secara nyata dan stabil
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var realm: Realm
    private lateinit var repository: NotaRepository
    private lateinit var viewModel: HomeViewModel
    private lateinit var viewModelStore: ViewModelStore
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        val config = RealmConfiguration.Builder(schema = setOf(AcaraEntity::class, NotaEntity::class, TransaksiEntity::class))
            .name("ui_test_db_${System.nanoTime()}")
            .inMemory()
            .build()
        realm = Realm.open(config)
        repository = NotaRepository(realm)

        viewModelStore = ViewModelStore()
        val factory = NotacaraViewModelFactory(app, repository)
        viewModel = ViewModelProvider(viewModelStore, factory)[HomeViewModel::class.java]
    }

    @After
    fun tearDown() = runBlocking {
        withContext(Dispatchers.Main) {
            viewModelStore.clear()
        }
        if (this@AppUiNavigationTest::realm.isInitialized) {
            realm.close()
        }
    }

    @Test
    fun HomeScreen_InitialEmptyState() {
        composeTestRule.setContent {
            NotacaraTheme {
                HomeScreen(onAcaraClick = {}, viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Gabungan Rendering Check & Empty State
        composeTestRule.onNodeWithTag("search_bar").assertExists()
        composeTestRule.onNodeWithText("Tambah Acara").assertIsDisplayed()
        composeTestRule.onNodeWithText("Belum ada acara. Ketuk + untuk membuat.").assertIsDisplayed()
    }

    @Test
    fun HomeScreen_SearchInteraction() {
        runBlocking {
            repository.insertAcara(AcaraEntity().apply { nama_acara = "Makan Malam" })
            repository.insertAcara(AcaraEntity().apply { nama_acara = "Liburan" })
        }

        composeTestRule.setContent {
            NotacaraTheme {
                HomeScreen(onAcaraClick = {}, viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("search_bar").performTextInput("Makan")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Makan Malam").assertIsDisplayed()
        composeTestRule.onNodeWithText("Liburan").assertDoesNotExist()
    }

    @Test
    fun AddAcaraDialog_Flow() {
        composeTestRule.setContent {
            NotacaraTheme {
                HomeScreen(onAcaraClick = {}, viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Tambah Acara").performClick()
        composeTestRule.onNodeWithText("Buat Acara").assertIsDisplayed()
        composeTestRule.onNodeWithText("Batal").performClick()
        composeTestRule.onNodeWithText("Buat Acara").assertDoesNotExist()
    }

    @Test
    fun LoadingState_IsDisplayed() {
        composeTestRule.setContent {
            NotacaraTheme {
                ProcessingScreen(imageUri = android.net.Uri.EMPTY, onProcessingFinished = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("loading_indicator").assertExists()
        composeTestRule.onNodeWithText("Sedang Memproses Nota Anda").assertIsDisplayed()
    }

    @Test
    fun Navigation_Trigger() {
        var clickedName = ""
        runBlocking {
            repository.insertAcara(AcaraEntity().apply { nama_acara = "Target" })
        }

        composeTestRule.setContent {
            NotacaraTheme {
                HomeScreen(onAcaraClick = { clickedName = it }, viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Target").performClick()
        assert(clickedName == "Target")
    }

    @Test
    fun EditNotaScreen_Binding() {
        val mockResult = AnalysisResult(namaToko = "INDOMARET", tanggal = "10/10/2023")
        composeTestRule.setContent {
            NotacaraTheme {
                EditNotaScreen(
                    analysisResult = mockResult, imageUri = null, isNewNota = true,
                    onSaveClick = { _, _, _, _, _, _ -> }, onCancelClick = {}, onReScan = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("INDOMARET").assertIsDisplayed()
        composeTestRule.onNodeWithText("10/10/2023").assertIsDisplayed()
    }

    @Test
    fun DataDisplay_AfterInsertion() {
        val testName = "Acara Kantor"
        runBlocking {
            repository.insertAcara(AcaraEntity().apply { nama_acara = testName })
        }

        composeTestRule.setContent {
            NotacaraTheme {
                HomeScreen(onAcaraClick = {}, viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(testName).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(testName).assertIsDisplayed()
    }

    @Test
    fun ContextualTopBar_SelectionInteraction() {
        runBlocking {
            repository.insertAcara(AcaraEntity().apply { nama_acara = "Acara Test" })
        }
        
        composeTestRule.setContent {
            NotacaraTheme {
                HomeScreen(onAcaraClick = {}, viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Long click untuk masuk mode seleksi
        composeTestRule.onNodeWithText("Acara Test").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        // Memastikan TopBar berubah (muncul angka jumlah pilihan dan tombol Hapus/Edit)
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hapus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit").assertIsDisplayed()
    }
}
