import React from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Route} from '../node/node_modules/react-router-dom';
// To add custom charts, override Routes.jsx and Index.jsx in a custom UI package.
import Processes from './Processes.jsx';
import Tasks from './Tasks.jsx';
import Activities from './Activities.jsx';
import InboundRequests from './InboundRequests.jsx';
import OutboundRequests from './OutboundRequests.jsx';

import Temp from './Temp.jsx';

function Routes(props, context) {
  // Routes should match nav.json dashboard entries
  return(
    <div className="panel panel-default mdw-panel">
      <Route exact path={context.hubRoot + '/dashboard/processes'}
        render={(props) => <Processes {...props} />} />
      <Route exact path={context.hubRoot + '/dashboard/tasks'}
        render={(props) => <Tasks {...props} />} />
      <Route exact path={context.hubRoot + '/dashboard/activities'}
        render={(props) => <Activities {...props} />} />
      <Route exact path={context.hubRoot + '/dashboard/inboundRequests'}
        render={(props) => <InboundRequests {...props} />} />
      <Route exact path={context.hubRoot + '/dashboard/outboundRequests'}
        render={(props) => <OutboundRequests{...props} />} />


      <Route exact path={context.hubRoot + '/dashboard/temp'}
        render={(props) => <Temp{...props} />} />
    </div>
  );
}

Routes.contextTypes = {
  hubRoot: PropTypes.string
};

export default Routes;
