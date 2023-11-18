import rk2023Map from './maps/rk2023Map';
import kov2021Map from './maps/kov2021Map';

const modes = {
  RK2023: {
    apiEndpoint: "RK2023",
    title: "2023 Riigikogu Valimisõhtu",
    header: "RK2023 Tulemused",
    partyValueSelector: "mandates",
    mandatesDistributionPlaceholder: "{%value}",
    mandatesDistributionLocation: "inside",
    map: rk2023Map,
  },
  KOV2021: {
    apiEndpoint: "KOV2021",
    title: "Eesti KOV 2021 valimisõhtu",
    header: "KOV2021 Tulemused",
    partyValueSelector: "votes",
    mandatesDistributionPlaceholder: "{%votesPercentage}%",
    mandatesDistributionLocation: "outside",
    map: kov2021Map,
  },
}

const getCurrentMode = () => {
  if (window.location.href.indexOf("KOV2021") !== -1) {
    return modes["KOV2021"];
  }
  return modes["RK2023"]
}

export default getCurrentMode