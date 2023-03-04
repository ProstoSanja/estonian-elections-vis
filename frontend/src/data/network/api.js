
function fetchAndProcess(){
  return fetch("/api/data")
    .then((result) => result.json())
    .then((text) => console.log(text))
}

export {
  fetchAndProcess
}