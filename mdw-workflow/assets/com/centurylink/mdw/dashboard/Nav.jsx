import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import NavLink from '../react/NavLink.jsx';

class Nav extends Component {

  constructor(...args) {
    super(...args);
    this.state = { dashLinks: [] };
  }

  componentDidMount() {
    fetch(new Request(this.context.hubRoot + '/js/nav.json', {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(navs => {
      const dashNav = navs.find(nav => nav.id === 'dashboardTab');
      this.setState({
        dashLinks: dashNav.navs
      });
    });
  }

  render() {
    return (
      <div>
      {
        this.state.dashLinks.map((dashSection, index) => {
          return (
            <ul key={index} className="nav mdw-nav">
            {
              dashSection.links.map((dashLink, index) => {
                return (
                  <NavLink key={index} to={this.context.hubRoot + dashLink.href}>
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
