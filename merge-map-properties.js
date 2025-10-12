const fs = require('fs');
const path = require('path');

// Read the source files
const geojsonPath = path.join(__dirname, 'frontend/src/data/const/maps/kov2025Map.json');
// const topojsonPath = path.join(__dirname, 'frontend/src/data/const/maps/kov2025MapTallinn.topo.json');
const topojsonPath = '/Users/alex/Downloads/eesti-tallinn.topo.json';

console.log('Reading files...');
const geojson = JSON.parse(fs.readFileSync(geojsonPath, 'utf8'));
const topojson = JSON.parse(fs.readFileSync(topojsonPath, 'utf8'));

// Create a lookup map by name from the geojson
const propertiesMap = {};
geojson.features.forEach(feature => {
  const name = feature.properties.name || feature.properties.REGION_NAME;
  if (name) {
    propertiesMap[name] = feature.properties;
  }
});

console.log(`Created lookup map with ${Object.keys(propertiesMap).length} entries`);

// Update topojson geometries
let matchedCount = 0;
let unmatchedCount = 0;
const unmatched = [];

topojson.objects.layer.geometries.forEach(geometry => {
  const name = geometry.properties.name;
  if (propertiesMap[name]) {
    // Replace properties entirely with the ones from geojson
    // geometry.properties = { ...propertiesMap[name] };
    geometry.properties = { ehakId: propertiesMap[name].id, name: name, layer: 'eesti-kov-2025-stylised-single-tallinn' };
    matchedCount++;
  } else {
    unmatchedCount++;
    unmatched.push(name);
  }
});

console.log(`\nMatched: ${matchedCount} geometries`);
console.log(`Unmatched: ${unmatchedCount} geometries`);
if (unmatched.length > 0) {
  console.log('Unmatched names:', unmatched);
}

// Write the updated topojson back
fs.writeFileSync(topojsonPath, JSON.stringify(topojson, null, 2));
console.log(`\n✓ Updated ${topojsonPath}`);

