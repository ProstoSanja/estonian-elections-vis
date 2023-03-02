import Map from "./components/Map";
import {useState} from "react";
import {mockMapData} from "./data/mockData";

function App() {

    const [mapData, setMapData] = useState(mockMapData)

  return (
    <div className="App">
      <h1>2023 Riigikogu Visimisõhtu</h1>
        <Map mapData={mapData}/>
    </div>
  );
}

export default App;
