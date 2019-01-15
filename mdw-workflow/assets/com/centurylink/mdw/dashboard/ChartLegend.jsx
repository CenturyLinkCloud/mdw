import React from '../node/node_modules/react';

function ChartLegend(props) {

  function getLabel(sel) {
    var label = '';
    const top = props.tops.find(t => t.id === sel.id);
    if (top) {
      label += top.name;
      if (top.value) {
        label += ' (' + top.value + ')';
      }
    }
    return label;
  }

  function getTitle(sel) {
    var title = '';
    const top = props.tops.find(t => t.id === sel.id);
    if (top) {
      title += top.name;
      if (top.packageName) {
        title = top.packageName + '/' + title;
        if (top.version) {
          title += ' v' + top.version;
        }
      }
    }
    return title;
  }

  return (
    <ul className="mdw-chart-legend">
      {props.selected &&
        props.selected.map((sel, i) => {
          return (
            <li key={i}>
              <span className="mdw-chart-legend-icon"
                style={{backgroundColor: props.colors[i], borderColor: props.colors[i]}}>
              </span>
              <span className="mdw-chart-legend-text" title={getTitle(sel)}>
                  {getLabel(sel)}
              </span>
            </li>
          );
        })
      }
    </ul>
  );
}

export default ChartLegend;
