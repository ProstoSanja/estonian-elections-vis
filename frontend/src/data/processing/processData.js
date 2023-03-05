import {partyColors} from "../const/mappings";

function processMapData(districts) {
  return districts.map((district) => {
    return {
      ...district,
      id: district.number,
      stroke: "#282c34 0.2",
      fill: partyColors[district.parties[0].code],
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
      fill: partyColors[party.code],
      value: party.mandates,
    }
  })
}

export { processMapData, processPartyData }