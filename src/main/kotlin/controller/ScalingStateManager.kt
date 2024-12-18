package com.github.abyssnlp.controller

import java.util.concurrent.ConcurrentHashMap

class ScalingStateManager {
    private val stateMap = ConcurrentHashMap<String, HashMap<String, PreviousScaleState>>()

    fun getState(resourceName: String, consumerGroup: String): PreviousScaleState {
        return stateMap
            .getOrPut(resourceName) { HashMap() }
            .getOrDefault(consumerGroup, PreviousScaleState(null, 0L))
    }

    fun updateState(resourceName: String, consumerGroup: String, state: PreviousScaleState) {
        stateMap.computeIfPresent(resourceName) { _, innerMap ->
            innerMap[consumerGroup] = state
            innerMap
        } ?: run {
            val innerMap = HashMap<String, PreviousScaleState>()
            innerMap[consumerGroup] = state
            stateMap[resourceName] = innerMap
        }
    }

    override fun toString(): String {
        val stateMapRepr = stateMap.entries.joinToString(
            separator = ", ",
            prefix = "{",
            postfix = "}"
        ) { (resourceName, innerMap) ->
            "\"$resourceName\": ${
                innerMap.entries.joinToString(
                    separator = ", ",
                    prefix = "{",
                    postfix = "}"
                ) { (consumerGroup, state) ->
                    "\"$consumerGroup\": $state"
                }
            }"
        }
        return "ScalingStateManager(stateMap=$stateMapRepr)"
    }
}