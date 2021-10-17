import './PersonSearch.scss'
import {Component} from "react";
import PersonCard from "./PersonCard";

class PersonSearch extends Component {

    callback = null;

    state = {
        search: '',
        searchHidden: false,
        selectedCandidate: null
    }

    selectCandidate = (candidate) => {
        this.setState({
            searchHidden: true,
            selectedCandidate: candidate
        })
        this.callback = setInterval(this.fetchCandidate, 60000);
    }

    componentWillUnmount() {
        clearInterval(this.callback);
    }


    fetchCandidate = () => {
        fetch("http://localhost:12345/api/person/"+ this.state.selectedCandidate.regNumber)
            .then(response => response.json())
            .then(data => this.setState({
                selectedCandidate: data
            }))
            .catch(error => console.error(error))
    }

    render() {
        const people = this.props.allPeople;
        const search = this.state.search;
        console.log("RENDER SEARCH")
        return (
            <div className="PersonSearch">
                <div className="searchField">
                    <input
                        value={search}
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
                                this.searchMatches(search, people).map(person =>
                                    <PersonCard person={person} hideVotes={true} callback={this.selectCandidate}/>
                                )
                            }
                        </div>
                    }
                </div>
                {this.state.selectedCandidate &&
                    <PersonCard person={this.state.selectedCandidate} large={true}/>
                }
            </div>
        );
    }

    searchMatches(query, list) {
        var searchTerms = query.split(' ');
        return list.filter(person => {
            for (const term of searchTerms) {
                if (!person.name.includes(term) && !person.lastname.includes(term)) {
                    return false;
                }
            }
            return true;
        }).slice(0,2);
    }
}

export default PersonSearch;
