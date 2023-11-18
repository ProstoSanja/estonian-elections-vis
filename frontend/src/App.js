import Map from "./components/Map";
import {useEffect, useState} from "react";
import {fetchAndProcess} from "./data/network/api";
import {processCandidateData, processMapData} from "./data/processing/processData";
import TopCandidates from "./components/TopCandidates";
import ProgressBar from "./components/ProgressBar";
import MandatesDistribution from "./components/MandatesDistribution";
import SearchCandidates from "./components/SearchCandidates";
import CoalitionPossibilities from "./components/CoalitionPossibilities";
import getCurrentMode from "./data/const/modes";

function App() {

  const [mapData, setMapData] = useState([])
  const [candidateData, setCandidateData] = useState([])
  const [coalitionPossibilities, setCoalitionPossibilities] = useState([])

  const processUpdate = async () => {
    console.log("starting update")
    const result = await fetchAndProcess()
    setMapData(processMapData(result.districts, 'mandates'))
    setCandidateData(processCandidateData(result.candidates))
    setCoalitionPossibilities(result.coalitionPossibilities)
  }

  useEffect(() => {
    console.log("Setting up timer")
    const timer = setInterval(() => {
      processUpdate()
    }, 60000);
    processUpdate()
    return () => clearInterval(timer);
  }, []);

  const globalRegion = mapData.find((it) => it.id === 0);

  // console.log("mapData", mapData, "candidateData", candidateData)
  return (
    <div className="App">
      <h1>{getCurrentMode().title}</h1>
      <Map mapData={mapData}/>
      {(!globalRegion?.voteStats?.evotesCounted && globalRegion?.voteStats?.protocolsCounted === 0) ?
        <div>
          <h3 style={{maxWidth: "90vw"}}>Infot kuvatakse kohe pärast esimeste häälte lugemist</h3>
        </div> :
        <>
          <div className={"SplitRow"}>
            <ProgressBar globalRegion={globalRegion}/>
            <CoalitionPossibilities coalitionPossibilities={coalitionPossibilities} />
            <MandatesDistribution globalRegion={globalRegion}/>
          </div>
          <div className={"SplitRow"}>
          </div>
          <h2>Häälte magnetid</h2>
          <TopCandidates candidates={candidateData}/>
          <h2>Kandidaatide otsing</h2>
          <SearchCandidates candidates={candidateData}/>
        </>
      }
      <footer>
        <br/>
        <br/>
        <br/>
        <span>
          Arhiiv:
          <a rel="noreferrer" target="_blank" href="/RK2023">RK2023</a>
          <a rel="noreferrer" target="_blank" href="/KOV2021">KOV2021</a>
        </span>
        <br/>
        <br/>
        <br/>
        <br/>
        <span>© 2023 Aleksandr Tšernõh:
          <a rel="noreferrer" target="_blank" href="https://www.linkedin.com/in/prostosanja/">Contact</a>
          <a rel="noreferrer" target="_blank" href="https://github.com/ProstoSanja/estonian-elections-vis/">Source</a>
        </span>
        <br/>
        <br/>
        <br/>
      </footer>
    </div>
  );
}

export default App;
