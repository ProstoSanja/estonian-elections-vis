import {getPartyColor} from "../const/mappings";
import {processName} from "./processText";

function processMapData(districts) {
  return districts.map((district) => {
    return {
      ...district,
      id: district.number,
      stroke: "#282c34 0.2",
      fill: getPartyColor(district.parties[0]?.code),
      parties: processPartyData(district.parties),
    }
  })
}

function processPartyData(parties) {
  return parties.map((party) => {
    return {
      ...party,
      id: party.code,
      stroke: "#282c34 0.2",
      fill: getPartyColor(party.code),
      value: party.mandates,
    }
  })
}

function processCandidateData(candidates) {
  return candidates.map((candidate) => {
    return {
      ...candidate,
      tokenizedName: processName(candidate.forename).concat(processName(candidate.surename)).join("~"),
    }
  })
}

export { processMapData, processPartyData, processCandidateData }