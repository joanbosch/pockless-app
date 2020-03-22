package com.pes.pockles.domain.newpock

import androidx.lifecycle.LiveData
import com.pes.pockles.data.Resource
import com.pes.pockles.data.repository.implementation.PockRepositoryImpl
import com.pes.pockles.model.Pock

class NewPockUseCase {
    val pockRepository: PockRepository by lazy {
        PockRepositoryImpl()
    }

    fun execute(pock: Pock): LiveData<Resource<Pock>> {
        return pockRepository.newPock(pock)
    }
}