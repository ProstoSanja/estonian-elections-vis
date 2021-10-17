import './App.css';
import {Component} from "react";
import anychart from 'anychart'

import localMap from './data/local'
import partyColor from './data/partyColor'
import HaalteMagnet from './widget/HaalteMagnet'
import PersonSearch from "./widget/PersonSearch";
import partyName from "./data/partyName";
import ProgressBar from "./widget/ProgressBar";

let mapData = anychart.data.set();
let topChartData = anychart.data.set();

let map = anychart.choropleth(mapData);
map.geoData(localMap);
map.background().fill("#282c34");
map.interactivity().selectionMode("none");
map.tooltip().format(function(e){
    let totalVotes = e.getData("totalVotes");
    let result =  "Votes counted: " + totalVotes +"\n";
    try {
        e.getData("partyData").forEach((party) => {
            result += party.name + " - " + party.votes + " (" + (party.votes / totalVotes * 100).toFixed(2) + "%)\n"
        });
    } catch (e) {
        console.log("failed to load tooltip", e);
    }
    return result;
});

let topChart = anychart.pie(topChartData);
topChart.background().fill("#282c34");
topChart.interactivity().selectionMode("none");
topChart.labels({position: "outside", fontColor:"white"});
topChart.connectorStroke({color: "white", thickness: 2, dash:"2 2"});
// topChart.legend().itemsLayout("vertical-expandable");
// topChart.legend().position("right");
topChart.title("Koondtulemus läbi Eesti")
topChart.title().fontColor("white");
topChart.title().fontWeight("bold");
topChart.title().fontSize("1.17em");

class App extends Component {

    interval = null;

    constructor(props) {
        super(props);
        this.state = {
            topPeople: [],
            allPeople: [],
            totalPolls: 0,
            donePolls: 0
        };
    }

    componentDidMount() {
        this.updateMap([]);
        map.container("map-container");
        map.draw();
        topChart.container("topChart-container");
        topChart.draw();
        this.fetchAndProcess('/api/countyData', this.updateMap);
        this.fetchAndProcess('/api/topPeopleData', this.updateTopPeople);
        this.fetchAndProcess('/api/person/list', this.updatePersonList);
        this.interval = setInterval(() => {
            this.fetchAndProcess('/api/countyData', this.updateMap);
            this.fetchAndProcess('/api/topPeopleData', this.updateTopPeople);
        }, 60000);
    }

    componentWillUnmount() {
        clearInterval(this.interval);
    }

    fetchAndProcess = (url, callback) => {
        fetch(url)
            .then(response => response.json())
            .then(data => callback(data))
            .catch(error => console.error(error))
    }

    updateMap = (newData) => {
        if (newData.length <= 0) {
            return;
        }
        var estoniaTotal = newData[0].partyData.map((party) => {
            return {
                // x: partyName(party.name),
                x: party.name,
                fill: partyColor(party.name),
                value: party.votes,
            }
        });

        topChartData.data(estoniaTotal);

        this.setState({
            totalPolls: newData[0].ballotStations,
            donePolls: newData[0].ballotStationsCounted
        });

        newData.forEach((item) => {
            item.fill = partyColor(item.leadingParty)
            item.stroke = "#282c34 0.2"
        });
        mapData.data(newData);
    }

    updateTopPeople = (newData) => {
        this.setState({
            topPeople: newData
        })
    }

    updatePersonList = (newData) => {
        this.setState({
            allPeople: newData
        })
    }

    render() {
        console.log("RENDER APP")
        return (
            <div className="App">
                <span>
                    <h2>Eesti KOV 2021 valimisõhtu</h2>
                </span>
                <div id="map-container"/>
                <div className="secondChart">
                    <div id="countBar-container">
                        <h3>Häälte lugemine</h3>
                        <span>Kokku loetud: {(this.state.donePolls / this.state.totalPolls * 100).toFixed(2)}%</span>
                        <span>Osakonniti: {this.state.donePolls}/{this.state.totalPolls}</span>
                        <br/>
                        <ProgressBar totalPolls={this.state.totalPolls} donePolls={this.state.donePolls}/>
                    </div>
                    <div id="topChart-container"/>
                </div>
                <span>
                    <h3>Häälte magnetid</h3>
                </span>
                <HaalteMagnet topPeople={this.state.topPeople}/>
                <span>
                    <h3>Kandidaadi Otsing</h3>
                </span>
                <PersonSearch allPeople={this.state.allPeople}/>
            </div>
        );
    }
}

export default App;
