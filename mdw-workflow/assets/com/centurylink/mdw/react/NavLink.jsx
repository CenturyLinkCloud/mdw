import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Link} from '../node/node_modules/react-router-dom';

function NavLink(props, context) {
  var path = window.location.pathname;
  if (!path.startsWith('/'))
    path = '/' + path; // ie 11
  var cl = '';
  if (path == props.to || (path == context.hubRoot + '/' && props.to == props.match))
    cl = 'mdw-active';
  const dest = props.to;
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
