import type { Candidate } from './api-types'

export const partyColorLookup: Map<string, string> = new Map([
  ["IE", "#009DE0"],
  ["SDE", "#d21b2f"],
  ["EKRE", "#29477c"],
  ["REF", "#ffd133"],
  ["KESK", "#027D2B"],

  ["EE200", "#F699CD"],
  ["PP", "#ff781b"],
  ["EERK", "#8b4513"],

  ["ROH", "#79ff0e"],
  ["EÜVP", "#996600"],
  ["ÜKSIK", "#8c8c8c"],
])

export const unknownPartyColor = '#9671ab'

export const getPartyColor = (partyCode: string) => {
  return partyColorLookup.get(partyCode) || unknownPartyColor
}

export const ultraShortPartyCodeLookup: Map<string, string> = new Map([
  ["IE", "IE"],
  ["SDE", "SD"],
  ["EKRE", "EK"],
  ["REF", "RE"],
  ["KESK", "KE"],

  ["EE200", "EE"],
  ["PP", "PP"],
  ["EERK", "ER"],
  ["ROH", "RO"],
  ["VAL_LIIDUD", "VL"],
])

export const veryShortPartyCodeLookup: Map<string, string> = new Map([
  ["IE", "ISA"],
  ["SDE", "SDE"],
  ["EKRE", "EKRE"],
  ["REF", "REF"],
  ["KESK", "KESK"],

  ["EE200", "200"],
  ["PP", "PP"],
  ["EERK", "ERK"],
  ["ROH", "ROH"],
  ["VAL_LIIDUD", "VAL"],
])

export const getShortPartyCode = (partyCode: string, type: 'ultra' | 'very' = 'ultra') => {
  if (type === 'ultra') return ultraShortPartyCodeLookup.get(partyCode) || partyCode.replace(/^VL|V/, '').slice(0, 2)
  return veryShortPartyCodeLookup.get(partyCode) || partyCode.replace(/^VL|V/, '').slice(0, 3)
}

export const candidateUniqueId = (candidate: Candidate) => {
  return `${candidate.primaryDistrictNumber}-${candidate.regNumber}`
}
