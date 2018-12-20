import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import NavLink from '../react/NavLink.jsx';

class Nav extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    return (
      <div>
      {
        this.props.dashLinks.map((dashSection, index) => {
          return (
            <ul key={index} className="nav mdw-nav">
            {
              dashSection.links.map((dashLink, index) => {
                return (
                  <NavLink key={index} root={this.context.hubRoot} to={dashLink.href}>
                    {dashLink.label}
                  </NavLink>
                );
              })
            }
            </ul>
          );
        })
      }
      </div>
    );
  }
}

Nav.contextTypes = {
  hubRoot: PropTypes.string
};

export default Nav;
