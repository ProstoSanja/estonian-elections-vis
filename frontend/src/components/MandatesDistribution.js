import anychart from 'anychart';
import {useEffect, useState} from "react";

const pieDataContainer = anychart.data.set([]);
const pie = anychart.pie(pieDataContainer);
pie.background().fill("#282c34");
pie.interactivity().selectionMode("none");
pie.labels().format("{%value}");
pie.innerRadius("40%");
pie.legend(false);
// map.tooltip().useHtml(true);

function MandatesDistribution({
  globalRegion
}) {
  const [firstRender, setFirstRender] = useState(true);
  useEffect(() => {
    if (!firstRender) {
      return
    }
    pie.container("mandates-container")
    pie.draw()
    setFirstRender(false)
  }, [firstRender])

  useEffect(() => {
    if (globalRegion?.parties) {
      pieDataContainer.data(globalRegion.parties);
    }
  }, [globalRegion]);

  return (
    <div className="MandatesDistribution">
      <h3>Mandaatide jaotus</h3>
      <div id="mandates-container" style={{height: "300px"}}/>
    </div>
  );
}

export default MandatesDistribution;