import './PersonCard.scss'
import {Component} from "react";
import partyColor from "../data/partyColor";

//let rawData = {"name":"Malle","lastname":"Rüütel","regNumber":457,"totalVotes":24668,"partyName":"KESK","elected":true};
//
// rawData.forEach((item) => {
//     item.color = partyColor(item.partyName)
// });

class PersonCard extends Component {

    render() {
        const person = this.props.person;
        return (
            <div className="PersonCard" onClick={() => {
                if (this.props.callback) {
                    this.props.callback(person)
                }
            }}>
                <div style={{backgroundColor: partyColor(person.partyName)}} className="colorLine"/>
                <div className="bio">
                    <span>{person.name} {person.lastname}</span>
                    {!this.props.hideVotes &&
                        <span className="voteCount">{person.totalVotes}</span>
                    }
                </div>
            </div>
        );
    }
}

export default PersonCard;
