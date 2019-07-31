import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';

class DefNav extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    const hubRoot = this.context.hubRoot;
    const e2e = location.hash.startsWith('#/milestones/definitions/e2e/');
    const assetPath = location.hash.substring(e2e ? 29 : 25);
    return (
      <div>
        <ul className="nav mdw-nav">
          <li>
            <a href={hubRoot + '#/workflow/definitions/' + assetPath}>Definition</a>
          </li>
          <li className={e2e ? '' : 'mdw-active'}>
            <a href={hubRoot + '#/milestones/definitions/' + assetPath}>Milestones Def.</a>
          </li>
          <li className={e2e ? 'mdw-active' : ''}>
            <a href={hubRoot + '#/milestones/definitions/e2e/' + assetPath}>Everything</a>
          </li>
        </ul>
        <ul className="nav mdw-nav">
          <li><a href={hubRoot + '#/workflow/processes'}>Process List</a></li>
          <li><a href={hubRoot + '#/workflow/definitions'}>Definitions</a></li>
        </ul>
      </div>
    );
  }
}

DefNav.contextTypes = {
  hubRoot: PropTypes.string
};

export default DefNav;
