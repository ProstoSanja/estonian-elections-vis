function ProgressBar({
  globalRegion
}) {
  if (!globalRegion) {
    return <></>
  }

  const stats = globalRegion.voteStats;
  const protocolsNotCounter = stats.protocolsTotal - stats.protocolsCounted;

  const donePercent = stats.protocolsCounted / stats.protocolsTotal * 100;
  const notDonePercent = protocolsNotCounter / stats.protocolsTotal * 100;

  return (
    <div className="ProgressBar">
      <h3>Häälte lugemine</h3>
      <span>Kokku loetud: {donePercent.toFixed(2)}%</span>
      <span>Protokolle esitatud: {stats.protocolsCounted}/{stats.protocolsTotal}</span>
      <br/>
      <div className="ProgressBarHolder">
        <div style={{width:donePercent + "%"}} className="progressBarCounted"/>
        <div style={{width:notDonePercent + "%"}}  className="progressBarNotCounted"/>
      </div>
    </div>
  );
}

export default ProgressBar;