import './PersonCard.scss'
import {Component} from "react";

//let rawData = {"name":"Malle","lastname":"Rüütel","regNumber":457,"totalVotes":24668,"partyName":"KESK","elected":true};
//
// rawData.forEach((item) => {
//     item.color = partyColor(item.partyName)
// });

class HaalteMagnet extends Component {

    render() {
        const person = this.props.person;
        return (
            <div className="PersonCard">
                <div style={{backgroundColor: person.color}} className="colorLine"/>
                <div className="bio">
                    <span>{person.name} {person.lastname}</span>
                    <span>{person.totalVotes}</span>
                </div>
            </div>
        );
    }
}

export default HaalteMagnet;
