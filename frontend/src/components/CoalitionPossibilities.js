import {partyColors} from "../data/const/mappings";

function CoalitionPossibilities({
  coalitionPossibilities
}) {
  if (!coalitionPossibilities || coalitionPossibilities.length === 0) {
    return <></>
  }
  return (
    <div className="ProgressBar">
      <h2>Koalitsioonid võimalused</h2>
      {coalitionPossibilities.map(possibility =>
        <CoalitionOption key={possibility} coalitionOption={possibility} />
      )}
    </div>
  );
}

export default CoalitionPossibilities;

function CoalitionOption({
  coalitionOption
}) {
  return (
    <div className="ProgressBarHolder">
      {coalitionOption.map((partyCode) =>
        <div key={partyCode} style={{width:(1/coalitionOption.length * 100) + "%", backgroundColor: partyColors[partyCode]}}  className="progressBarNoColor">
          <span className="coalitionLabel">{partyCode}</span>
        </div>
      )}
    </div>
  );
}
