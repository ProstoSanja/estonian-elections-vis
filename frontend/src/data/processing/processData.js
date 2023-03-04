import {partyColors} from "../const/mappings";

function processMapData(districts) {
  return districts.map((district) => {
    return {
      ...district,
      id: district.number,
      stroke: "#282c34 0.2",
      fill: partyColors[district.parties[0].code],
    }
  })
}

export { processMapData }