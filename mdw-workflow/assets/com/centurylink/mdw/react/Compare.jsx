import React from '../node/node_modules/react';
import ply from '../node/node_modules/ply-ct';
import CodeBlock from './CodeBlock.jsx';

function Compare(props) {
  const compare = ply.compare;
  var markers = compare.getMarkers(props.diffs, props.lines);
  markers = markers.map(marker => {
    return {
      start: marker.start,
      end: marker.end,
      className: marker.ignored ? props.ignoredClass : props.diffClass
    };
  });

  return (
    <CodeBlock language={props.language} code={props.code} lineNumbers={true} 
      markers={markers} />
  );
}

export default Compare;
