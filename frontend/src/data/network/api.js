import getCurrentMode from "../const/modes";

function fetchAndProcess(){
  const env = getCurrentMode().apiEndpoint
  return fetch(`/api/data/${env}`)
    .then((result) => result.json())
}

export {
  fetchAndProcess
}