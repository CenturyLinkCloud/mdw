import React from '../node/node_modules/react';

function HeaderLabel(props) {
  return (
    <div className="mdw-heading-label" style={props.style}>
      {props.title}
      {props.subtitle &&
        <span className="mdw-heading-sub">{props.subtitle}</span>
      }
    </div>
  );
}

export default HeaderLabel;
