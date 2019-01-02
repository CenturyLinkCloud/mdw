import React from '../node/node_modules/react';

function Footer(props) { // eslint-disable-line no-unused-vars

  return (
    <div id="mdw-footer">
      <div className="mdw_footer">
        MDW {$mdwVersion}
        <br/>
        Copyright &#169; 2019 CenturyLink, Inc.
      </div>
    </div>
  );
}

export default Footer;
