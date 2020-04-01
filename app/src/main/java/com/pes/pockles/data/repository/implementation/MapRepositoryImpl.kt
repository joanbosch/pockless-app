package com.pes.pockles.data.repository.implementation

import androidx.lifecycle.LiveData
import com.pes.pockles.data.Resource
import com.pes.pockles.data.repository.BaseRepository
import com.pes.pockles.domain.repositories.MapRepository
import com.pes.pockles.model.Location
import com.pes.pockles.model.Pock
import io.reactivex.functions.Function

class MapRepositoryImpl : BaseRepository(), MapRepository {

    override fun getPocks(loc: Location): LiveData<Resource<List<Pock>>> {
        return callApi(Function { apiService -> apiService.getNearPocks(loc.latitude, loc.longitude) })

    }
}