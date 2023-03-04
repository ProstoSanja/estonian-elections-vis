import anychart from 'anychart';
import localMap from '../data/const/mapOutline';
import {useEffect, useState} from "react";

const mapDataContainer = anychart.data.set([]);
const map = anychart.choropleth(mapDataContainer);
map.geoData(localMap);
map.background().fill("#282c34");
map.interactivity().selectionMode("none");

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
        console.log("mapData");
    }, [mapData]);

    return (
        <div id="map-container" />
    );
}

export default Map;
