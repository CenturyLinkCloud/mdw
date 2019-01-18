import React from '../node/node_modules/react';
import HeaderLabel from './HeaderLabel.jsx';

function PanelHeader(props) {
  return (
    <div className="panel-heading mdw-heading">
      {props.title &&
        <HeaderLabel title={props.title} />
      }
      {props.children}
    </div>
  );
}

export default PanelHeader;
