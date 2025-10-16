package com.thatguyalex.rk2023.infrastructure.classes.elections

import org.springframework.context.ApplicationEvent

data class ElectionsDataUpdatedEvent(
    val eventSource: Any,
    val electionType: ElectionType,
    val oldData: ProcessedResults,
    val newData: ProcessedResults
) : ApplicationEvent(eventSource)