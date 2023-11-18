import anychart from 'anychart';
import {useEffect, useState} from "react";
import getCurrentMode from "../data/const/modes";

const pieDataContainer = anychart.data.set([]);
const pie = anychart.pie(pieDataContainer);
pie.background().fill("#282c34");
pie.interactivity().selectionMode("none");
pie.labels().useHtml(true);
pie.labels().fontColor("white");
pie.labels({position: getCurrentMode().mandatesDistributionLocation});
pie.labels().format(getCurrentMode().mandatesDistributionPlaceholder);
pie.innerRadius("40%");
pie.legend(false);
pie.credits().enabled(false);
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
      <h2>Mandaatide jaotus</h2>
      <div id="mandates-container" style={{height: "300px"}}/>
    </div>
  );
}

export default MandatesDistribution;