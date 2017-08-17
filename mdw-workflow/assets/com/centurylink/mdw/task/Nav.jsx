import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Link} from '../node/node_modules/react-router-dom';

class Nav extends Component {
    
  constructor(...args) {
    super(...args);
  }
  
  render() {
    const id = this.props.task.id;
    return (
      <div>
        <ul className="nav mdw-nav">
          <NavLink to="/" id={id}>Task</NavLink>
          <NavLink to="/values" id={id}>Values</NavLink>
          <NavLink to="/discussion" id={id}>Discussion</NavLink>
          <NavLink to="/subtasks" id={id}>Subtasks</NavLink>
          <NavLink to="/history" id={id}>History</NavLink>
        </ul>
        <ul className="nav mdw-nav">
          <li><a href={this.context.hubRoot + '/#/tasks'} target="_self">Task List</a></li>
        </ul>
      </div>
    );
  }
}

function NavLink(props, context) {
  const path = window.location.pathname;
  if (!path.startsWith('/'))
      path = '/' + path; // ie 11
  var dest = context.hubRoot + '/tasks/' + props.id;
  if (props.to != '/')
      dest += props.to;
  var cl = '';
  if (path == dest || (path == context.hubRoot + '/' && props.to == '/'))
    cl = 'mdw-active';
  return (
    <li className={cl}>
      <Link to={dest}>
        {props.children}
      </Link>
    </li>
  );  
}

Nav.contextTypes = NavLink.contextTypes = {
  hubRoot: PropTypes.string
} 

export default Nav;  