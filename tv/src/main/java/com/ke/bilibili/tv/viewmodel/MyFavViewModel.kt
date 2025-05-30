package com.ke.bilibili.tv.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.ke.biliblli.api.response.FavResourceMediaResponse
import com.ke.biliblli.api.response.UserFavResponse
import com.ke.biliblli.common.BilibiliRepository
import com.ke.biliblli.common.CrashHandler
import com.ke.biliblli.common.Screen
import com.ke.biliblli.common.event.MainTab
import com.ke.biliblli.common.event.MainTabChanged
import com.ke.biliblli.repository.paging.FavResourceListPagingSource
import com.ke.biliblli.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import javax.inject.Inject

@HiltViewModel
class MyFavViewModel @Inject constructor(
    private val bilibiliRepository: BilibiliRepository,
    savedStateHandle: SavedStateHandle
) :
    BaseViewModel<MyFavState, MyFavAction, Unit>(MyFavState()) {

    private val userId = savedStateHandle.toRoute<Screen.Main>().userId


    override fun handleAction(action: MyFavAction) {
        when (action) {
            is MyFavAction.ClickFav -> {
                _uiState.update {
                    it.copy(selected = action.value)
                }
            }
        }
    }

    val listFlow = Pager<Int, FavResourceMediaResponse>(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        FavResourceListPagingSource(bilibiliRepository, uiState.value.selected?.id ?: 0)
    }.flow.cachedIn(viewModelScope)


    @Subscribe
    fun onTabChanged(event: MainTabChanged) {
        if (event.tab == MainTab.Fav) {
            refresh()
        }
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    init {

        EventBus.getDefault().register(this)
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            try {
                val list = bilibiliRepository.userFav(userId).data!!.list
                _uiState.value = MyFavState(list, list.first())
                _event.send(Unit)
            } catch (e: Exception) {
                CrashHandler.handler(e)
            }
        }
    }
}

data class MyFavState(
    val list: List<UserFavResponse> = emptyList(), val selected: UserFavResponse? = null
)

sealed interface MyFavAction {
    data class ClickFav(val value: UserFavResponse) : MyFavAction
}