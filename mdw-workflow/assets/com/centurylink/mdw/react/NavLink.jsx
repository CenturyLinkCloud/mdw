import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Link} from '../node/node_modules/react-router-dom';

function NavLink(props, context) {
  var path = window.location.pathname;
  if (!path.startsWith('/'))
    path = '/' + path; // ie 11
  var dest = props.root ? props.root : '/';
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

NavLink.contextTypes = {
  hubRoot: PropTypes.string
};

export default NavLink;
