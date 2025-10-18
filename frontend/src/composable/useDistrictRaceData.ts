import { computed, type Ref } from 'vue'
import { getPartyColor } from '@/data/data-lookups'
import type { District, Party, VoteStats } from '@/data/api-types'

export interface VoteStatsWithEVotes extends VoteStats {
  originalProtocolsTotal: number
  originalProtocolsCounted: number
  totalMandates: number
  protocolsCountedShare: number
  votesPerProtocol: number
  votesTotal: number
  uncountedVotes: number
  uncountedVotesShare: number
}

export interface PartyWithParticipation extends Party {
  color: string
  shareOfCountedVotes: number
  shareOfTotalVotes: number
}

export const useDistrictRaceData = (district: Ref<District | undefined>) => {
  const voteStatsWithEVotes = computed<VoteStatsWithEVotes | undefined>(() => {
    if (!district.value) return undefined
    const eVotesAsProtocols = district.value.voteStats.protocolsTotal
    const protocolsTotal = district.value.voteStats.protocolsTotal + eVotesAsProtocols
    const protocolsCounted = district.value.voteStats.protocolsCounted + (district.value.voteStats.evotesCounted ? eVotesAsProtocols : 0)
    const protocolsCountedShare = protocolsCounted / protocolsTotal
    const votesPerProtocol = district.value.voteStats.votesCounted / protocolsCounted
    const votesTotal = votesPerProtocol * protocolsTotal
    const uncountedVotes = votesTotal - district.value.voteStats.votesCounted
    const uncountedVotesShare = uncountedVotes / votesTotal
    return {
      evotesCounted: district.value.voteStats.evotesCounted,
      votesCounted: district.value.voteStats.votesCounted,
      totalMandates: district.value.totalMandates,
      originalProtocolsTotal: district.value.voteStats.protocolsTotal,
      originalProtocolsCounted: district.value.voteStats.protocolsCounted,
      protocolsTotal,
      protocolsCounted,
      protocolsCountedShare,
      votesPerProtocol,
      votesTotal,
      uncountedVotes,
      uncountedVotesShare
    }
  })

  const partiesWithParticipation = computed<PartyWithParticipation[]>(() => {
    if (!district.value || !voteStatsWithEVotes.value) return []
    return district.value.parties.map(party => {
      const shareOfCountedVotes = party.votes / district.value!.voteStats.votesCounted
      const shareOfTotalVotes = party.votes / voteStatsWithEVotes.value!.votesTotal
      return {
        ...party,
        color: getPartyColor(party.code),
        shareOfCountedVotes,
        shareOfTotalVotes
      }
    })
  })

  return {
    voteStatsWithEVotes,
    partiesWithParticipation
  }
}
