import React from '../node/node_modules/react';

function HeaderButtons(props) {
  return (
    <div className="mdw-buttons">
      {props.children}
    </div>
  );
}

export default HeaderButtons;
