package com.example.study.ui.main

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.Observable
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.ViewModelStoreOwner
import com.example.study.R
import com.example.study.data.local.MovieDatabase
import com.example.study.data.local.source.NaverSearchLocalDataSourceImpl
import com.example.study.data.remote.source.NaverSearchRemoteDataSourceImpl
import com.example.study.data.repository.NaverSearchRepositoryImpl
import com.example.study.databinding.ActivityMainBinding
import com.example.study.ui.adapter.MovieAdapter
import com.example.study.util.base.BaseActivity
import com.example.study.util.extension.plusAssign
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(R.layout.activity_main) {

    private val movieAdapter: MovieAdapter by lazy {
        MovieAdapter()
    }

/*        override val vm: MainViewModel by lazy {
            ViewModelProvider(this, object: ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return MainViewModel(NaverSearchRepositoryImpl.getInstance(
                        NaverSearchLocalDataSourceImpl.getInstance(SearchResultDatabase.getInstance(applicationContext)!!.searchResultDao())
                        , NaverSearchRemoteDataSourceImpl.getInstance()
                    )) as T
                }
            }).get(MainViewModel::class.java)
        }*/

    private val compositeDisposable = CompositeDisposable()
    private val backKeySubject = BehaviorSubject.createDefault<Long>(0L)

    override val vm: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return MainViewModel(
                    NaverSearchRepositoryImpl.getInstance(
                        NaverSearchLocalDataSourceImpl.getInstance(
                            MovieDatabase.getInstance(
                                applicationContext
                            )!!.MovieEntityDao()
                        )
                        , NaverSearchRemoteDataSourceImpl.getInstance()
                    )
                ) as T
            }
        }
    } // ktx 사용

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.rvMovieList.adapter = movieAdapter
        getRecentSearchResult()
        initObserve()
    }

    private fun initObserve() {
        vm.movieItems.observe(this, Observer { movieAdapter.setItem(it) })

        vm.errorQueryEmpty.observe(this, Observer { showErrorQueryEmpty() })

        vm.errorFailSearch.observe(
            this,
            Observer {
                Toast.makeText(applicationContext, it.toString(), Toast.LENGTH_SHORT).show()
            })

        vm.errorResultEmpty.observe(this, Observer { showErrorEmptyResult() })

        vm.isKeyboardBoolean.observe(this, Observer { if (!it) hideKeyboard() })

        vm.isProgressBoolean.observe(this, Observer {
            if(it) {
                binding.pbLoading.visibility = View.VISIBLE
            } else {
                binding.pbLoading.visibility = View.GONE
            }
        })
    }

    private fun bindViewModel() {
        backKeySubject
            .buffer(2, 1)
            .map { it[0] to it[1] }
            .subscribe {
                if(it.second - it.first < 2000L) {
                    super.onBackPressed()
                } else {
                    Toast.makeText(this, "앱을 종료하려면 한번 더 눌러주세요", Toast.LENGTH_SHORT).show()
                }
            }
            .addTo(compositeDisposable)
    }

    private fun getRecentSearchResult() {
        vm.getRecentSearchResult()
    }

    private fun showErrorQueryEmpty() {
        Toast.makeText(applicationContext, R.string.empty_query_message, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorEmptyResult() {
        Toast.makeText(applicationContext, R.string.empty_result_message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        bindViewModel()
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun onBackPressed() {
        backKeySubject.onNext(System.currentTimeMillis())
    }
}




