import './PersonCard.scss'
import {getPartyColor} from "../data/const/mappings";

function PersonCard({
  candidate,
  callback
}) {
  return (
    <div style={callback && {cursor: "pointer", width: "initial"}} onClick={callback && (() => {callback(candidate.regNumber);})} className="PersonCard">
      <div style={{backgroundColor: getPartyColor(candidate.partyCode)}} className="colorLine"/>
      <div className="bio">
        <span>{candidate.forename} {candidate.surename}</span>
        {!callback &&
          <span className="voteCount">{candidate.votes}</span>}
      </div>
    </div>
  );
}

export default PersonCard;