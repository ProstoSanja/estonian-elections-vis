package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionsDataUpdatedEvent
import com.thatguyalex.rk2023.infrastructure.classes.elections.ProcessedResults
import com.thatguyalex.rk2023.infrastructure.classes.helpers.PartyCodeType
import com.thatguyalex.rk2023.infrastructure.classes.helpers.getShortPartyCode
import com.thatguyalex.rk2023.infrastructure.classes.push.ElectionPushMessage
import com.thatguyalex.rk2023.infrastructure.classes.push.PushMessage
import com.thatguyalex.rk2023.infrastructure.classes.push.PushTopicType
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.round

@Service
class ElectionsNotificationsProcessingApplication(
    private val pushNotificationApplication: PushNotificationApplication
) {
    private val logger = LoggerFactory.getLogger(ElectionsNotificationsProcessingApplication::class.java)

    @EventListener
    fun onMqttMessageReceived(event: ElectionsDataUpdatedEvent) {
        process(event.oldData, event.newData, event.electionType)
    }

    fun process(old: ProcessedResults, new: ProcessedResults, electionType: ElectionType) {
        logger.info("Start processing notifications for election $electionType")
        val messages = processDistricts(old, new, electionType) + processCandidates(old, new, electionType)
        pushNotificationApplication.sendElectionMessages(messages)
        logger.info("Finished processing notifications for election $electionType")
    }

    fun processCandidates(
        old: ProcessedResults,
        new: ProcessedResults,
        electionType: ElectionType
    ): List<ElectionPushMessage> {
        val currentTime = ZonedDateTime.now(ZoneId.of("Europe/Tallinn")).format(DateTimeFormatter.ofPattern("HH:mm"))

        return new.candidatesList
            .associateWith { newCandidate -> old.candidates[newCandidate.uniqueId] }
            .mapNotNull { (newCandidate, oldCandidate) ->
                if (oldCandidate == null) return@mapNotNull null
                if (oldCandidate.votes >= newCandidate.votes) return@mapNotNull null
                val statsForCandidateRegion = new.districts[newCandidate.districtNumber]?.voteStats
                    ?.let { "(${it.protocolsCounted}/${it.protocolsTotal} jaoskonda)" }
                ElectionPushMessage(
                    electionType,
                    PushTopicType.CANDIDATE,
                    newCandidate.uniqueId,
                    "${newCandidate.forename} ${newCandidate.surename}",
                    body = "$currentTime - ${newCandidate.votes} hääli $statsForCandidateRegion",
                )
            }
    }

    fun processDistricts(
        old: ProcessedResults,
        new: ProcessedResults,
        electionType: ElectionType
    ): List<ElectionPushMessage> {
        val currentTime = ZonedDateTime.now(ZoneId.of("Europe/Tallinn")).format(DateTimeFormatter.ofPattern("HH:mm"))

        return new.districtsList
            .filter { it.number != 0 } // TODO: KOV only: Filter out entire country for local elections to avoid spam
            .associateWith { newDistrict -> old.districts[newDistrict.number] }
            .mapNotNull { (newDistrict, oldDistrict) ->
                if (oldDistrict == null) return@mapNotNull null
                if (oldDistrict.voteStats.protocolsCounted >= newDistrict.voteStats.protocolsCounted) return@mapNotNull null
                val regionStats = "(${newDistrict.voteStats.protocolsCounted}/${newDistrict.voteStats.protocolsTotal} jaoskonda)"
                val message = newDistrict.parties
                    .associate { party -> party.code to round(party.votes.toDouble() * 100 / newDistrict.voteStats.votesCounted).toInt() }
                    .filterValues { it > 5 }
                    .map { "${getShortPartyCode(it.key, PartyCodeType.ULTRA)}:${it.value}%" }
                    .joinToString(" ")
                ElectionPushMessage(
                    electionType,
                    PushTopicType.REGION,
                    newDistrict.number.toString(),
                    "${newDistrict.name} $regionStats",
                    "$currentTime - $message",
                )
            }

    }
}