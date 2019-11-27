import React from '../node/node_modules/react';
import { Glyphicon } from '../node/node_modules/react-bootstrap';

function WorkflowZoom() {
  return (
    <div className="mdw-workflow-zoom">
      <a href="" title="Zoom In">
        <Glyphicon glyph="zoom-out"/>
      </a>
      <input type="range" min="20" max="200" defaultValue="100" />
      <a href="" title="Zoom In">
        <Glyphicon glyph="zoom-in"/>
      </a>
      <a href="" title="Close" className="mdw-warn mdw-close" style={{visibility:'hidden'}}>
        <Glyphicon glyph="remove-circle"/>
      </a>
    </div>
  );
}

export default WorkflowZoom;
