import React from '../node/node_modules/react';

// items prop should have id, name (and optionally packageName/version and count/value)
function ChartLegend(props) {

  function getLabel(item) {
    var label = '';
    const found = props.items.find(it => it.id === item.id);
    if (found) {
      label += found.name;
      if (label.startsWith('http://') || label.startsWith('https://')) {
        label = new URL(label).pathname;
      }
      if (found.value || found.count) {
        label += ' (' + (found.count ? found.count : found.value) + ')';
      }
    }
    return label;
  }

  function getTitle(item) {
    var title = '';
    const found = props.items.find(it => it.id === item.id);
    if (found) {
      title += found.name;
      if (found.packageName) {
        title = found.packageName + '/' + title;
        if (found.version) {
          title += ' v' + found.version;
        }
      }
    }
    return title;
  }

  return (
    <ul className="mdw-chart-legend">
      {props.items &&
        props.items.map((item, i) => {
          let backgroundColor = item.color ? item.color : props.colors[i];
          let borderColor = item.borderColor ? item.borderColor : backgroundColor;
          return (
            <li key={i}>
              <span className="mdw-chart-legend-icon"
                style={{backgroundColor: backgroundColor, borderColor: borderColor}}>
              </span>
              <span className="mdw-chart-legend-text" title={getTitle(item)}>
                  {getLabel(item)}
              </span>
            </li>
          );
        })
      }
    </ul>
  );
}

export default ChartLegend;
