import './ProgressBar.scss'
import {Component} from "react";

class HaalteMagnet extends Component {

    render() {
        var totalPolls = this.props.totalPolls;
        var donePolls = this.props.donePolls;
        var undonePolls = totalPolls - donePolls;

        var donePercent = donePolls / totalPolls * 100;
        var undonePercent = undonePolls / totalPolls * 100;
        console.log(donePercent, undonePercent)
        return (
            <div className="ProgressBarHolder">
                <div style={{width:donePercent + "%"}} className="progressBarCounted"/>
                <div style={{width:undonePercent + "%"}}  className="progressBarNotCounted"/>
            </div>
        );
    }
}

export default HaalteMagnet;
