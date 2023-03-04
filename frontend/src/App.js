import Map from "./components/Map";
import {useEffect, useState} from "react";
import {fetchAndProcess} from "./data/network/api";
import {processMapData} from "./data/processing/processData";

function App() {

  const [mapData, setMapData] = useState([])

  const processUpdate = async () => {
    console.log("starting update")
    const result = await fetchAndProcess()
    setMapData(processMapData(result.districts))
  }

  useEffect(() => {
    console.log("Setting up timer")
    const timer = setTimeout(() => {
      processUpdate()
    }, 30000);
    processUpdate()
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className="App">
      <h1>2023 Riigikogu Visimisõhtu</h1>
      <Map mapData={mapData}/>
    </div>
  );
}

export default App;
