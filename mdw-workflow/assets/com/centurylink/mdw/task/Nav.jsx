import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import NavLink from '../react/NavLink.jsx';

class Nav extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    const id = this.props.task.id;
    return (
      <div>
        <ul className="nav mdw-nav">
          <NavLink root={this.context.hubRoot + '/tasks/' + id} to="/">Task</NavLink>
          <NavLink root={this.context.hubRoot + '/tasks/' + id} to="/values">Values</NavLink>
          <NavLink root={this.context.hubRoot + '/tasks/' + id} to="/discussion">Discussion</NavLink>
          <NavLink root={this.context.hubRoot + '/tasks/' + id} to="/subtasks">Subtasks</NavLink>
          <NavLink root={this.context.hubRoot + '/tasks/' + id} to="/history">History</NavLink>
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
