import React from '../node/node_modules/react';

function Footer(props) { // eslint-disable-line no-unused-vars

  return (
    <div id="mdw-footer">
      <div className="mdw_footer">
        {$mdwAppId && $mdwAppId !== 'Unknown' && $mdwAppId !== 'mdw6' &&
          <span>
            {$mdwAppId}
            {$mdwAppVersion && $mdwAppVersion != 'Unknown' &&
              <span> {$mdwAppVersion}</span>
            }
            <br/>
          </span>
        }
        MDW {$mdwVersion}
        <br/>
        Copyright &#169; 2020 CenturyLink, Inc.
      </div>
    </div>
  );
}

export default Footer;
