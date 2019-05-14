import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import NavLink from '../react/NavLink.jsx';

class Nav extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    var root = this.context.hubRoot + '/milestones/';
    if (this.props.milestone.masterRequestId) {
      root += this.props.milestone.masterRequestId + '/';
    }
    return (
      <div>
        <ul className="nav mdw-nav">
          <NavLink to={root} match={root}>Milestones</NavLink>
          <NavLink to={root + 'gantt'}>Gantt</NavLink>
        </ul>
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
