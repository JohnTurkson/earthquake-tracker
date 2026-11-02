package com.johnturkson.sandbox.data

import com.johnturkson.sandbox.annotations.IoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

class MainRepository @Inject constructor(
    private val api: ItemApi,
    private val dao: ItemDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
)
