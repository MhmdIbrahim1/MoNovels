package com.lagradost.quicknovel.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

//https://stackoverflow.com/questions/34697828/parallel-operations-on-kotlin-collections
fun <T, R> Iterable<T>.pmap(
    numThreads: Int = maxOf(Runtime.getRuntime().availableProcessors() - 2, 1),
    exec: ExecutorService = Executors.newFixedThreadPool(numThreads),
    transform: (T) -> R,
): List<R> {

    // default size is just an inlined version of kotlin.collections.collectionSizeOrDefault
    val defaultSize = if (this is Collection<*>) this.size else 10
    val destination = Collections.synchronizedList(ArrayList<R>(defaultSize))

    for (item in this) {
        exec.submit { destination.add(transform(item)) }
    }

    exec.shutdown()
    exec.awaitTermination(1, TimeUnit.DAYS)

    return ArrayList<R>(destination)
}

fun <A, B>List<A>.apmap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun <A, B> List<A>.amap(f: suspend (A) -> B): List<B> =
    with(CoroutineScope(GlobalScope.coroutineContext)) {
        map { async { f(it) } }.map { it.await() }
    }