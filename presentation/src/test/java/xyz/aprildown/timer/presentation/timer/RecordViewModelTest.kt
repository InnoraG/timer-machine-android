package xyz.aprildown.timer.presentation.timer

import android.text.format.DateUtils
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.FolderSortBy
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.domain.entities.toTimerInfo
import xyz.aprildown.timer.domain.repositories.PreferencesRepository
import xyz.aprildown.timer.domain.usecases.folder.FolderSortByRule
import xyz.aprildown.timer.domain.usecases.folder.GetFolders
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.domain.usecases.record.GetRecords
import xyz.aprildown.timer.domain.usecases.timer.GetTimerInfo

class RecordViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val getRecords: GetRecords = mock()
    private val getFolders: GetFolders = mock()
    private val folderSortByRule: FolderSortByRule = mock()
    private val getTimerInfo: GetTimerInfo = mock()
    private val preferencesRepository: PreferencesRepository = mock()

    @Test
    fun `initial load`() = runTest {
        whenever(folderSortByRule.get()).thenReturn(FolderSortBy.entries.random())
        val folders = listOf(
            TestData.defaultFolder,
            TestData.trashFolder,
            TestData.fakeFolder
        ).shuffled().filter { !it.isTrash }
        whenever(getFolders()).thenReturn(folders)
        val timerInfo = listOf(
            TestData.fakeTimerSimpleA,
            TestData.fakeTimerSimpleB,
            TestData.fakeTimerAdvanced
        ).map { it.toTimerInfo() }
        whenever(getTimerInfo(any())).thenReturn(timerInfo)

        val viewModel = newViewModel()

        val allTimerInfoObserver: Observer<List<TimerInfo>> = mock()
        viewModel.allTimerInfo.observeForever(allTimerInfoObserver)
        val selectedTimerInfoObserver: Observer<List<TimerInfo>> = mock()
        viewModel.selectedTimerInfo.observeForever(selectedTimerInfoObserver)
        val paramsObserver: Observer<GetRecords.Params> = mock()
        viewModel.params.observeForever(paramsObserver)
        val startTimeObserver: Observer<Long> = mock()
        viewModel.startTime.observeForever(startTimeObserver)
        val endTimeObserver: Observer<Long> = mock()
        viewModel.endTime.observeForever(endTimeObserver)

        verify(folderSortByRule).get()
        verify(getFolders).invoke()
        verify(getTimerInfo, times(folders.size)).invoke(any())
        val allTimerInfo = mutableListOf<TimerInfo>().apply {
            repeat(folders.size) {
                addAll(timerInfo)
            }
        }
        verify(allTimerInfoObserver).onChanged(allTimerInfo)
        argumentCaptor<GetRecords.Params> {
            verify(paramsObserver).onChanged(capture())
            val params = firstValue
            val span = params.endTime - params.startTime
            assertTrue(
                span >= DateUtils.WEEK_IN_MILLIS &&
                    span < DateUtils.WEEK_IN_MILLIS + DateUtils.DAY_IN_MILLIS * 2
            )
            verify(startTimeObserver).onChanged(params.startTime)
            verify(endTimeObserver).onChanged(params.endTime)
        }
        verify(selectedTimerInfoObserver).onChanged(allTimerInfo)

        verifyNoMoreInteractionsForAll()
        verifyNoMoreInteractions(allTimerInfoObserver)
        verifyNoMoreInteractions(selectedTimerInfoObserver)
        verifyNoMoreInteractions(paramsObserver)
        verifyNoMoreInteractions(startTimeObserver)
        verifyNoMoreInteractions(endTimeObserver)
    }

    @Test
    fun `distinct params`() = runTest {
        whenever(getFolders.invoke()).thenReturn(emptyList())
        whenever(folderSortByRule.get()).thenReturn(FolderSortBy.entries.random())

        val viewModel = newViewModel()

        val paramsObserver: Observer<GetRecords.Params> = mock()
        viewModel.params.observeForever(paramsObserver)

        viewModel.updateParams()
        viewModel.updateParams()

        verify(paramsObserver, times(1)).onChanged(any())
        verifyNoMoreInteractions(paramsObserver)
    }

    private suspend fun TestScope.newViewModel(): RecordViewModel {
        whenever(getRecords.getMinDateMilli()).thenReturn(TimerStampEntity.getMinDateMilli())
        whenever(
            preferencesRepository.getInt(any(), any())
        ).thenReturn(RecordViewModel.START_SINCE_LAST_WEEK)
        whenever(preferencesRepository.getNullableString(any(), any())).thenReturn(null)
        return RecordViewModel(
            mainDispatcher = StandardTestDispatcher(testScheduler),
            getRecords = getRecords,
            getFolders = getFolders,
            folderSortByRule = folderSortByRule,
            getTimerInfo = getTimerInfo,
            preferencesRepository = preferencesRepository,
        ).also {
            testScheduler.advanceUntilIdle()
            verify(getRecords).getMinDateMilli()
        }
    }

    private fun verifyNoMoreInteractionsForAll() {
        verifyNoMoreInteractions(getRecords)
        verifyNoMoreInteractions(getFolders)
        verifyNoMoreInteractions(folderSortByRule)
        verifyNoMoreInteractions(getTimerInfo)
    }
}
