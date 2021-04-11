package ca.burchill.cointracker.viewModels

import android.app.Application
import androidx.lifecycle.*
import ca.burchill.cointracker.database.getDatabase
import ca.burchill.cointracker.repository.CoinsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException


enum class CoinApiStatus { LOADING, ERROR, DONE }


class CoinListViewModel(application: Application) : AndroidViewModel(application) {

    // The internal MutableLiveData that stores the status of the most recent request
    private val _status = MutableLiveData<CoinApiStatus>()

    val status: LiveData<CoinApiStatus>
        get() = _status

    //from original file
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)


    /**
     * The data source this ViewModel will fetch results from.
     */
    private val coinsRepository = CoinsRepository(getDatabase(application))

    /**
     * A playlist of videos displayed on the screen.
     */
    val coins = coinsRepository.coins

    /**
     * Event triggered for network error. This is private to avoid exposing a
     * way to set this value to observers.
     */
    private var _eventNetworkError = MutableLiveData<Boolean>(false)

    /**
     * Event triggered for network error. Views should use this to get access
     * to the data.
     */
    val eventNetworkError: LiveData<Boolean>
        get() = _eventNetworkError

    /**
     * Flag to display the error message. This is private to avoid exposing a
     * way to set this value to observers.
     */
    private var _isNetworkErrorShown = MutableLiveData<Boolean>(false)


    /**
     * init{} is called immediately when this ViewModel is created.
     */
    init {
        refreshDataFromRepository()
    }

    /**
     * Refresh data from the repository. Use a coroutine launch to run in a
     * background thread.
     */
    private fun refreshDataFromRepository() {
        viewModelScope.launch {
            try {
                coinsRepository.refreshCoins()
                _eventNetworkError.value = false
                _isNetworkErrorShown.value = false

            } catch (networkError: IOException) {
                // Show a Toast error message and hide the progress bar.
                _status.value = CoinApiStatus.ERROR
            }
        }
    }

    /**
     * Factory for constructing CoinListViewModel with parameter
     */
    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CoinListViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CoinListViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}