import Map from "./components/Map";
import {useEffect, useState} from "react";
import {fetchAndProcess} from "./data/network/api";
import {processMapData} from "./data/processing/processData";
import TopCandidates from "./components/TopCandidates";

function App() {

  const [mapData, setMapData] = useState([])
  const [candidateData, setCandidateData] = useState([])

  const processUpdate = async () => {
    console.log("starting update")
    const result = await fetchAndProcess()
    console.log(result)
    setMapData(processMapData(result.districts))
    setCandidateData(result.candidates)
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
      <h2>Häälte magnetid</h2>
      <TopCandidates candidates={candidateData}/>
    </div>
  );
}

export default App;
