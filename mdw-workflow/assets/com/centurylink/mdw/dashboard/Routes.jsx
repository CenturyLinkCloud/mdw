import React from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Route} from '../node/node_modules/react-router-dom';
// To add custom charts, override Routes.jsx and Index.jsx in custom UI package.
import Processes from './Processes.jsx';
import Requests from './Requests.jsx';

function Routes(props, context) {
  // Routes should match nav.json dashboard entries
  return(
    <div className="panel panel-default mdw-panel">
      <Route exact path={context.hubRoot + '/dashboard/processes'}
        render={(props) => <Processes {...props} />} />
      <Route exact path={context.hubRoot + '/dashboard/requests'}
        render={(props) => <Requests {...props} />} />
    </div>
  );
}

Routes.contextTypes = {
  hubRoot: PropTypes.string
};

export default Routes;
