import React, {Component} from '../node/node_modules/react';
import {Link} from '../node/node_modules/react-router-dom';
import MdwContext from '../react/MdwContext';

class Nav extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    var hub = this.context.hubRoot + '/';
    const hasHistory = window.location.pathname.startsWith(hub + 'staging/');
    var cuid;
    var assetPath;
    var isHistory;
    if (hasHistory) {
      let path = window.location.pathname.substring((hub + 'staging/').length);
      const slash = path.indexOf('/');
      if (slash > 0) {
        cuid = path.substring(0, slash);
        path = path.substring(slash);
        if (path.startsWith('/assets/')) {
          assetPath = path.substring('/assets/'.length);
        }
        else if (path.startsWith('/history/')) {
          assetPath = path.substring('/history/'.length);
          isHistory = true;
        }
      }
    }
    return (
      <div>
        {this.context.authUser.roles.includes('User Admin') &&
          <ul className="nav mdw-nav">
            <li><a href={hub + '#/users'}>Users</a></li>
            <li><a href={hub + '#/groups'}>Workgroups</a></li>
            <li><a href={hub + '#/roles'}>Roles</a></li>
          </ul>
        }
        <ul className="nav mdw-nav">
          <li><a href={hub + '#/packages'}>Assets</a></li>
          <li className={isHistory ? '' : 'mdw-active'}>
            <a href={hub + 'staging'}>Staging</a>
          </li>
          {this.context.authUser.roles.includes('Process Execution') &&
            <li><a href={hub + '/#/tests'}>Testing</a></li>
          }
        </ul>
        {hasHistory &&
          <ul className="nav mdw-nav">
            <li className={isHistory ? 'mdw-active' : ''}>
              <Link
                to={this.context.hubRoot + '/staging/' + cuid + '/history/' + assetPath}>
                {'History '}
              </Link>
            </li>
          </ul>
        }
    </div>
  );
  }
}

Nav.contextType = MdwContext;
export default Nav;
