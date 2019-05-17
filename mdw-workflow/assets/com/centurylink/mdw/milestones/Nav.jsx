import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import NavLink from '../react/NavLink.jsx';

class Nav extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    const isDef = location.hash.startsWith('#/milestones/definitions');
    var milestoneRoot = this.context.hubRoot + '/milestones/';
    if (!isDef && this.props.milestone.masterRequestId) {
        milestoneRoot += this.props.milestone.masterRequestId + '/';
    }
    return (
      <div>
          {!isDef &&
            <ul className="nav mdw-nav">
              <NavLink to={milestoneRoot} match={milestoneRoot}>Milestones</NavLink>
              <NavLink to={milestoneRoot + 'timeline'}>Timeline</NavLink>
            </ul>
          }
          {isDef &&
            <ul className="nav mdw-nav">
              <li className="mdw-active"><a>Milestones</a></li>
            </ul>
          }
        <ul className="nav mdw-nav">
          <li><a href={this.context.hubRoot + '/#/milestones'} target="_self">Milestones List</a></li>
        </ul>
      </div>
    );
  }
}

Nav.contextTypes = {
  hubRoot: PropTypes.string
};

export default Nav;
