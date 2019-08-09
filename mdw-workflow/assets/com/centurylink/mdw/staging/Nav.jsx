import React, {Component} from '../node/node_modules/react';
import MdwContext from '../react/MdwContext';

class Nav extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    var hub = this.context.hubRoot + '/';
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
          <li className='mdw-active'>
            <a href={hub + 'staging'}>
              Staging
              <span className="mdw-note">beta</span>
            </a>
          </li>
          {this.context.authUser.roles.includes('Process Execution') &&
            <li><a href={hub + '/#/tests'}>Testing</a></li>
          }
        </ul>
      </div>
    );
  }
}

Nav.contextType = MdwContext;
export default Nav;
