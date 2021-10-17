import './App.css';
import {Component} from "react";
import anychart from 'anychart'

import localMap from './data/local'
import partyColor from './data/partyColor'
import HaalteMagnet from './widget/HaalteMagnet'
import PersonSearch from "./widget/PersonSearch";

let data = anychart.data.set();

let map = anychart.choropleth(data);
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

class App extends Component {

    interval = null;

    constructor(props) {
        super(props);
        this.state = {
            topPeople: [],
            allPeople: []
        };
    }

    componentDidMount() {
        this.updateMap([]);
        map.container("map-container");
        map.draw();
        this.fetchAndProcess('http://192.168.0.15:12345/api/countyData', this.updateMap);
        this.fetchAndProcess('http://192.168.0.15:12345/api/topPeopleData', this.updateTopPeople);
        this.fetchAndProcess('http://192.168.0.15:12345/api/person/list', this.updatePersonList);
        this.interval = setInterval(() => {
            this.fetchAndProcess('http://192.168.0.15:12345/api/countyData', this.updateMap);
            this.fetchAndProcess('http://192.168.0.15:12345/api/topPeopleData', this.updateTopPeople);
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

    updateMap(newData) {
        newData.forEach((item) => {
            item.fill = partyColor(item.leadingParty)
            item.stroke = "#282c34 0.2"
        });
        data.data(newData);
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
