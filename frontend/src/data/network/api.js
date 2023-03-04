function fetchAndProcess(){
  return fetch("/api/data")
    .then((result) => result.json())
}

export {
  fetchAndProcess
}