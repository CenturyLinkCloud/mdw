import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import NavLink from './NavLink.jsx';

class Nav extends Component {

  constructor(...args) {
    super(...args);
    this.state = { navLinks: [] };
  }

  componentDidMount() {
    fetch(new Request(this.context.hubRoot + '/js/nav.json', {
      method: 'GET',
      headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(navs => {
      const navTab = navs.find(nav => nav.id === this.props.tab);
      this.setState({
        navLinks: navTab.navs
      });
    });
  }

  render() {
    return (
      <div>
      {
        this.state.navLinks.map((navSection, index) => {
          return (
            <ul key={index} className="nav mdw-nav">
            {
              navSection.links.map((navLink, index) => {
                return (
                  <NavLink key={index} to={this.context.hubRoot + navLink.href}>
                    {navLink.label}
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
