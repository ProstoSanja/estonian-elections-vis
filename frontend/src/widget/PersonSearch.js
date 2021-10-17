import './PersonSearch.scss'
import {Component} from "react";
import PersonCard from "./PersonCard";

class PersonSearch extends Component {

    callback = null;

    state = {
        search: '',
        searchHidden: false,
        selectedCandidate: []
    }

    selectCandidate = (candidate) => {
        this.setState({
            search: '',
            searchHidden: true,
            selectedCandidate: this.state.selectedCandidate.push(candidate)
        })
        if (this.callback != null) {
            clearInterval(this.callback);
        }
        this.fetchCandidate()
        this.callback = setInterval(this.fetchCandidate, 60000);
    }

    componentWillUnmount() {
        clearInterval(this.callback);
    }


    fetchCandidate = () => {
        if (this.state.selectedCandidate.length <= 0) {
            return;
        }
        var baseUrl = "/api/person?names=";
        this.state.selectedCandidate.forEach(item => {
            baseUrl += item.name + item.lastname + ","
        })
        fetch(baseUrl)
            .then(response => response.json())
            .then(data => this.setState({
                selectedCandidate: data
            }))
            .catch(error => console.error(error))
    }

    render() {
        const people = this.props.allPeople;
        const search = this.state.search;
        return (
            <div className="PersonSearch">
                <div className="searchField">
                    <input
                        value={search}
                        placeholder={"Kandidaati nimi või perekonnanimi"}
                        onChange={event => {
                            this.setState({
                                search: event.target.value,
                                searchHidden: false
                            })
                        }}
                    />
                    {search.length > 3 && !this.state.searchHidden &&
                        <div className={"suggestions"}>
                            {
                                this.searchMatches(search, people).map((person, i) =>
                                    <PersonCard key={i} person={person} hideVotes={true} callback={this.selectCandidate}/>
                                )
                            }
                        </div>
                    }
                </div>
                <div className="searchResults">
                    {this.state.selectedCandidate.length > 0 && this.state.selectedCandidate.map((candidate, i) =>
                        <PersonCard key={i} person={candidate}/>
                    )}
                </div>
            </div>
        );
    }

    searchMatches(query, list) {
        var searchTerms = query.split(' ');
        return list.filter(person => {
            for (const term of searchTerms) {
                if (!person.name.toLowerCase().includes(term.toLowerCase()) && !person.lastname.toLowerCase().includes(term.toLowerCase())) {
                    return false;
                }
            }
            return true;
        }).slice(0,2);
    }
}

export default PersonSearch;
