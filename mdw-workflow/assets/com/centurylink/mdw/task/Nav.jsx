import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import NavLink from '../react/NavLink.jsx';

class Nav extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    var root = this.context.hubRoot + '/tasks/';
    if (this.props.task.id) {
      root += this.props.task.id + '/';
    }
    return (
      <div>
        <ul className="nav mdw-nav">
          <NavLink to={root} match={root}>Task</NavLink>
          <NavLink to={root + 'values'}>Values</NavLink>
          <NavLink to={root + 'discussion'}>Discussion</NavLink>
          <NavLink to={root + 'subtasks'}>Subtasks</NavLink>
          <NavLink to={root + 'history'}>History</NavLink>
        </ul>
        <ul className="nav mdw-nav">
          <li><a href={this.context.hubRoot + '/#/tasks'} target="_self">Task List</a></li>
        </ul>
      </div>
    );
  }
}

Nav.contextTypes = {
  hubRoot: PropTypes.string
};

export default Nav;
