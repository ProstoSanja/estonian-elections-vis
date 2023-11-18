import anychart from 'anychart';
import {useEffect, useState} from "react";
import getCurrentMode from "../data/const/modes";

const mapDataContainer = anychart.data.set([]);
const map = anychart.choropleth(mapDataContainer);
map.geoData(getCurrentMode().map);
map.background().fill("#282c34");
map.interactivity().selectionMode("none");
map.tooltip().useHtml(true);
map.credits().enabled(false);
map.tooltip().format((e) => {
  const parties = e.getData("parties")
  const voteStats = e.getData("voteStats")
  let result = "<span>Protokolle esitatud: " + voteStats.protocolsCounted + "/" + voteStats.protocolsTotal + "</span><br/>"
  parties.forEach((party) => {
    result += "<span>" + party.code + " - " + party.votes + " (" + (party.votes / voteStats.votesCounted * 100).toFixed(2) +"%)</span><br/>"
  })
  return result
});

function Map({
     mapData
 }) {
    const [firstRender, setFirstRender] = useState(true);
    useEffect(() => {
        if (!firstRender) {
            return
        }
        map.container("map-container")
        map.draw()
        setFirstRender(false)
    }, [firstRender])

    useEffect(() => {
        mapDataContainer.data(mapData);
    }, [mapData]);

    return (
        <div id="map-container" />
    );
}

export default Map;
