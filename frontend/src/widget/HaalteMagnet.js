import './HaalteMagnet.scss'
import {Component} from "react";

import PersonCard from "./PersonCard";

class HaalteMagnet extends Component {

    render() {
        return (
            <div className="HaalteMagnet">
                {this.props.topPeople.map((person, i) => {
                    return <PersonCard key={i} index={i} person={person} hideVotes={false}/>
                })}
            </div>
        );
    }
}

export default HaalteMagnet;
