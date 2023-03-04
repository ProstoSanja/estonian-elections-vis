import PersonCard from "./PersonCard";
import {useState} from "react";

function TopCandidates({
  candidates
}) {
  const [showNumber, setShowNumber] = useState(49);

  return (
    <div className="TopCandidates">
      {candidates.length > 0 && candidates.slice(0, showNumber).map((candidate) => {
        return <PersonCard key={candidate.regNumber} candidate={candidate}/>
      })}
      <button onClick={() => {setShowNumber(showNumber+25);}}>Näita veel</button>
    </div>
  );
}

export default TopCandidates;