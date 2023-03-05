import Map from "./components/Map";
import {useEffect, useState} from "react";
import {fetchAndProcess} from "./data/network/api";
import {processCandidateData, processMapData} from "./data/processing/processData";
import TopCandidates from "./components/TopCandidates";
import ProgressBar from "./components/ProgressBar";
import MandatesDistribution from "./components/MandatesDistribution";
import SearchCandidates from "./components/SearchCandidates";

function App() {

  const [mapData, setMapData] = useState([])
  const [candidateData, setCandidateData] = useState([])

  const processUpdate = async () => {
    console.log("starting update")
    const result = await fetchAndProcess()
    setMapData(processMapData(result.districts))
    setCandidateData(processCandidateData(result.candidates))
  }

  useEffect(() => {
    console.log("Setting up timer")
    const timer = setTimeout(() => {
      processUpdate()
    }, 30000);
    processUpdate()
    return () => clearTimeout(timer);
  }, []);

  const globalRegion = mapData.find((it) => it.id === 0);

  console.log(mapData, candidateData)
  return (
    <div className="App">
      <h1>2023 Riigikogu Visimisõhtu</h1>
      <Map mapData={mapData}/>
      <div className={"SplitRow"}>
        <ProgressBar globalRegion={globalRegion} />
        <MandatesDistribution globalRegion={globalRegion} />
      </div>
      <div className={"SplitRow"}>
      </div>
      <h2>Häälte magnetid</h2>
      <TopCandidates candidates={candidateData}/>
      <h2>Kandidaatide otsing</h2>
      <SearchCandidates candidates={candidateData} />
    </div>
  );
}

export default App;
