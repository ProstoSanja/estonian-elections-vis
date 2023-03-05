import PersonCard from "./PersonCard";
import {useState} from "react";
import {processName} from "../data/processing/processText";

function SearchCandidates({
  candidates
}) {
  const [addedCandidates, setAddedCandidates] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");

  const searchMatches = () => {
    const searchTokens = processName(searchTerm);
    return candidates.filter(person => {
      for (const token of searchTokens) {
        if (person.regNumber === parseInt(token)) {
          return true;
        }
        if (!person.tokenizedName.includes(token)) {
          return false;
        }
      }
      return true;
    }).slice(0,3);
  }

  const selectCandidate = (regNumber) => {
    setSearchTerm("")
    setAddedCandidates([].concat(addedCandidates, regNumber))
  }

  return (
    <div className="PersonSearch">
      <div className="searchField">
        <input
          value={searchTerm}
          placeholder={"Kandidaati nimi, perekonnanimi või number"}
          onChange={event => {
            setSearchTerm(event.target.value)
          }}
        />
        {searchTerm.length > 2 &&
          <div className={"suggestions"}>
            {
              searchMatches().map((candidate) =>
                <PersonCard key={candidate.regNumber} candidate={candidate} callback={selectCandidate}/>
              )
            }
          </div>
        }
      </div>
      <div className="SearchResults">
        {candidates.length > 0 && candidates.filter((candidate) => {
          return addedCandidates.indexOf(candidate.regNumber) !== -1
        }).map((candidate) => {
          return <PersonCard key={candidate.regNumber} candidate={candidate}/>
        })}
      </div>
    </div>
  );
}

export default SearchCandidates;