import React, {Component} from '../node/node_modules/react';
import {Link} from '../node/node_modules/react-router-dom';

class Nav extends Component {
    
  constructor(...args) {
    super(...args);
  }
  
  render() {
    var id = 10193;
     
    return (
      <ul className="nav mdw-nav">
        <li><Link to={'/tasks/' + id }>Task</Link></li>
        <li><a href="">Discussion</a></li>
        <li><a href="">Subtasks</a></li>
        <li><Link to={'/tasks/' + id + '/history'}>History</Link></li>
        <li><a href="">Task List</a></li>
      </ul>
    );
  }
}

export default Nav;  