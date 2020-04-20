import React from '../node/node_modules/react';
import mobile from './mobile';

function HeaderButtons(props) {
  return (
    <div className={mobile.isMobile() ? 'mdw-buttons-mobile' : 'mdw-buttons'}>
      {props.children}
    </div>
  );
}

export default HeaderButtons;
