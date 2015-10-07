package dev.wizrad.helloandroid.presenters

import dev.wizrad.helloandroid.views.MainView
import dev.wizrad.helloandroid.models.Region
import dev.wizrad.helloandroid.services.SummonerService
import dev.wizrad.helloandroid.services.utilities.UrlComponents

import android.util.Log
import rx.Observable
import rx.subjects.BehaviorSubject

import javax.inject.Inject

internal class MainPresenter @Inject constructor(
    val view: MainView,
    val summonerService: SummonerService) : Presenter(), MainPresenterType {

    //
    // region Properties
    //

    private val regions = Region.all()
    private val name = BehaviorSubject.create<String>()
    private val selectedRegion = BehaviorSubject.create<Region>()

    override fun didBecomeActive() {
        super.didBecomeActive()

        // automatically update the regions
        view.didUpdateRegions(regions.map { it.code })

        // update canSubmit as it changes
        subscriptions.add(canSubmit.subscribe { canSubmit ->
            view.didEnableSubmit(canSubmit)
        })
    }

    //
    // Input Bindings
    //

    override fun bindName(source: Observable<CharSequence>) {
        subscriptions.add(source
            .map { it.toString() }
            .subscribe(this.name))
    }

    override fun bindRegion(source: Observable<Int>) {
        subscriptions.add(source
            .map { regions[it] }
            .subscribe(selectedRegion))
    }

    override fun bindAction(source: Observable<Any>) {
        this.subscriptions.add(source
            .subscribe {
                this.fetchSummoner()
            })
    }

    //
    // Observables
    //

    val canSubmit: Observable<Boolean> get() {
        return Observable
            .combineLatest(selectedRegion, name) { region, name ->
                name.length() != 0 && region != null
            }
    }

    //
    // Services
    //

    private fun fetchSummoner() {
        this.subscriptions.add(Observable
            .combineLatest(selectedRegion, name) { region, name ->
                object { val region = region; val name = name }
            }
            .switchMap { tuple ->
                this.summonerService.fetchSummonersByName(tuple.region.code, UrlComponents(tuple.name))
            }
            .subscribe (
                { summoners -> this.view.didUpdateSummoner(summoners.values().first()) },
                { error     -> Log.e("test", "$error") }
            ))
    }
}
