import PersonCard from "./PersonCard";

function TopCandidates({
  candidates
}) {
  return (
    <div className="TopCandidates">
      {candidates.length > 0 && candidates.slice(0, 50).map((candidate) => {
        return <PersonCard key={candidate.regNumber} candidate={candidate}/>
      })}
    </div>
  );
}

export default TopCandidates;