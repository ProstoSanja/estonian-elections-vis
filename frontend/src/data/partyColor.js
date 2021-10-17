let partyColor = (partyname) => {
    switch(partyname) {
        case "KESK":
            return "#027D2B"
        case "REF":
            return "#FFDC00"
        case "SDE":
            return "#CE1126"
        case "IRL":
            return "#009DE0"
        case "EKRE":
            return "#996600"
        case "ROH":
            return "#60BE19"
        default:
            return "#F699CD"
    }
};
export default partyColor