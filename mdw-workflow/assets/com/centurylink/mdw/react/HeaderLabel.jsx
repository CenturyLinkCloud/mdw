import React from '../node/node_modules/react';

function HeaderLabel(props) {
  return (
    <div className="mdw-heading-label" style={props.style}>
      {props.title}
    </div>
  );
}

export default HeaderLabel;
