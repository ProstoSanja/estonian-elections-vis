let partyName = (partyname) => {
    switch(partyname) {
        case "KESK":
            return "Eesti Keskerakond"
        case "REF":
            return "Eesti Reformierakond"
        case "SDE":
            return "Sotsiaaldemokraatlik Erakond"
        case "IRL":
            return "Erakond Isamaa ja Res Publica Liit"
        case "EKRE":
            return "Eesti Konservatiivne Rahvaerakond"
        case "ROH":
            return "Erakond Eestimaa Rohelised"
        default:
            return partyname
    }
};
export default partyName