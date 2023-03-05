import anychart from 'anychart';
import localMap from '../data/const/mapOutline';
import {useEffect, useState} from "react";

const mapDataContainer = anychart.data.set([]);
const map = anychart.choropleth(mapDataContainer);
map.geoData(localMap);
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
