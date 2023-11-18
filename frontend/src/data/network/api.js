function fetchAndProcess(){
  return fetch("/api/data/RK2023")
    .then((result) => result.json())
}

export {
  fetchAndProcess
}