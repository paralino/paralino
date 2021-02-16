package com.zbapp.location.repo.circle

import com.zbapp.location.models.circle.Circle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class CircleRepository {

    private val circles = MutableStateFlow(emptyList<Circle>())
    private val personalTrackingCircle get() = Circle.personalTracking
    private val publicSharingCircle get() = Circle.publicSharing

    private fun getAllCircles(): Flow<List<Circle>> {
        return circles.map {
            it + listOfNotNull(personalTrackingCircle, publicSharingCircle)
        }
    }

    fun getMyCircles(): Flow<List<Circle>> {
        return circles
    }

    fun getCircle(id: String): Flow<Circle?> {
        return circles.map { it.firstOrNull { it.id == id } }
    }

    fun createOrUpdateCircle(circle: Circle) {
        circles.value = circles.value.toMutableList().apply {
            removeAll { it.id == circle.id }
            add(circle)
        }
    }

    fun deleteCircle(circleId: String) {
        circles.value = circles.value.toMutableList().apply {
            removeAll { it.id == circleId }
        }
    }

}