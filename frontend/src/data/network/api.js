function fetchAndProcess(){
  return fetch("/api/data/rk2023")
    .then((result) => result.json())
}

export {
  fetchAndProcess
}