import Map from "./components/Map";
import {useEffect, useState} from "react";
import {mockMapData} from "./data/const/mockData";
import {fetchAndProcess} from "./data/network/api";

function App() {

  const [mapData, setMapData] = useState(mockMapData)

  const processUpdate = () => {
    console.log("starting update")
    fetchAndProcess()
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
