import './PersonCard.scss'
import {partyColors} from "../data/const/mappings";

function PersonCard({
  candidate
}) {
  return (
    <div className="PersonCard">
      <div style={{backgroundColor: partyColors[candidate.partyCode]}} className="colorLine"/>
      <div className="bio">
        <span>{candidate.forename} {candidate.surename}</span>
        <span className="voteCount">{candidate.votes}</span>
      </div>
    </div>
  );
}

export default PersonCard;